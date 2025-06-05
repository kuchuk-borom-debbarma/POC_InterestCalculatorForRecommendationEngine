package dev.kuku.interestcalculator.secondIteration.services.baseScoreCalculator;

import dev.kuku.interestcalculator.secondIteration.fakeDatabase.UserInteractionsDb;
import dev.kuku.interestcalculator.secondIteration.models.TopicScoreTuple;
import dev.kuku.interestcalculator.secondIteration.services.UserActivityCalculator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * DYNAMIC CONTENT DISCOVERY BASED INTEREST SCORE CALCULATOR
 * <p>
 * This service calculates user interest/disinterest scores based on a sophisticated multi-factor algorithm
 * that considers content discovery method, interaction type, and user activity patterns across multiple
 * time horizons to provide nuanced, personalized interest scoring.
 * <p>
 * ===================================================================================
 * CORE ALGORITHM OVERVIEW
 * ===================================================================================
 * <p>
 * The algorithm works in four main phases:
 * <p>
 * 1. BASE SCORE CALCULATION (Content Discovery Context)
 *    - Assigns different base values based on HOW content was discovered
 *    - Reflects user intent and engagement likelihood
 * <p>
 * 2. INTERACTION WEIGHTING (User Action Significance)
 *    - Multiplies base score by interaction type weight
 *    - Positive weights indicate interest, negative indicate disinterest
 * <p>
 * 3. ACTIVITY-BASED NORMALIZATION (Temporal Context)
 *    - Applies inverse activity multiplier to balance scoring across user types
 *    - Prevents power users from dominating interest profiles
 * <p>
 * 4. SCORE SEGREGATION (Interest vs Disinterest)
 *    - Separates positive and negative scores into distinct metrics
 *    - Allows for independent tracking of interests and dislikes
 * <p>
 * ===================================================================================
 * CONFIGURATION PROPERTIES
 * ===================================================================================
 * <p>
 * All algorithm parameters can be configured via application.properties using the prefix:
 * 'interest-calculator.scoring'
 * <p>
 * Example configuration:
 * interest-calculator.scoring.discovery.search-score=4.0
 * interest-calculator.scoring.interaction.comment-weight=2.0
 * interest-calculator.scoring.activity.daily-weight=0.5
 * interest-calculator.scoring.multiplier.min-multiplier=0.3
 * <p>
 * See ScoreCalculatorConfig inner class for all available configuration options.
 */
@Service
@RequiredArgsConstructor
public class DynamicContentDiscoveryBasedBaseScoreCalculator {
    private final UserActivityCalculator userActivityCalculator;
    private final ScoreCalculatorConfig config;

    /**
     * Configuration class for all scoring algorithm parameters.
     * Uses Spring Boot's @ConfigurationProperties for easy configuration management.
     */
    @Data
    @Component
    @ConfigurationProperties(prefix = "interest-calculator.scoring")
    public static class ScoreCalculatorConfig {

        /**
         * Content discovery method base scores
         */
        private DiscoveryConfig discovery = new DiscoveryConfig();

        /**
         * Interaction type weights
         */
        private InteractionConfig interaction = new InteractionConfig();

        /**
         * Time horizon weights for activity calculation
         */
        private ActivityConfig activity = new ActivityConfig();

        /**
         * Activity multiplier configuration
         */
        private MultiplierConfig multiplier = new MultiplierConfig();

        /**
         * Expected maximum interactions for different time periods
         */
        private MaxScaleConfig maxScale = new MaxScaleConfig();

        /**
         * Activity composition weights
         */
        private CompositionConfig composition = new CompositionConfig();

        @Data
        public static class DiscoveryConfig {
            /**
             * Base score for content discovered through search (highest intent)
             */
            private double searchScore = 4.0;

            /**
             * Base score for trending content (medium intent)
             */
            private double trendingScore = 3.0;

            /**
             * Base score for recommended content (lower intent)
             */
            private double recommendationScore = 2.0;
        }

        @Data
        public static class InteractionConfig {
            /**
             * Weight for comment interactions (highest positive engagement)
             */
            private double commentWeight = 2.0;

            /**
             * Weight for like interactions (moderate positive engagement)
             */
            private double likeWeight = 1.0;

            /**
             * Weight for dislike interactions (moderate negative engagement)
             */
            private double dislikeWeight = -1.0;

            /**
             * Weight for report interactions (highest negative engagement)
             */
            private double reportWeight = -2.0;
        }

        @Data
        public static class ActivityConfig {
            /**
             * Weight for daily activity in composite calculation (most important)
             */
            private double dailyWeight = 0.5;

            /**
             * Weight for monthly activity in composite calculation (medium importance)
             */
            private double monthlyWeight = 0.3;

            /**
             * Weight for yearly activity in composite calculation (least important)
             */
            private double yearlyWeight = 0.2;
        }

        @Data
        public static class MultiplierConfig {
            /**
             * Maximum activity multiplier for inactive users
             */
            private double maxMultiplier = 2.0;

