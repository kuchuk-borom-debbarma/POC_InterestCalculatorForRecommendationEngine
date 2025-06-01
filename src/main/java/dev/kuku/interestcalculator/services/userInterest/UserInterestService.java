package dev.kuku.interestcalculator.services.userInterest;

import dev.kuku.interestcalculator.models.ActivityLevel;
import dev.kuku.interestcalculator.models.InteractionData;
import dev.kuku.interestcalculator.models.UserInteractionData;
import dev.kuku.interestcalculator.models.entities.PostEntity;
import dev.kuku.interestcalculator.models.entities.UserInterestEntity;
import dev.kuku.interestcalculator.repo.UserInteractionRepo;
import dev.kuku.interestcalculator.repo.UserInterestRepo;
import dev.kuku.interestcalculator.services.ai.LLMService;
import dev.kuku.interestcalculator.services.platform.PlatformService;
import dev.kuku.interestcalculator.services.topic.TopicService;
import dev.kuku.interestcalculator.services.userActivity.UserActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.util.Tuple;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserInterestService {
    private final UserInteractionRepo userInterestRepository;
    private final UserInterestRepo userInterestRepo;
    private final PlatformService platformService;
    private final LLMService llmService;
    private final UserActivityService userActivityService;
    private final List<PostEntity> posts = new ArrayList<>();
    private final Set<String> topics = new HashSet<>();
    private final TopicService topicService;

    private static int getDecayedScore(long currentTime, Long lastUpdatedTimestamp, Integer score) {
        long timeElapsed = currentTime - lastUpdatedTimestamp;
        long daysElapsed = timeElapsed / (24 * 60 * 60 * 1000);
        // Apply decay factor based on elapsed time as per README
        double decayFactor = -1.0; // Default: no decay
        if (daysElapsed <= 7) {
            // Recent interactions (0-7 days): No decay (1.0x)
            decayFactor = 1.0;
        } else if (daysElapsed <= 28) {
            // Moderate age (1-4 weeks): Slight decay (0.8-0.9x)
            // Linear interpolation between 0.9 and 0.8
            decayFactor = 0.9 - ((daysElapsed - 7) / 21.0) * 0.1;
        } else if (daysElapsed <= 180) {
            // Older interactions (1-6 months): Significant decay (0.3-0.7x)
            // Linear interpolation between 0.7 and 0.3
            decayFactor = 0.7 - ((daysElapsed - 28) / 152.0) * 0.4;
        } else {
            // Historical data (6+ months): Minimal impact (0.1-0.2x)
            // Linear interpolation between 0.2 and 0.1
            decayFactor = 0.2 - Math.min(((daysElapsed - 180) / 180.0) * 0.1, 0.1);
        }
        // Apply decay to score
        int decayedScore = (int) Math.max(0, Math.round(score * decayFactor));
        return decayedScore;
    }

    public void calculateUserInterestScore(Set<UserInteractionData> userInteractions) {
        for (var ui : userInteractions) {
            List<InteractionData> interactions = ui.interactionData();
            String userId = ui.userId();
            // We decay existing interest scores
            decayInterestScore(userId);
            // Get activity scaling of user
            long currentTime = Instant.now().toEpochMilli();
            long thirtyDaysAgo = Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS).toEpochMilli();
            ActivityLevel userActivity = userActivityService.getActivityLevelOfUser(userId, reactor.util.function.Tuples.of(thirtyDaysAgo, currentTime));
            //Get contents so that we can extract topics
            Map<String, String> contentIdAndContent = new HashMap<>();
            interactions.forEach(interaction -> {
                PostEntity post = posts.stream().filter(p -> p.id().equals(interaction.contentId())).findFirst().orElse(null);
                if (post == null) return;
                contentIdAndContent.put(interaction.contentId(), post.data());
            });
            Map<String, Set<String>> contentToTopics = llmService.getTopics(topics, contentIdAndContent);
            //Now we can go ahead and count the appearance of each topic
            Map<String, Integer> topicAndCount = new HashMap<>();
            contentToTopics.forEach((contentId, topics) -> topics.forEach(topic -> {
                if (topicAndCount.containsKey(topic)) {
                    topicAndCount.put(topic, topicAndCount.get(topic) + 1);
                } else {
                    topicAndCount.put(topic, 1);
                }
            }));

            //TODO interaction weight, gathering cross-topic influence within a threshold and adding scores to existing topics with saturation,

            //update cross topic relationships
            topicService.updateTopicRelationships(); //TODO
        }
    }

    public void decayInterestScore(String userId) {
        Map<String, Tuple<Integer, Long>> userInterestsMap = userInterestRepo.getUserInterests(userId);
        var currentTime = Instant.now().toEpochMilli();

        // If no interests for user, nothing to decay
        if (userInterestsMap.isEmpty()) {
            return;
        }

        // Create a map to hold the updated scores
        Map<String, Tuple<Integer, Long>> updatedTopics = new HashMap<>();

        // Process each topic and apply appropriate decay factor
        for (Map.Entry<String, Tuple<Integer, Long>> entry : userInterestsMap.entrySet()) {
            String topic = entry.getKey();
            Integer score = entry.getValue()._1();
            Long lastUpdatedTimestamp = entry.getValue()._2();
            int decayedScore = getDecayedScore(currentTime, lastUpdatedTimestamp, score);
            // Add to updated topics with the original timestamp (we're only decaying score, not updating timestamp)
            updatedTopics.put(topic, new Tuple<>(decayedScore, lastUpdatedTimestamp));
        }

        // Update the user's interests with decayed scores
        UserInterestEntity updatedEntity = new UserInterestEntity(userId, updatedTopics);

        userInterestRepo.saveUserInterests(updatedEntity);
    }

    /**
     * Calculates the saturation multiplier based on the current interest score.
     * As a user's interest score in a topic increases, new interactions with that topic
     * will have diminishing returns to prevent algorithmic "rabbit holes" and encourage
     * content diversity.
     *
     * @param currentScore The current interest score for a specific topic
     * @param scoreRange   A tuple containing (minScore, maxScore) to define the saturation thresholds
     * @return A multiplier between 0.1 and 1.0 to be applied to new score additions
     */
    public double calculateSaturationMultiplier(int currentScore, Tuple<Integer, Integer> scoreRange) {
        // Extract the min and max from the score range
        int minScore = scoreRange._1();
        int maxScore = scoreRange._2();

        // Calculate threshold values based on the provided range
        int threshold1 = minScore + (int) ((maxScore - minScore) * 0.1);  // 10% of range
        int threshold2 = minScore + (int) ((maxScore - minScore) * 0.3);  // 30% of range
        int threshold3 = minScore + (int) ((maxScore - minScore) * 0.6);  // 60% of range
        int threshold4 = minScore + (int) ((maxScore - minScore) * 0.9);  // 90% of range

        // Define multipliers based on where the current score falls within the range
        if (currentScore <= threshold1) {
            // Fresh Interest: Full impact (1.0x)
            return 1.0;
        } else if (currentScore <= threshold2) {
            // Developing Interest: Reduced impact (0.8x)
            return 0.8;
        } else if (currentScore <= threshold3) {
            // Established Interest: Diminished impact (0.6x)
            return 0.6;
        } else if (currentScore <= threshold4) {
            // Saturated Interest: Minimal impact (0.3x)
            return 0.3;
        } else {
            // Extremely Saturated Interest: Very minimal impact (0.1x)
            return 0.1;
        }
    }

    /**
     * Alternative implementation using a continuous function for smoother transitions
     * between saturation levels.
     *
     * @param currentScore The current interest score for a specific topic
     * @param scoreRange   A tuple containing (minScore, maxScore) to define the saturation curve
     * @return A multiplier between 0.1 and 1.0 to be applied to new score additions
     */
    public double calculateSaturationMultiplierContinuous(int currentScore, Tuple<Integer, Integer> scoreRange) {
        // Extract the min and max from the score range
        int minScore = scoreRange._1();
        int maxScore = scoreRange._2();

        // Calculate the midpoint of the range, which will be used as the scaling factor
        double scalingFactor = (maxScore - minScore) / 2.0;

        // Normalize the current score relative to the range
        double normalizedScore = (currentScore - minScore) / (double) (maxScore - minScore);

        // Calculate the saturation multiplier using a sigmoid-like curve
        // This will give 1.0 for low scores and gradually approach 0.1 for high scores
        double rawMultiplier = 1.0 / (1.0 + (normalizedScore * 3.0));

        // Ensure multiplier never goes below 0.1
        return Math.max(0.1, rawMultiplier);
    }


}