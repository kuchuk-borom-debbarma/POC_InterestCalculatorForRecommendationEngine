package dev.kuku.interestcalculator.UserTopicScoringSystem.TopicScorer.subSystem;

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
        double minRawScore = Double.POSITIVE_INFINITY;  // Fixed: Start with positive infinity
        double maxRawScore = Double.NEGATIVE_INFINITY;  // Fixed: Start with negative infinity

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

    /**
     * Should return delta
     */
    public double calculateInteractionScoreDelta(UserInteractionsDb.Discovery contentDiscovery,
                                                 UserInteractionsDb.InteractionType interactionType) {

        String cacheKey = contentDiscovery.name() + "_" + interactionType.name();
        double rawScore = SCORE_CACHE.get(cacheKey);
        return normalizeToRange(rawScore);
    }

    /**
     * Normalize a value from the raw score range to [-TARGET_MAX_POSSIBLE_DELTA, +TARGET_MAX_POSSIBLE_DELTA]
     * The highest raw score maps to +TARGET_MAX_POSSIBLE_DELTA
     * The lowest raw score maps to -TARGET_MAX_POSSIBLE_DELTA
     *
     * @param value The raw score value to normalize
     * @return The normalized value in range [-TARGET_MAX_POSSIBLE_DELTA, +TARGET_MAX_POSSIBLE_DELTA]
     */
    private double normalizeToRange(double value) {
        // Handle edge case where all scores are the same
        if (MAX_RAW_SCORE == MIN_RAW_SCORE) {
            return 0.0; // Return neutral value
        }

        // Maps [MIN_RAW_SCORE, MAX_RAW_SCORE] to [-TARGET_MAX_POSSIBLE_DELTA, +TARGET_MAX_POSSIBLE_DELTA]
        // Formula: 2 * ((value - min) / (max - min)) - 1, then scale by TARGET_MAX_POSSIBLE_DELTA
        double normalizedToMinusOneToOne = 2.0 * (value - MIN_RAW_SCORE) / (MAX_RAW_SCORE - MIN_RAW_SCORE) - 1.0;
        return normalizedToMinusOneToOne * TARGET_MAX_POSSIBLE_DELTA;
    }
}