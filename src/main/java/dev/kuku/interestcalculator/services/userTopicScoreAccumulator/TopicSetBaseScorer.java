package dev.kuku.interestcalculator.services.userTopicScoreAccumulator;

import dev.kuku.interestcalculator.fakeDatabase.UserInteractionsDb;
import org.springframework.stereotype.Component;

@Component
public class TopicSetBaseScorer {
    // Content discovery multipliers (intent strength: 1.0 - 5.0)
    private static final double SEARCH_MULTIPLIER = 5.0;        // Highest intent
    private static final double TRENDING_MULTIPLIER = 2.5;      // Medium intent
    private static final double RECOMMENDATION_MULTIPLIER = 1.0; // Lowest intent

    // Interaction multipliers
    // Positive interactions
    private static final double COMMENT_MULTIPLIER = 2.0;   // Strong positive engagement
    private static final double LIKE_MULTIPLIER = 1.0;     // Minor positive engagement

    // Negative interactions
    private static final double REPORT_MULTIPLIER = -2.0;   // Strong negative feedback
    private static final double DISLIKE_MULTIPLIER = -1.0; // Minor negative feedback

    // Score bounds
    private static final double MAX_POSITIVE_SCORE = 10.0;  // 5.0 * 2.0 = 10
    private static final double MAX_NEGATIVE_SCORE = -10.0; // 5.0 * -2.0 = -10

    public double calculateRawScore(UserInteractionsDb.Discovery contentDiscovery,
                                    UserInteractionsDb.InteractionType interactionType) {

        double discoveryMultiplier = getDiscoveryMultiplier(contentDiscovery);
        double interactionMultiplier = getInteractionMultiplier(interactionType);

        // Simple multiplication: discovery intent × interaction engagement
        double rawScore = discoveryMultiplier * interactionMultiplier;

        // Clamp to ensure we stay within [-10, 10] range
        return clamp(rawScore, MAX_NEGATIVE_SCORE, MAX_POSITIVE_SCORE);
    }

    /**
     * Maps content discovery method to intent strength multiplier
     * Range: 1.0 - 5.0 (higher = stronger user intent)
     */
    private double getDiscoveryMultiplier(UserInteractionsDb.Discovery discovery) {
        return switch (discovery) {
            case SEARCH -> SEARCH_MULTIPLIER;           // User actively searched
            case TRENDING -> TRENDING_MULTIPLIER;       // User browsed trending
            case RECOMMENDATION -> RECOMMENDATION_MULTIPLIER; // Algorithm suggested
        };
    }

    /**
     * Maps interaction type to engagement multiplier
     * Positive: 1.0 (like) to 2.0 (comment)
     * Negative: -1.0 (dislike) to -2.0 (report)
     */
    private double getInteractionMultiplier(UserInteractionsDb.InteractionType interactionType) {
        return switch (interactionType) {
            // Positive interactions
            case COMMENT -> COMMENT_MULTIPLIER;     // Strong positive engagement
            case LIKE -> LIKE_MULTIPLIER;           // Minor positive engagement

            // Negative interactions
            case REPORT -> REPORT_MULTIPLIER;       // Strong negative feedback
            case DISLIKE -> DISLIKE_MULTIPLIER;     // Minor negative feedback
        };
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}