            /**
             * Minimum activity multiplier for very active users
             */
            private double minMultiplier = 0.3;

            /**
             * Multiplier range (maxMultiplier - minMultiplier)
             * Calculated automatically based on max and min values
             */
            public double getMultiplierRange() {
                return maxMultiplier - minMultiplier;
            }
        }

        @Data
        public static class MaxScaleConfig {
            /**
             * Expected maximum interactions per day for highly active users
             */
            private int dailyMax = 20;

            /**
             * Expected maximum interactions per month for highly active users
             */
            private int monthlyMax = 300;

            /**
             * Expected maximum interactions per year for highly active users
             */
            private int yearlyMax = 2000;

            /**
             * Days in a month for daily average calculations
             */
            private int daysInMonth = 30;
        }

        @Data
        public static class CompositionConfig {
            /**
             * Weight for total interactions in activity score calculation
             */
            private double totalInteractionsWeight = 0.7;

            /**
             * Weight for daily average in activity score calculation
             */
            private double dailyAverageWeight = 0.3;
        }
    }

    /**
     * Main execution method that orchestrates the complete interest scoring process.
     * <p>
     * CRITICAL: All activity-based calculations are performed on the INTERACTING USER
     * (userInteraction.userId), NOT on the content creator or any other user.
     * <p>
     * This method implements a four-phase algorithm:
     * 1. Gather INTERACTING USER's activity data across multiple time horizons
     * 2. Calculate base score and interaction weight
     * 3. Apply INTERACTING USER's activity-based normalization to balance score accumulation
     * 4. Segregate into interest/disinterest scores
     * <p>
     * The activity normalization ensures that:
     * - Active users (who interact frequently) get lower per-interaction scores to prevent dominance
     * - Casual users (who interact rarely) get higher per-interaction scores to survive decay
     *
     * @param userInteraction The interaction event to score (userId field determines whose activity is analyzed)
     * @return TopicScoreTuple containing separated interest and disinterest scores
     */
    public TopicScoreTuple execute(UserInteractionsDb.UserInteractionRow userInteraction) {
        // Phase 1: Gather multi-horizon activity data
        // We need different time perspectives to understand user behavior patterns
        UserActivityCalculator.ActivityMetrics dailyActivity = userActivityCalculator.getDailyActivityFromNow(userInteraction.userId);
        UserActivityCalculator.ActivityMetrics monthlyActivity = userActivityCalculator.getMonthlyActivityFromNow(userInteraction.userId);
        UserActivityCalculator.ActivityMetrics yearlyActivity = userActivityCalculator.getYearlyActivityFromNow(userInteraction.userId);

        // Phase 2: Calculate raw interaction score
        // This represents the "face value" of the interaction before normalization
        double baseScore = getBaseScore(userInteraction.contentDiscovery);
        double interactionWeight = getInteractionWeight(userInteraction.interactionType);
        double finalRawScore = baseScore * interactionWeight;

        // Phase 3: Apply activity-based normalization
        // This is where we account for user behavior patterns to ensure fairness
        double activityMultiplier = calculateCompositeInverseMultiplier(dailyActivity, monthlyActivity, yearlyActivity);

        // Phase 4: Apply multiplier and segregate scores
        // Separate positive (interest) and negative (disinterest) signals
        double interestScore = 0;
        double disinterestScore = 0;

        if (finalRawScore < 0) {
            // Negative interactions become disinterest scores
            disinterestScore = finalRawScore * activityMultiplier;
        } else {
            // Positive interactions become interest scores
            interestScore = finalRawScore * activityMultiplier;
        }

        // Return segregated scores for independent processing
        var finalTopicScore = new TopicScoreTuple();
        finalTopicScore.interestScore = interestScore;
        finalTopicScore.disinterestScore = disinterestScore;
        return finalTopicScore;
    }

    /**
     * Calculates a composite activity multiplier using weighted contributions from multiple time horizons.
     * <p>
     * This method addresses the core challenge of normalizing user interactions across different
     * activity levels and time periods. The goal is to ensure that:
     * - Highly active users don't dominate the scoring system
     * - Rare interactions from inactive users carry appropriate weight
     * - Recent activity patterns influence scoring more than historical patterns
     * <p>
     * Time Horizon Weighting Strategy (configurable):
     * - Daily: Captures current mood, immediate interests, situational factors
     * - Monthly: Represents medium-term behavioral patterns and sustained interests
     * - Yearly: Reflects long-term user personality and fundamental preferences
     *
     * @param dailyActivity User's interaction patterns in the last 24 hours
     * @param monthlyActivity User's interaction patterns in the last 30 days
     * @param yearlyActivity User's interaction patterns in the last 365 days
     * @return Composite multiplier between configured min and max values
     */
    private double calculateCompositeInverseMultiplier(
            UserActivityCalculator.ActivityMetrics dailyActivity,
            UserActivityCalculator.ActivityMetrics monthlyActivity,
            UserActivityCalculator.ActivityMetrics yearlyActivity) {

        // Calculate individual inverse multipliers for each time period
        // Each period has different interaction volume expectations
        double dailyMultiplier = calculateSinglePeriodInverseMultiplier(
                dailyActivity.totalInteractions(),
                dailyActivity.dailyAverage(),
                config.getMaxScale().getDailyMax()
        );

        double monthlyMultiplier = calculateSinglePeriodInverseMultiplier(
                monthlyActivity.totalInteractions(),
                monthlyActivity.dailyAverage(),
                config.getMaxScale().getMonthlyMax()
        );

        double yearlyMultiplier = calculateSinglePeriodInverseMultiplier(
                yearlyActivity.totalInteractions(),
                yearlyActivity.dailyAverage(),
                config.getMaxScale().getYearlyMax()
        );

        // Combine multipliers using configurable weighted average
        // This creates a nuanced multiplier that considers all time horizons
        return (dailyMultiplier * config.getActivity().getDailyWeight()) +
                (monthlyMultiplier * config.getActivity().getMonthlyWeight()) +
                (yearlyMultiplier * config.getActivity().getYearlyWeight());
    }

