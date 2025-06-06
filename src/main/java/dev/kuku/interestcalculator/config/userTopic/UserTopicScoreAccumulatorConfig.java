package dev.kuku.interestcalculator.config.userTopic;

import lombok.Data;

@Data
public class UserTopicScoreAccumulatorConfig {
    /// Recommended to keep it enabled
    boolean contentDiscoveryMultiplierEnabled = true;
    /// Recommended to keep it enabled. This helps topic to be either an "interest" or "disinterest".
    boolean interactionTypeMultiplierEnabled = true;
    /// Primarily used for scaling score based on user activity.<br>
    /// Casual users will have a higher score to compensate for decays due to lower activity while power users have lower score as their score naturally goes up due to heavy interaction.
    boolean userActivityScalingMultiplierEnabled = false;
    /// Used to scale a topic score based on past interactions. <br>
    /// More recent topic will have higher score. This promotes recent interest to build up.
    boolean topicRecencyMultiplier = true;
    /// The closer the score is to maxRange, the slower the score increases.
    boolean applySaturationEnabled = true;
    private MultiplierConfig multiplier = new MultiplierConfig();

    @Data
    public static class MultiplierConfig {
        private double saturationScalingFactor = 35;
        private ContentDiscoveryMultiplierConfig contentDiscoveryMultiplier = new ContentDiscoveryMultiplierConfig();
        private InteractionTypeMultiplierConfig interactionTypeMultiplier = new InteractionTypeMultiplierConfig();

        @Data
        public static class ContentDiscoveryMultiplierConfig {
            private double searchMultiplier = 2.0;
            private double trendingMultiplier = 1.5;
            private double recommendationMultiplier = 1.0;
        }

        @Data
        public static class InteractionTypeMultiplierConfig {
            private double commentMultiplier = 2.0;
            private double likeMultiplier = 1.0;
            private double reportMultiplier = -2.0;
        }
    }
}
