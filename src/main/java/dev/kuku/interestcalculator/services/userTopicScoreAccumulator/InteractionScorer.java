package dev.kuku.interestcalculator.services.userTopicScoreAccumulator;

import dev.kuku.interestcalculator.fakeDatabase.UserInteractionsDb;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.HashMap;

@Component
public class InteractionScorer {
    //The final value is calculated by doing MAX_DISCOVERY_VALUE * multiplier
    private static final double MAX_DISCOVERY_VALUE = 5.0;
    private static final double MAX_INTERACTION_WEIGHT = 1.0;
    // The maximum possible delta for calculating. The rawScore will be scaled to this range.
    private static final double TARGET_MAX_POSSIBLE_DELTA = 1.0;
    // Pre-calculated score cache for efficiency
    private static final Map<String, Double> SCORE_CACHE = calculateScoreCache();
    // Calculate the theoretical min/max raw scores from cache
    private static final double MIN_RAW_SCORE = SCORE_CACHE.get("MIN_SCORE");
    private static final double MAX_RAW_SCORE = SCORE_CACHE.get("MAX_SCORE");

    private static double getDiscoveryMultiplier(UserInteractionsDb.Discovery discovery) {
        return switch (discovery) {
            case SEARCH -> 1.0;
            case TRENDING -> 0.5;
            case RECOMMENDATION -> 0.2;
        };
    }

    private static double getInteractionMultiplier(UserInteractionsDb.InteractionType interactionType) {
        return switch (interactionType) {
            case COMMENT -> 1.0;
            case LIKE -> 0.5;
            case DISLIKE -> -0.5;
            case REPORT -> -1.0;
        };
    }

    /**
     * Pre-calculate all possible scores and store min/max for efficiency
     */
    private static Map<String, Double> calculateScoreCache() {
        Map<String, Double> cache = new HashMap<>();
        double minRawScore = Double.MIN_VALUE;
        double maxRawScore = Double.MAX_VALUE;

        // Calculate all possible scores once and cache min/max
        for (UserInteractionsDb.Discovery discovery : UserInteractionsDb.Discovery.values()) {
            for (UserInteractionsDb.InteractionType interactionType : UserInteractionsDb.InteractionType.values()) {
                double discoveryScore = getDiscoveryMultiplier(discovery) * MAX_DISCOVERY_VALUE;
                double interactionWeight = getInteractionMultiplier(interactionType) * MAX_INTERACTION_WEIGHT;
                double rawScore = discoveryScore * interactionWeight;

                // Store individual scores for potential future use
                String key = discovery.name() + "_" + interactionType.name();
                cache.put(key, rawScore);

                // Track min/max
                minRawScore = Math.min(minRawScore, rawScore);
                maxRawScore = Math.max(maxRawScore, rawScore);
            }
        }

        // Store min/max in cache
        cache.put("MIN_SCORE", minRawScore);
        cache.put("MAX_SCORE", maxRawScore);

        return cache;
    }

    public double calculateInteractionScoreDelta(UserInteractionsDb.Discovery contentDiscovery,
                                                 UserInteractionsDb.InteractionType interactionType) {

        String cacheKey = contentDiscovery.name() + "_" + interactionType.name();
        double rawScore = SCORE_CACHE.get(cacheKey);
        return normalizeToRange(rawScore);
    }

    /**
     * Normalize a value from one range to another
     *
     * @param value The value to normalize
     * @return The normalized value
     */
    private double normalizeToRange(double value) {
        if (InteractionScorer.MAX_RAW_SCORE == InteractionScorer.MIN_RAW_SCORE) {
            return InteractionScorer.TARGET_MAX_POSSIBLE_DELTA;
        }
        return (value - InteractionScorer.MIN_RAW_SCORE) / (InteractionScorer.MAX_RAW_SCORE - InteractionScorer.MIN_RAW_SCORE) * (-1.0 - InteractionScorer.TARGET_MAX_POSSIBLE_DELTA) + InteractionScorer.TARGET_MAX_POSSIBLE_DELTA;
    }

}