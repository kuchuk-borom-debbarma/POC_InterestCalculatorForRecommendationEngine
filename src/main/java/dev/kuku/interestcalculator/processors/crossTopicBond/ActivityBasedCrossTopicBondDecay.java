package dev.kuku.interestcalculator.processors.crossTopicBond;


/**
 * What does it do? <br>
 * Applies activity-aware decay to topic relationship bonds, considering both recency
 * AND volume of interactions. Relationships need sustained, meaningful activity from
 * multiple users to resist decay, not just occasional interactions from single users.
 * <p>
 * Why do we need it? <br>
 * The original approach was flawed: even one user occasionally interacting with a topic
 * pair would keep it "fresh" indefinitely. A single person playing Flappy Bird once a month
 * shouldn't keep Gaming↔FlappyBird artificially strong against modern Gaming↔Valorant.
 * We need both recency AND volume to determine true relationship health.
 * <p>
 * Method: <br>
 * 1. Track both last_updated timestamp AND recent activity metrics for each bond <br>
 * 2. Calculate activity_score based on interactions in recent time windows <br>
 * 3. Apply compound decay using both time-based and volume-based factors <br>
 * 4. Only relationships with sustained, multi-user activity resist aggressive decay <br>
 * 5. Remove relationships below minimum weight threshold <br>
 * 6. Preserve analytics metadata for trend analysis
 * <p>
 * Enhanced Formula: <br>
 * {@code days_since_update = current_time - last_updated} <br>
 * {@code activity_score = (interactions_7d × 1.0) + (interactions_30d × 0.5) + (unique_users_30d × 2.0)} <br>
 * {@code volume_penalty = max(0, 1.0 - (activity_score / expected_baseline_activity))} <br>
 * {@code time_penalty = min(days_since_update × 0.002, 0.08)} <br>
 * {@code combined_decay_rate = base_decay_rate × (1 - volume_penalty) × (1 - time_penalty)} <br>
 * {@code new_weight = current_weight × (combined_decay_rate ^ days_elapsed)} <br>
 * where {@code base_decay_rate = 0.98}, {@code expected_baseline_activity = 10.0}
 * <p>
 * Activity Score Components: <br>
 * - interactions_7d: Recent co-occurrences in past 7 days (weight: 1.0) <br>
 * - interactions_30d: Co-occurrences in past 30 days (weight: 0.5) <br>
 * - unique_users_30d: Distinct users creating this relationship (weight: 2.0) <br>
 * - Expected baseline: 10.0 (represents healthy relationship activity) <br>
 * <p>
 * Example Scenarios: <br>
 * Gaming↔Valorant: 50 interactions/7d, 15 unique users → activity_score = 80.0 → strong protection <br>
 * Gaming↔CSGO: 20 interactions/7d, 8 unique users → activity_score = 36.0 → moderate protection <br>
 * Gaming↔FlappyBird: 2 interactions/30d, 1 unique user → activity_score = 3.0 → aggressive decay <br>
 * Gaming↔DeadGame: 0 interactions/30d, 0 unique users → activity_score = 0.0 → maximum decay <br>
 * <p>
 * Decay Rate Results: <br>
 * - High activity (score > 20): decay_rate ≈ 0.975-0.98 (minimal decay) <br>
 * - Moderate activity (score 5-20): decay_rate ≈ 0.94-0.975 (gradual decay) <br>
 * - Low activity (score < 5): decay_rate ≈ 0.90-0.94 (aggressive decay) <br>
 * - No activity: decay_rate ≈ 0.90 (maximum decay) <br>
 * <p>
 * Benefits of Volume-Aware Approach: <br>
 * - Single-user interactions can't artificially sustain dead relationships <br>
 * - Requires community consensus (multiple users) for relationship persistence <br>
 * - Recent activity weighted more heavily than historical patterns <br>
 * - Viral/trending content gets natural boost through high unique user count <br>
 * - Niche but active communities can maintain their relationships <br>
 * - Seasonal content properly fades when activity drops, returns when active <br>
 * - System adapts to both user count growth and content trend shifts <br>
 * <p>
 * Additional Considerations: <br>
 * - Track interaction quality/type (explicit vs implicit signals) <br>
 * - Consider user engagement depth, not just interaction count <br>
 * - Implement relationship "hibernation" instead of deletion for seasonal topics <br>
 * - Use rolling windows for activity calculation to smooth temporary spikes/drops
 * <p>
 * Runs daily to maintain graph health and ensure recommendations reflect genuine,
 * sustained community interest patterns rather than historical artifacts or single-user noise.
 */
public class ActivityBasedCrossTopicBondDecay {
}
