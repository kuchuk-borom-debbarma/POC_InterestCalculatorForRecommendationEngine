package dev.kuku.interestcalculator.config.userTopic;

import lombok.Data;

@Data
public class UserTopicScoreAccumulatorConfig {
    /// Recommended to keep it enabled
    boolean contentDiscoveryMultiplierEnabled = true;
    /// Recommended to keep it enabled. This helps topic to be either an "interest" or "disinterest".
    boolean interactionTypeMultiplierEnabled = true;
    /// Primarily used for scaling score based on user activity.
    /// Casual users will have a higher score to compensate for decays due to lower activity while power users have lower score as their score naturally goes up due to heavy interaction.
    boolean userActivityScalingMultiplierEnabled = false;
    /// Used to scale a topic score based on past interactions.
    /// More recent topic will have higher score. This promotes recent interest to build up.
    boolean topicRecencyMultiplier = true;
    /// The closer the score is to maxRange, the slower the score increases.
    boolean applySaturationEnabled = true;
    private MultiplierConfig multiplier = new MultiplierConfig();

    @Data
    public static class MultiplierConfig {
        // Reduced from 35 to 15 for better saturation curve balance
        // This allows scores to grow more naturally in the middle range
        private double saturationScalingFactor = 15.0;

        private ContentDiscoveryMultiplierConfig contentDiscoveryMultiplier = new ContentDiscoveryMultiplierConfig();
        private InteractionTypeMultiplierConfig interactionTypeMultiplier = new InteractionTypeMultiplierConfig();

        @Data
        public static class ContentDiscoveryMultiplierConfig {
            // Reduced search multiplier from 2.0 to 1.8
            // Search shows intent but shouldn't dominate too heavily
            private double searchMultiplier = 1.8;

            // Reduced trending from 1.5 to 1.3
            // Trending content is more passive consumption
            private double trendingMultiplier = 1.3;

            // Increased recommendation from 1.0 to 1.2
            // Engaging with recommendations shows algorithm alignment
            private double recommendationMultiplier = 1.2;
        }

        @Data
        public static class InteractionTypeMultiplierConfig {
            // Reduced comment multiplier from 2.0 to 1.8
            // Comments are high engagement but 2.0 was too aggressive
            private double commentMultiplier = 1.8;

            // Kept like multiplier at 1.0 as baseline
            private double likeMultiplier = 1.0;

            // Changed report from -2.0 to -1.5
            // Reports are negative but -2.0 was too punitive
            // Could create topic avoidance too quickly
            private double reportMultiplier = -1.5;
        }
    }
}