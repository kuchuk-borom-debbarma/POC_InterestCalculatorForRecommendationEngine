package dev.kuku.interestcalculator.services.platform;

import dev.kuku.interestcalculator.models.InteractionType;
import org.springframework.stereotype.Service;
import reactor.util.function.Tuple2;

import java.util.Map;
import java.util.HashMap;

@Service
public class PlatformService {
    public Map<InteractionType, Integer> getInteractionWeight(Tuple2<Integer, Integer> range) {
        // Extract min and max from the range
        int min = range.getT1();
        int max = range.getT2();
        
        // Calculate the weights based on the README guidelines
        // REACTION ~ Likes & Reactions: Medium weight (1.0-2.0)
        // COMMENT ~ Comments & Replies: High weight (3.0-5.0)
        // SHARE ~ Shares & Reposts: Medium-High weight (2.5-3.5)
        // VIEW ~ Views & Clicks: Low weight (0.1-0.5)
        
        Map<InteractionType, Integer> weights = new HashMap<>();
        
        // Using values within the range, with appropriate distribution
        weights.put(InteractionType.REACTION, calculateValueInRange(min, max, 0.3)); // Medium
        weights.put(InteractionType.COMMENT, calculateValueInRange(min, max, 0.9));  // High
        weights.put(InteractionType.SHARE, calculateValueInRange(min, max, 0.7));    // Medium-High
        weights.put(InteractionType.VIEW, calculateValueInRange(min, max, 0.1));     // Low
        
        return weights;
    }

    /**
     * Calculates a value within the range based on the percentage.
     * @param min Minimum value of the range
     * @param max Maximum value of the range
     * @param percentage A value between 0.0 and 1.0 representing where in the range the result should fall
     * @return An integer value within the range
     */
    private int calculateValueInRange(int min, int max, double percentage) {
        return min + (int) Math.round((max - min) * percentage);
    }
}