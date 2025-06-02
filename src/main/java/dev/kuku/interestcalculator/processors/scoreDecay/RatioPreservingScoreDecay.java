package dev.kuku.interestcalculator.processors.scoreDecay;

/**
 * What does it do? <br>
 * Applies ratio-preserving decay to all user topic scores based on interaction time gaps,
 * maintaining relative preference hierarchy while reducing absolute values over time.
 * <p>
 * Why do we need it? <br>
 * Traditional decay methods either reset interests to zero (losing user preferences) or
 * apply uniform decay (breaking relative rankings). This approach preserves what users
 * care about most while naturally filtering out weak interests and allowing room for
 * new discoveries when they return after long periods of inactivity.
 * <p>
 * Method: <br>
 * 1. Calculate time elapsed since user's last interaction <br>
 * 2. Find min/max scores across all user's topics to establish current range <br>
 * 3. Apply proportional decay: normalize each score to 0-1 ratio, apply time-based decay
 *    to the maximum, then reconstruct scores maintaining exact ratios <br>
 * 4. Result: lowest topic becomes 0, highest retains meaningful value, hierarchy preserved
 * <p>
 * Formula: <br>
 * {@code new_score = ((original_score - min_score) / (max_score - min_score)) × (max_score × decay_factor)} <br>
 * where {@code decay_factor = 0.98^days_elapsed}
 * <p>
 * Example: <br>
 * User inactive for 60 days with topics [Gaming:9, Tech:6, Music:3, Sports:1] <br>
 * After decay: [Gaming:4.05, Tech:2.7, Music:1.35, Sports:0] <br>
 * Ratios preserved: Gaming still 1.5x Tech, Tech still 2x Music, Sports filtered out
 * <p>
 * Benefits: <br>
 * - Strong preferences survive long inactivity periods <br>
 * - Weak interests get naturally pruned (become 0) <br>
 * - Relative rankings remain intact <br>
 * - Fast rebuilding when user returns (boost existing strong interests) <br>
 * - No computational waste during inactive periods (on-demand calculation)
 *
 * Has to be calculated per interaction
 */
public class RatioPreservingScoreDecay {
}
