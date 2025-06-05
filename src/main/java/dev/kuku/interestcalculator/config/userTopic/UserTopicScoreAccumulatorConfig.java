package dev.kuku.interestcalculator.config.userTopic;

import lombok.Data;

@Data
public class UserTopicScoreAccumulatorConfig {
    boolean useContentDiscoveryMultiplier = true;
    boolean useInteractionTypeMultiplier = true;
    boolean useUserActivityScalingMultiplier = true;
    boolean usePastInteractionMultiplier = true;
    boolean applySaturation = true;
    private MultiplierConfig multiplier = new MultiplierConfig();
    @Data
    public static class MultiplierConfig {
        double contentDiscoveryMultiplier = 0.0;
        double interactionTypeMultiplier = 0.0;
        double userActivityScalingMultiplier = 0.0;
        double pastInteractionMultiplier = 0.0;
    }
}