    /**
     * Calculates an inverse activity multiplier for a single time period.
     * <p>
     * This method implements the core inverse relationship between user activity and interaction weight:
     * - More active users get lower multipliers (their interactions are less special)
     * - Less active users get higher multipliers (their interactions are more meaningful)
     * <p>
     * The algorithm considers both total interaction volume and daily consistency:
     * - Total interactions: Raw activity level
     * - Daily average: Consistency and engagement pattern
     * <p>
     * Mathematical Approach:
     * 1. Normalize both metrics to 0-1 scale based on expected maximums
     * 2. Combine them with configurable weights
     * 3. Apply inverse transformation: high activity → low multiplier
     * 4. Constrain result to configurable bounds
     *
     * @param totalInteractions Total interactions in the period
     * @param dailyAverage Average interactions per day in the period
     * @param maxScale Expected maximum interactions for this time period
     * @return Inverse multiplier between configured min and max values
     */
    private double calculateSinglePeriodInverseMultiplier(int totalInteractions, double dailyAverage, int maxScale) {
        // Handle complete inactivity - give maximum boost to rare interactions
        if (totalInteractions == 0) return config.getMultiplier().getMaxMultiplier();

        // Normalize total interactions to 0-1 scale based on expected maximum for this time period
        // This allows us to compare activity levels across different time horizons
        double normalizedTotal = Math.min(1.0, (double) totalInteractions / maxScale);

        // Normalize daily average to 0-1 scale
        // Daily average helps us understand consistency vs. burst behavior
        double maxDailyForScale = (double) maxScale / config.getMaxScale().getDaysInMonth();
        double normalizedDaily = Math.min(1.0, dailyAverage / maxDailyForScale);

        // Combine total volume and daily consistency using configurable weights
        double activityScore = (normalizedTotal * config.getComposition().getTotalInteractionsWeight()) +
                (normalizedDaily * config.getComposition().getDailyAverageWeight());

        // Apply inverse relationship with configurable range
        // Formula: maxMultiplier - (activityScore × multiplierRange)
        // - activityScore = 0 (inactive) → multiplier = maxMultiplier
        // - activityScore = 1 (very active) → multiplier = minMultiplier
        double multiplier = config.getMultiplier().getMaxMultiplier() -
                (activityScore * config.getMultiplier().getMultiplierRange());

        // Ensure multiplier stays within configured bounds
        return Math.max(config.getMultiplier().getMinMultiplier(),
                Math.min(config.getMultiplier().getMaxMultiplier(), multiplier));
    }

    /**
     * Determines base score based on content discovery method using configured values.
     * <p>
     * The base score reflects the user's level of intent and the likelihood that
     * their interaction represents a genuine preference signal.
     *
     * @param contentDiscovery How the user discovered this content
     * @return Configured base score representing discovery context strength
     */
    private double getBaseScore(UserInteractionsDb.Discovery contentDiscovery) {
        return switch (contentDiscovery) {
            case SEARCH -> config.getDiscovery().getSearchScore();
            case TRENDING -> config.getDiscovery().getTrendingScore();
            case RECOMMENDATION -> config.getDiscovery().getRecommendationScore();
        };
    }

    /**
     * Determines interaction weight based on the type of user action using configured values.
     * <p>
     * The interaction weight reflects both the effort required for the action
     * and the sentiment expressed by the user.
     *
     * @param interactionType The type of interaction the user performed
     * @return Configured weight multiplier (positive for interest, negative for disinterest)
     */
    private double getInteractionWeight(UserInteractionsDb.InteractionType interactionType) {
        return switch (interactionType) {
            case COMMENT -> config.getInteraction().getCommentWeight();
            case LIKE -> config.getInteraction().getLikeWeight();
            case DISLIKE -> config.getInteraction().getDislikeWeight();
            case REPORT -> config.getInteraction().getReportWeight();
        };
    }
}