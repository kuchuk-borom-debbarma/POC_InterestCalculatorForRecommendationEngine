package dev.kuku.interestcalculator.services.userTopicScoreAccumulator;

import dev.kuku.interestcalculator.fakeDatabase.UserInteractionsDb;
import org.springframework.stereotype.Component;

@Component
public class TopicSetBaseScorer {
    // Content discovery multipliers
    private static final double SEARCH_MULTIPLIER = 2.0;
    private static final double TRENDING_MULTIPLIER = 1.5;
    private static final double RECOMMENDATION_MULTIPLIER = 1.2;

    // Interaction type multipliers
    private static final double COMMENT_MULTIPLIER = 1.5;
    private static final double LIKE_MULTIPLIER = 1.0;
    private static final double REPORT_MULTIPLIER = -2.0;

    // Validation constants
    private static final double MAX_RAW_PRODUCT = 10.0;
    private static final double MIN_RAW_PRODUCT = -10.0;

    public double calculateRawScore(UserInteractionsDb.Discovery contentDiscovery,
                                    UserInteractionsDb.InteractionType interactionType) {
        double discoveryMultiplier = getMultiplier(contentDiscovery);
        double interactionMultiplier = getMultiplier(interactionType);

        double rawProduct = clamp(discoveryMultiplier * interactionMultiplier,
                MIN_RAW_PRODUCT, MAX_RAW_PRODUCT);

        // More numerically stable than log(1 + x) with clamping
        double magnitude = Math.log1p(Math.min(Math.abs(rawProduct), MAX_RAW_PRODUCT - 1));

        return Math.copySign(magnitude, rawProduct);
    }

    private double getMultiplier(UserInteractionsDb.Discovery discovery) {
        return switch (discovery) {
            case SEARCH -> SEARCH_MULTIPLIER;
            case TRENDING -> TRENDING_MULTIPLIER;
            case RECOMMENDATION -> RECOMMENDATION_MULTIPLIER;
            default -> 1.0;
        };
    }

    private double getMultiplier(UserInteractionsDb.InteractionType interactionType) {
        return switch (interactionType) {
            case COMMENT -> COMMENT_MULTIPLIER;
            case LIKE -> LIKE_MULTIPLIER;
            case REPORT -> REPORT_MULTIPLIER;
        };
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}