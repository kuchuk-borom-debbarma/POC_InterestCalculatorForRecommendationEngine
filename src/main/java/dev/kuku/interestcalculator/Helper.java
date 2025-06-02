package dev.kuku.interestcalculator;

import dev.kuku.interestcalculator.models.ActivityLevel;
import dev.kuku.interestcalculator.models.InteractionType;
import dev.kuku.interestcalculator.models.entities.UserInterestEntity;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.util.Tuple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@Service
public class Helper {
    // Scaling factors based on user activity level for cross-topic influence
    public static final Map<ActivityLevel, Double> ACTIVITY_SCALING_FACTORS = Map.of(
            ActivityLevel.NO_ACTIVITY, 1.5,      // Highest cross-topic influence
            ActivityLevel.LOW_ACTIVITY, 1.4, 
            ActivityLevel.LOW_MID_ACTIVITY, 1.3, 
            ActivityLevel.MID_ACTIVITY, 1.0,     // Baseline
            ActivityLevel.MID_HIGH_ACTIVITY, 0.8, 
            ActivityLevel.HIGH_ACTIVITY, 0.6, 
            ActivityLevel.NOLIFER_ACTIVITY, 0.4  // Lowest cross-topic influence
    );
    
    // Activity level thresholds based on weighted interaction scores
    public static final Map<ActivityLevel, Integer> ACTIVITY_THRESHOLDS = Map.of(
            ActivityLevel.NO_ACTIVITY, 0, 
            ActivityLevel.LOW_ACTIVITY, 50, 
            ActivityLevel.LOW_MID_ACTIVITY, 200, 
            ActivityLevel.MID_ACTIVITY, 500, 
            ActivityLevel.MID_HIGH_ACTIVITY, 1000, 
            ActivityLevel.HIGH_ACTIVITY, 2000, 
            ActivityLevel.NOLIFER_ACTIVITY, 5000
    );

    /**
     * Calculates a decayed score based on time elapsed since last update
     */
    public long getDecayedScore(long currentTime, Long lastUpdatedTimestamp, Long score) {
        long timeElapsed = currentTime - lastUpdatedTimestamp;
        long daysElapsed = timeElapsed / (24 * 60 * 60 * 1000);
        double decayFactor; // Default: no decay
        
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
        return Math.max(0, Math.round(score * decayFactor));
    }

    /**
     * Calculates the saturation multiplier based on current score and range
     */
    public double calculateSaturationMultiplier(long currentScore, Tuple<Long, Long> scoreRange) {
        // Extract the min and max from the score range
        long minScore = scoreRange._1();
        long maxScore = scoreRange._2();

        // Calculate threshold values based on the provided range
        long threshold1 = minScore + (long) ((maxScore - minScore) * 0.1);  // 10% of range
        long threshold2 = minScore + (long) ((maxScore - minScore) * 0.3);  // 30% of range
        long threshold3 = minScore + (long) ((maxScore - minScore) * 0.6);  // 60% of range
        long threshold4 = minScore + (long) ((maxScore - minScore) * 0.9);  // 90% of range

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
     * Updates user interests and returns the updated list
     */
    public List<UserInterestEntity> updateUserInterestsAndReturn(String userId, Map<String, Tuple<Long, Long>> updatedTopics) {
        UserInterestEntity updatedEntity = new UserInterestEntity(userId, updatedTopics);
        // Create a mutable ArrayList
        List<UserInterestEntity> mutableList = new ArrayList<>();
        mutableList.add(updatedEntity);

        // Find the index of the user's existing entity (if any)
        int existingIndex = -1;
        for (int i = 0; i < mutableList.size(); i++) {
            if (mutableList.get(i).userId().equals(updatedEntity.userId())) {
                existingIndex = i;
                break;
            }
        }

        // Update or add the entity
        if (existingIndex != -1) {
            // Replace the existing entity
            mutableList.set(existingIndex, updatedEntity);
        } else {
            // Add a new entity
            mutableList.add(updatedEntity);
        }
        return mutableList;
    }

    /**
     * Gets the weight for a specific interaction type
     */
    public long getInteractionWeight(InteractionType interactionType, Tuple<Long, Long> range) {
        long min = range._1();
        long max = range._2();

        // Return the appropriate weight based on the interaction type
        return switch (interactionType) {
            case REACTION -> calculateValueInRange(min, max, 0.3); // Medium weight
            case COMMENT -> calculateValueInRange(min, max, 0.9); // High weight
            case SHARE -> calculateValueInRange(min, max, 0.7); // Medium-High weight
            case VIEW -> calculateValueInRange(min, max, 0.1); // Low weight
            default -> 0L; // Default case
        };
    }

    /**
     * Calculates a value within a given range based on percentage
     */
    public long calculateValueInRange(long min, long max, double percentage) {
        return min + Math.round((max - min) * percentage);
    }

    /**
     * Determines activity level based on activity score
     */
    public ActivityLevel determineActivityLevel(long activityScore) {
        if (activityScore >= ACTIVITY_THRESHOLDS.get(ActivityLevel.NOLIFER_ACTIVITY)) {
            return ActivityLevel.NOLIFER_ACTIVITY;
        } else if (activityScore >= ACTIVITY_THRESHOLDS.get(ActivityLevel.HIGH_ACTIVITY)) {
            return ActivityLevel.HIGH_ACTIVITY;
        } else if (activityScore >= ACTIVITY_THRESHOLDS.get(ActivityLevel.MID_HIGH_ACTIVITY)) {
            return ActivityLevel.MID_HIGH_ACTIVITY;
        } else if (activityScore >= ACTIVITY_THRESHOLDS.get(ActivityLevel.MID_ACTIVITY)) {
            return ActivityLevel.MID_ACTIVITY;
        } else if (activityScore >= ACTIVITY_THRESHOLDS.get(ActivityLevel.LOW_MID_ACTIVITY)) {
            return ActivityLevel.LOW_MID_ACTIVITY;
        } else if (activityScore >= ACTIVITY_THRESHOLDS.get(ActivityLevel.LOW_ACTIVITY)) {
            return ActivityLevel.LOW_ACTIVITY;
        } else {
            return ActivityLevel.NO_ACTIVITY;
        }
    }

    /**
     * Calculates how many unique users engage with each topic.
     * This helps identify topics with broad appeal versus those sustained by few users.
     * 
     * @param userInterestEntities List of user interest entities to analyze
     * @return Map of topic IDs to their unique user engagement counts
     */
    public Map<String, Integer> calculateTopicEngagementBreadth(List<UserInterestEntity> userInterestEntities) {
        Map<String, Set<String>> topicToUsersMap = new HashMap<>();
        
        // Collect all user interactions with topics
        for (UserInterestEntity userInterest : userInterestEntities) {
            String userId = userInterest.userId();
            
            // For each topic the user is interested in
            for (String topic : userInterest.topics().keySet()) {
                // Add this user to the set of users interested in this topic
                topicToUsersMap.computeIfAbsent(topic, k -> new HashSet<>()).add(userId);
            }
        }
        
        // Convert sets of users to counts
        Map<String, Integer> topicEngagementCounts = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : topicToUsersMap.entrySet()) {
            topicEngagementCounts.put(entry.getKey(), entry.getValue().size());
        }
        
        return topicEngagementCounts;
    }
}