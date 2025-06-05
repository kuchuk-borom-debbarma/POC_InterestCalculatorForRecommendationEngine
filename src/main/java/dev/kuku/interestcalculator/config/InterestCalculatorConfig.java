package dev.kuku.interestcalculator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Main configuration class for the Interest Calculator system.
 * Uses Spring Boot's @ConfigurationProperties for easy configuration management.
 *
 * Configuration prefix: 'interest-calculator'
 *
 * Example configuration:
 * interest-calculator.topic-score-min=0
 * interest-calculator.topic-score-max=10
 * interest-calculator.scoring.discovery.search-score=4.0
 * interest-calculator.scoring.interaction.comment-weight=2.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "interest-calculator")
public class InterestCalculatorConfig {

    /**
     * Minimum allowed topic score value
     */
    private double topicScoreMin = 0.0;

    /**
     * Maximum allowed topic score value
     */
    private double topicScoreMax = 10.0;

    /**
     * Configuration for topic score calculation algorithm
     */
    private TopicScoreCalculationConfig scoring = new TopicScoreCalculationConfig();

    /**
     * Configuration class for all scoring algorithm parameters.
     */
    @Data
    public static class TopicScoreCalculationConfig {

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

        /**
         * Score saturation control configuration
         */
        private SaturationConfig saturation = new SaturationConfig();

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

        @Data
        public static class SaturationConfig {
            /**
             * Saturation factor controlling how aggressively scores approach boundaries.
             * Higher values create more aggressive saturation (faster approach to limits).
             * Lower values create gentler saturation (slower approach to limits).
             *
             * Typical values:
             * - 0.5: Very gentle saturation, scores can grow quite high before significant resistance
             * - 1.0: Moderate saturation, balanced approach
             * - 2.0: Aggressive saturation, strong resistance as scores increase
             */
            private double saturationFactor = 1.0;

            /**
             * Whether saturation control is enabled.
             * When disabled, scores can grow without bounds (not recommended for production).
             */
            private boolean enabled = true;
        }
    }
}