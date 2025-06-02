package dev.kuku.interestcalculator;

import dev.kuku.interestcalculator.models.ActivityLevel;
import org.yaml.snakeyaml.util.Tuple;

import java.util.Map;

public class Config {
    public static final Tuple<Long, Long> SCORE_RANGE = new Tuple<>(1L, 100L);
    // Scaling factors based on user activity level for cross-topic influence
    public static final Map<ActivityLevel, Double> ACTIVITY_SCALING_FACTORS = Map.of(ActivityLevel.NO_ACTIVITY, 1.5,      // Highest cross-topic influence
            ActivityLevel.LOW_ACTIVITY, 1.4, ActivityLevel.LOW_MID_ACTIVITY, 1.3, ActivityLevel.MID_ACTIVITY, 1.0,     // Baseline
            ActivityLevel.MID_HIGH_ACTIVITY, 0.8, ActivityLevel.HIGH_ACTIVITY, 0.6, ActivityLevel.NOLIFER_ACTIVITY, 0.4  // Lowest cross-topic influence
    );
    // Activity level thresholds based on weighted interaction scores
    public static final Map<ActivityLevel, Integer> ACTIVITY_THRESHOLDS = Map.of(ActivityLevel.NO_ACTIVITY, 0, ActivityLevel.LOW_ACTIVITY, 50, ActivityLevel.LOW_MID_ACTIVITY, 200, ActivityLevel.MID_ACTIVITY, 500, ActivityLevel.MID_HIGH_ACTIVITY, 1000, ActivityLevel.HIGH_ACTIVITY, 2000, ActivityLevel.NOLIFER_ACTIVITY, 5000);
}
