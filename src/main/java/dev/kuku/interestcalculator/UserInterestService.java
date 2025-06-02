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
     * Works by getting all the topics, and it's score for the user and then decaying the score based on the time the topic was updated last and current time.
     * Decaying ensures that topics which are no longer relevant to the user are gradually removed from their interests.
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

    public void decayTopicRelationships(double decayFactor, long minimumWeight) {
        List<TopicRelationshipEntity> updatedRelationships = new ArrayList<>();

        for (TopicRelationshipEntity relationship : db.topicRelationships) {
            long newWeight = (long) Math.max(0, (relationship.weight() * decayFactor));

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