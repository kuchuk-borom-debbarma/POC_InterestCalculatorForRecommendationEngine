package dev.kuku.interestcalculator.processors.scoreDecay;

/**
 * What does it do? <br>
 * Applies time-based decay to user topic scores with configurable threshold behavior,
 * naturally filtering weak interests to zero while preserving strong preferences
 * through flexible decay rate adjustments.
 * <p>
 * Why do we need it? <br>
 * Provides organic interest cleanup where weak topics get filtered out (reach 0)
 * before threshold period, creating natural "fresh start" opportunities. Strong
 * interests survive with configurable decay behavior post-threshold, balancing
 * preference retention with discovery promotion.
 * <p>
 * Method: <br>
 * 1. Calculate time elapsed since user's last interaction <br>
 * 2. Apply ratio-preserving decay - weakest topics naturally reach 0 first <br>
 * 3. If time delta exceeds threshold (e.g., 30 days): apply configurable strategy <br>
 *    - Pause decay (preserve remaining interests) <br>
 *    - Reduce decay rate significantly (slow preservation) <br>
 *    - Continue normal decay (eventual reset) <br>
 * 4. Business configures threshold point and post-threshold behavior
 * <p>
 * Natural Fresh Start: <br>
 * Before threshold reached, weak interests (lowest scores) naturally decay to 0,
 * removing them from topic pool completely. This creates organic cleanup without
 * losing strong preferences, giving users partial fresh slate for new discoveries.
 * <p>
 * Configurable Post-Threshold Strategies: <br>
 * <b>Pause:</b> Stop decay, preserve remaining strong interests <br>
 * <b>Slow Decay:</b> Reduce decay rate (e.g., 0.999 instead of 0.98) <br>
 * <b>Continue:</b> Maintain normal decay until complete reset
 * <p>
 * Example (30-day threshold, slow decay post-threshold): <br>
 * Day 0: [Gaming:9, Tech:6, Music:3, Sports:1] <br>
 * Day 20: [Gaming:6.2, Tech:4.1, Music:2.1, Sports:0] ← Sports filtered out <br>
 * Day 30: [Gaming:5.1, Tech:3.4, Music:1.7] (threshold reached, decay slows) <br>
 * Day 90: [Gaming:4.8, Tech:3.2, Music:1.6] ← Minimal further decay
 * <p>
 * Benefits: <br>
 * - Organic weak interest removal before threshold <br>
 * - Strong preference preservation with flexible control <br>
 * - Natural discovery opportunities without complete reset <br>
 * - Business-configurable engagement lifecycle management
 * <p>
 * Can be CRON job or on-demand calculation. Preferably CRON to have predictability.
 */
public class ThresholdScoreDecay {
}
