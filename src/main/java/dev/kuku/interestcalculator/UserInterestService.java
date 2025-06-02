package dev.kuku.interestcalculator;

import dev.kuku.interestcalculator.models.ActivityLevel;
import dev.kuku.interestcalculator.models.InteractionData;
import dev.kuku.interestcalculator.models.InteractionType;
import dev.kuku.interestcalculator.models.UserInteractionData;
import dev.kuku.interestcalculator.models.entities.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.util.Tuple;
import reactor.util.function.Tuple2;

import java.time.Instant;
import java.util.*;

import static dev.kuku.interestcalculator.Config.SCORE_RANGE;

@Service
@RequiredArgsConstructor
public class UserInterestService {
    private final Db db;
    private final LLMService llmService;
    private final Helper helper;

    //TODO make this a CRON job that takes in from a user interaction queue

    /**
     * Dynamic User Interest Calculation Algorithm
     *
     * This algorithm processes user interactions to build a personalized interest profile that evolves over time.
     * It balances content personalization with discovery through a multi-step approach:
     *
     * 1. Temporal Decay: Applies time-based decay to existing interests to naturally fade outdated preferences.
     *    Example: A user who showed interest in "Skiing" 3 months ago but hasn't engaged since will see 
     *    their skiing interest score decay from 850 to 340 points (60% decay), reducing skiing content in their feed.
     *
     * 2. Direct Interest Calculation: Analyzes recent user interactions and assigns weighted scores based on engagement depth.
     *    Example: A user who comments on a "Photography" post (weight: 90) and views a "Travel" post (weight: 10)
     *    receives 90 points toward Photography and 10 points toward Travel interests.
     *
     * 3. Cross-Topic Discovery: Identifies and scores related topics based on established topic relationships.
     *    Example: When a user engages with "Cooking" content, the system may add 15 points to "Baking" and 
     *    8 points to "Nutrition" based on topic relationship strengths (0.6 and 0.3 respectively).
     *
     * 4. Activity-Based Scaling: Adjusts cross-topic influence based on user activity levels to promote discovery.
     *    Example: A casual user (low activity) engaging with "Jazz" content will receive a 1.4x multiplier on related 
     *    topics like "Blues" (21 points instead of 15), while a power user receives only a 0.6x multiplier (9 points).
     *    This helps casual users discover more diverse content while respecting power users' established preferences.
     *
     * 5. Saturation Control: Implements diminishing returns for frequently engaged topics to prevent content tunnel vision.
     *    Example: For a new interest in "Podcasts" (score: 30), a full 100% of new engagement points are applied.
     *    For an established interest in "Football" (score: 700), only 30% of new engagement points are applied,
     *    making it easier for newer interests to gain visibility in the user's content feed.
     * 
     * The combined effect of these mechanisms creates a dynamic, self-balancing interest profile that:
     * - Prioritizes recent engagement while preserving long-term interests
     * - Facilitates discovery of related content without overwhelming established preferences
     * - Adapts to changing user interests at an appropriate pace
     * - Prevents algorithm-induced content bubbles through saturation controls
     *
     * @param userInteractions Set of user interaction data to process
     */
    public void calculateUserInterestScore(Set<UserInteractionData> userInteractions) {
        for (UserInteractionData ui : userInteractions) {
            Map<String, Long> uniqueTopicScores = new HashMap<>();
            List<InteractionData> interactions = ui.interactionData();
            String userId = ui.userId();

            // 1. Decay existing topics scores
            decayInterestScore(userId);

            // 2. Iterate each interaction, extract topics and calculate base score with interaction weight
            for (var i : interactions) {
                long weight = helper.getInteractionWeight(i.interactionType(), SCORE_RANGE);

                ContentTopic contentTopic = db.findContentTopicByContentId(i.contentId());
                List<String> topics;

                if (contentTopic == null) {
                    PostEntity contentData = db.findPostById(i.contentId());
                    if (contentData == null) {
                        throw new RuntimeException("Content data not found for id: " + i.contentId());
                    }
                    topics = llmService.getTopics(new ArrayList<>(db.topics), contentData.data());
                    db.addTopics(topics);
                    db.addContentTopic(new ContentTopic(i.contentId(), topics));
                } else {
                    topics = contentTopic.topics();
                }

                // Add score to unique topic scores
                topics.forEach(topic -> {
                    if (uniqueTopicScores.containsKey(topic)) {
                        uniqueTopicScores.put(topic, uniqueTopicScores.get(topic) * weight);
                    } else {
                        uniqueTopicScores.put(topic, weight);
                    }
                });
            }

            // 3. Add cross-topic influence scores for related topic
            Set<String> usedTopics = new HashSet<>();
            Map<String, Long> crossTopicScores = new HashMap<>();

            // Calculate user activity level to scale cross-topic influence topic score.
            long currentTime = Instant.now().toEpochMilli();
            long thirtyDaysAgo = Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS).toEpochMilli();
            ActivityLevel userActivity = getActivityLevelOfUser(userId, reactor.util.function.Tuples.of(thirtyDaysAgo, currentTime));
            double activityScalingFactor = Helper.ACTIVITY_SCALING_FACTORS.getOrDefault(userActivity, 1.0);

            for (var topic : uniqueTopicScores.keySet()) {
                db.topicRelationships.forEach(tr -> {
                    if (tr.topic1().equals(topic)) {
                        if (!usedTopics.contains(tr.topic2())) {
                            usedTopics.add(tr.topic2());
                            long baseScore = 25L;
                            long score = (long) (baseScore * tr.weight() * activityScalingFactor);
                            crossTopicScores.merge(tr.topic2(), score, Long::sum);
                        }
                    } else if (tr.topic2().equals(topic)) {
                        if (!usedTopics.contains(tr.topic1())) {
                            usedTopics.add(tr.topic1());
                            long baseScore = 25L;
                            long score = (long) (baseScore * tr.weight() * activityScalingFactor);
                            crossTopicScores.merge(tr.topic1(), score, Long::sum);
                        }
                    }
                });
            }

            // Merge cross-topic scores into unique topic scores
            for (Map.Entry<String, Long> entry : crossTopicScores.entrySet()) {
                uniqueTopicScores.merge(entry.getKey(), entry.getValue(), Long::sum);
            }

            // 4. Apply saturation and update scores
            Map<String, Tuple<Long, Long>> existingUserInterests = db.getUserInterests(userId);
            Map<String, Tuple<Long, Long>> updatedUserInterests = new HashMap<>(existingUserInterests);

            for (var topic : uniqueTopicScores.keySet()) {
                long score = uniqueTopicScores.get(topic);
                long currentScore = 0;

                if (existingUserInterests.containsKey(topic)) {
                    currentScore = existingUserInterests.get(topic)._1();
                }

                double saturationMultiplier = helper.calculateSaturationMultiplier(currentScore, SCORE_RANGE);
                long scoreAddition = Math.round(score * saturationMultiplier);
                long newScore = currentScore + scoreAddition;

                // Ensure score stays within range
                newScore = Math.max(SCORE_RANGE._1(), Math.min(newScore, SCORE_RANGE._2()));

                // Update score
                updatedUserInterests.put(topic, new Tuple<>(newScore, currentTime));
            }

            // Update the database
            db.updateUserInterests(helper.updateUserInterestsAndReturn(userId, updatedUserInterests));

            // 5. Update topic relationships
            Set<Set<String>> topicSets = new HashSet<>();
            for (var interaction : interactions) {
                ContentTopic contentTopic = db.findContentTopicByContentId(interaction.contentId());
                if (contentTopic != null && contentTopic.topics().size() > 1) {
                    topicSets.add(new HashSet<>(contentTopic.topics()));
                }
            }
            //TODO send topicSet to updateTopicRelationships queue
        }
    }

    //TODO make this a CRON job that takes in from a queue
    private void updateTopicRelationships(Set<Set<String>> topicSets) {
        // First decay existing relationships
        decayTopicRelationships(0.9, 5);

        // Process each set of co-occurring topics
        for (Set<String> topicSet : topicSets) {
            if (topicSet.size() < 2) {
                continue;
            }

            List<String> topicList = new ArrayList<>(topicSet);
            for (int i = 0; i < topicList.size(); i++) {
                for (int j = i + 1; j < topicList.size(); j++) {
                    String topic1 = topicList.get(i);
                    String topic2 = topicList.get(j);

                    // Ensure consistent ordering
                    if (topic1.compareTo(topic2) > 0) {
                        String temp = topic1;
                        topic1 = topic2;
                        topic2 = temp;
                    }

                    final String finalTopic1 = topic1;
                    final String finalTopic2 = topic2;

                    var existingRelationship = db.findTopicRelationship(finalTopic1, finalTopic2);

                    if (existingRelationship.isPresent()) {
                        // Update existing relationship
                        var relationship = existingRelationship.get();
                        db.removeTopicRelationship(relationship);
                        db.addTopicRelationship(new TopicRelationshipEntity(
                                relationship.topic1(),
                                relationship.topic2(),
                                relationship.weight() + 1L
                        ));
                    } else {
                        // Create new relationship
                        db.addTopicRelationship(new TopicRelationshipEntity(topic1, topic2, 1L));
                    }
                }
            }
        }
    }

    /**
     * Temporal Decay for User Interests
     * 
     * This method applies time-based decay to a user's interest scores, simulating the natural fading of interests over time.
     * The decay follows a progressive curve from recent interests (no decay) to historical interests (significant decay).
     * 
     * Key behaviors:
     * 1. Topics with zero scores after decay are automatically removed from the user's interest profile
     * 2. The timestamp of last activity is preserved, allowing for accurate decay calculation in future updates
     * 
     * Design considerations:
     * - Interest Retention for Inactive Users: The current implementation may result in complete topic removal for
     *   long-inactive users. This provides a "clean slate" when they return. Alternative approaches include:
     *   
     *   Example: A user who hasn't engaged for 8 months returns to find their previous interests have decayed to zero.
     *   The system now treats them similar to a new user, allowing rapid adaptation to their current interests.
     *   
     * - Topic Capacity Limits: An alternative approach would implement a maximum topic capacity per user (e.g., 30 topics),
     *   removing only the lowest-scoring topics when capacity is reached rather than zero-score topics.
     *   
     *   Example: If a user has 30 topics and returns after inactivity, instead of removing all decayed topics,
     *   the system preserves their historical top interests to provide continuity in content recommendations.
     * 
     * - Score Normalization: The current implementation maintains absolute scores. Consider normalizing scores
     *   within a fixed range (e.g., 0-1000) based on the user's highest and lowest scores to maintain balanced
     *   topic distribution regardless of activity level.
     *   
     *   Example: After decay, a user's scores range from 10 to 875. Normalizing would rescale these to utilize the
     *   full 0-1000 range, giving clearer separation between interest levels.
     *
     * The current implementation prioritizes adaptability and relevance by removing zero-score topics,
     * ensuring the user's interest profile remains current and reflective of genuine ongoing interests.
     *
     * @param userId The identifier of the user whose interest scores should be decayed
     */
    public void decayInterestScore(String userId) {
        Map<String, Tuple<Long, Long>> userInterestsMap = db.getUserInterests(userId);
        long currentTime = Instant.now().toEpochMilli();

        if (userInterestsMap.isEmpty()) {
            return;
        }

        Map<String, Tuple<Long, Long>> updatedTopics = new HashMap<>();

        for (Map.Entry<String, Tuple<Long, Long>> entry : userInterestsMap.entrySet()) {
            String topic = entry.getKey();
            Long score = entry.getValue()._1();
            Long lastUpdatedTimestamp = entry.getValue()._2();
            long decayedScore = helper.getDecayedScore(currentTime, lastUpdatedTimestamp, score);

            // Only keep topics with non-zero scores
            if (decayedScore > 0) {
                updatedTopics.put(topic, new Tuple<>(decayedScore, lastUpdatedTimestamp));
            }
            // Topics with zero score will not be added to updatedTopics, effectively removing them
        }

        db.updateUserInterests(helper.updateUserInterestsAndReturn(userId, updatedTopics));
    }

    /**
     * Adaptive Decay for Topic Relationships
     * 
     * This method applies intelligent decay to topic relationship weights based on multiple factors:
     * 1. Time-based decay using the decay factor parameter
     * 2. Popularity-based adjustment to identify and reduce artificially sustained relationships
     * 
     * The implementation addresses a key challenge in relationship scoring: topics that remain active due to
     * a small number of highly engaged users rather than genuine broad interest. This ensures that topic
     * relationships reflect actual content affinity patterns across the user base rather than outlier behavior.
     * 
     * Approach:
     * - Calculate a popularity score for each topic based on user engagement breadth
     * - Apply stronger decay to relationships where topics have low engagement breadth
     * - Remove relationships that fall below the minimum weight threshold
     * 
     * Example: A relationship between "Cryptocurrency" and "Finance" that was once trending (weight: 85)
     * but is now only sustained by 2 active users will receive an additional decay multiplier of 0.7x,
     * accelerating its decline compared to topics engaged with by hundreds of users.
     * 
     * @param baseDecayFactor The standard time-based decay factor (e.g., 0.9 for 10% decay per cycle)
     * @param minimumWeight The threshold below which relationships are removed
     */
    public void decayTopicRelationships(double baseDecayFactor, long minimumWeight) {
        // 1. Calculate popularity metrics for each topic
        Map<String, Integer> topicEngagementCounts = helper.calculateTopicEngagementBreadth(db.userInterestEntities);
        
        // Find max engagement to normalize popularity
        int maxEngagement = topicEngagementCounts.values().stream()
                .max(Integer::compare)
                .orElse(1);
        
        List<TopicRelationshipEntity> updatedRelationships = new ArrayList<>();

        for (TopicRelationshipEntity relationship : db.topicRelationships) {
            // 2. Calculate popularity-adjusted decay factor
            String topic1 = relationship.topic1();
            String topic2 = relationship.topic2();
            
            int topic1Engagement = topicEngagementCounts.getOrDefault(topic1, 0);
            int topic2Engagement = topicEngagementCounts.getOrDefault(topic2, 0);
            
            // Calculate popularity ratios (0.3 to 1.0)
            double topic1PopularityRatio = 0.3 + (0.7 * Math.min(1.0, (double)topic1Engagement / maxEngagement));
            double topic2PopularityRatio = 0.3 + (0.7 * Math.min(1.0, (double)topic2Engagement / maxEngagement));
            
            // Use the lower popularity as the modifier (more aggressive decay for less popular topics)
            double popularityModifier = Math.min(topic1PopularityRatio, topic2PopularityRatio);
            
            // 3. Apply decay with popularity adjustment
            double adjustedDecayFactor = baseDecayFactor * popularityModifier;
            long newWeight = (long) Math.max(0, (relationship.weight() * adjustedDecayFactor));

            // 4. Keep only relationships above minimum weight
            if (newWeight >= minimumWeight) {
                updatedRelationships.add(new TopicRelationshipEntity(
                        relationship.topic1(),
                        relationship.topic2(),
                        newWeight
                ));
            }
        }

        db.updateTopicRelationships(updatedRelationships);
    }

    public Map<InteractionType, Long> getInteractionWeight(Tuple<Long, Long> range) {
        Map<InteractionType, Long> weights = new HashMap<>();

        weights.put(InteractionType.REACTION, helper.calculateValueInRange(range._1(), range._2(), 0.3));
        weights.put(InteractionType.COMMENT, helper.calculateValueInRange(range._1(), range._2(), 0.9));
        weights.put(InteractionType.SHARE, helper.calculateValueInRange(range._1(), range._2(), 0.7));
        weights.put(InteractionType.VIEW, helper.calculateValueInRange(range._1(), range._2(), 0.1));

        return weights;
    }

    public ActivityLevel getActivityLevelOfUser(String userId, Tuple2<Long, Long> timespan) {
        // Get user interactions for the specified timespan
        List<UserInteractionEntity> userInteractions = db.getUserInteractionsInTimeRange(
                userId, timespan.getT1(), timespan.getT2());

        if (userInteractions.isEmpty()) {
            return ActivityLevel.NO_ACTIVITY;
        }

        Map<InteractionType, Long> interactionWeights = getInteractionWeight(
                new Tuple<>(1L, 10L)
        );

        long activityScore = calculateActivityScore(userInteractions, interactionWeights);

        return helper.determineActivityLevel(activityScore);
    }

    private long calculateActivityScore(List<UserInteractionEntity> interactions, Map<InteractionType, Long> weights) {
        return interactions.stream()
                .mapToLong(interaction -> weights.getOrDefault(interaction.interactionType(), 0L))
                .sum();
    }
}