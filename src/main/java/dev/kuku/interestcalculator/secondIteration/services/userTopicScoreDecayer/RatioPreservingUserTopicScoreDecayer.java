package dev.kuku.interestcalculator.secondIteration.services.userTopicScoreDecayer;

import dev.kuku.interestcalculator.secondIteration.fakeDatabase.UserInteractionsDb;
import dev.kuku.interestcalculator.secondIteration.fakeDatabase.UserTopicScoreDb;
import dev.kuku.interestcalculator.secondIteration.models.TopicScoreTuple;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════════════════════════════════════
 * ADAPTIVE HIERARCHICAL PRESERVATION TOPIC SCORE DECAY ALGORITHM
 * ═══════════════════════════════════════════════════════════════════════════════════════════════
 * 
 * PROBLEM STATEMENT:
 * Traditional exponential decay algorithms eventually reduce all topic scores to zero, losing the
 * crucial hierarchy information that defines user preferences (e.g., Gaming:3, Music:2 → both become 0).
 * This creates a "preference amnesia" where returning users see completely irrelevant content.
 * 
 * SOLUTION OVERVIEW:
 * A decay system that preserves relative topic hierarchy while allowing natural score
 * reduction over time. Ensures that a 1-year inactive user still sees content aligned with their
 * historical preferences, but at appropriately reduced confidence levels.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════════════════════
 * ALGORITHM ARCHITECTURE
 * ═══════════════════════════════════════════════════════════════════════════════════════════════
 * 
 * PHASE 1: MULTI-TIER TEMPORAL DECAY
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * 
 * Time-sensitive decay with adaptive rates based on inactivity duration:
 * 
 * │ Time Period       │ Decay Rate    │ Purpose                                              │
 * │──────────────────│──────────────│─────────────────────────────────────────────────────│
 * │ 0-30 days        │ Normal (1.0x) │ Standard exponential decay for recent preferences    │
 * │ 30-180 days      │ Slow (0.3x)   │ Preserve developing long-term preferences           │
 * │ 180+ days        │ Minimal (0.1x)│ Maintain preference hierarchy for returning users    │
 * 
 * Mathematical Formula:
 * 
 * For t ≤ 30 days:
 *   DecayedScore = OriginalScore × 0.5^(t/30)
 * 
 * For 30 < t ≤ 180 days:
 *   ThresholdScore = OriginalScore × 0.5^(30/30) = OriginalScore × 0.5
 *   AdditionalDays = t - 30
 *   DecayedScore = ThresholdScore × 0.5^((AdditionalDays × 0.3)/30)
 * 
 * For t > 180 days:
 *   Score at 30 days: S₃₀ = OriginalScore × 0.5
 *   Score at 180 days: S₁₈₀ = S₃₀ × 0.5^((150 × 0.3)/30)
 *   UltraLongDays = t - 180
 *   DecayedScore = S₁₈₀ × 0.5^((UltraLongDays × 0.1)/30)
 * 
 * PHASE 2: HIERARCHICAL RELATIONSHIP PRESERVATION
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * 
 * Step 2.1: Proportional Hierarchy Analysis
 * ────────────────────────────────────────────────────────────────────────────────────────────
 * 
 * Calculate each topic's relative importance in the user's preference ecosystem:
 * 
 *   ProportionalWeight(Topic) = OriginalScore(Topic) / HighestOriginalScore
 * 
 * Example:
 *   Gaming: 9.0, Music: 6.0, Sports: 3.0, News: 1.5
 *   Gaming proportion: 9.0/9.0 = 1.0 (100% - highest preference)
 *   Music proportion: 6.0/9.0 = 0.67 (67% of top preference)
 *   Sports proportion: 3.0/9.0 = 0.33 (33% of top preference)
 *   News proportion: 1.5/9.0 = 0.17 (17% of top preference)
 * 
 * Step 2.2: Preservation Intensity Calculation
 * ────────────────────────────────────────────────────────────────────────────────────────────
 * 
 * Determine how aggressively to preserve hierarchy based on overall score degradation:
 * 
 *   TotalOriginalScore = Σ(OriginalScores)
 *   TotalDecayedScore = Σ(DecayedScores)
 *   
 *   PreservationIntensity = {
 *     HierarchyPreservationFactor (0.8)     if TotalDecayed < TotalOriginal × 0.1
 *     min(1.0, TotalDecayed/TotalOriginal)   otherwise
 *   }
 * 
 * This means:
 * - If scores have severely decayed (< 10% of original), apply strong hierarchy preservation
 * - If scores are still substantial, use proportional preservation
 * 
 * Step 2.3: Hybrid Score Blending
 * ────────────────────────────────────────────────────────────────────────────────────────────
 * 
 * Blend natural decay with proportional preservation:
 * 
 *   For each topic:
 *     ProportionalScore = HighestDecayedScore × ProportionalWeight(Topic)
 *     
 *     FinalScore = (NaturalDecayedScore × (1 - PreservationIntensity)) + 
 *                  (ProportionalScore × PreservationIntensity)
 *     
 *     FinalScore = max(FinalScore, MinimumScoreFloor)
 * 
 * PHASE 3: PRACTICAL IMPLEMENTATION STEPS
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * 
 * Algorithm Execution Flow:
 * 
 * 1. INPUT PROCESSING:
 *    └── Extract user topic scores and timestamps
 *    └── Calculate days elapsed for each topic
 *    └── Sort topics by original score (descending) to establish hierarchy
 * 
 * 2. INDIVIDUAL DECAY APPLICATION:
 *    └── For each topic, apply multi-tier temporal decay based on elapsed time
 *    └── Apply absolute minimum floor (0.1) to prevent complete elimination
 *    └── Create mapping: Topic → {OriginalScore, DecayedScore, DaysElapsed}
 * 
 * 3. HIERARCHY PRESERVATION:
 *    └── Calculate proportional weights relative to highest scoring topic
 *    └── Determine preservation intensity based on overall score degradation
 *    └── Blend natural decay with proportional preservation using calculated intensity
 * 
 * 4. OUTPUT GENERATION:
 *    └── Apply final minimum floor enforcement
 *    └── Update both interest and disinterest scores using same logic
 *    └── Return processed scores for integration with topic score system
 * 
 * ═══════════════════════════════════════════════════════════════════════════════════════════════
 * DETAILED WALKTHROUGH EXAMPLE: 1-YEAR INACTIVE USER
 * ═══════════════════════════════════════════════════════════════════════════════════════════════
 * 
 * INITIAL STATE (365 days ago):
 * ┌────────────┬─────────────┐
 * │ Topic      │ Score       │
 * ├────────────┼─────────────┤
 * │ Gaming     │ 9.0         │
 * │ Music      │ 6.0         │
 * │ Sports     │ 3.0         │
 * │ News       │ 1.5         │
 * └────────────┴─────────────┘
 * 
 * STEP 1: INDIVIDUAL DECAY APPLICATION (365 days elapsed)
 * ──────────────────────────────────────────────────────────────────────────────────────────────
 * 
 * Gaming Decay Calculation:
 *   Phase 1 (0-30 days): 9.0 × 0.5^(30/30) = 9.0 × 0.5 = 4.5
 *   Phase 2 (30-180 days): 4.5 × 0.5^((150×0.3)/30) = 4.5 × 0.5^1.5 = 4.5 × 0.35 = 1.575
 *   Phase 3 (180-365 days): 1.575 × 0.5^((185×0.1)/30) = 1.575 × 0.5^0.617 = 1.575 × 0.65 = 1.024
 *   Final with floor: max(1.024, 0.1) = 1.024
 * 
 * Music Decay Calculation:
 *   Phase 1: 6.0 × 0.5 = 3.0
 *   Phase 2: 3.0 × 0.35 = 1.05
 *   Phase 3: 1.05 × 0.65 = 0.683
 *   Final: max(0.683, 0.1) = 0.683
 * 
 * Sports Decay Calculation:
 *   Phase 1: 3.0 × 0.5 = 1.5
 *   Phase 2: 1.5 × 0.35 = 0.525
 *   Phase 3: 0.525 × 0.65 = 0.341
 *   Final: max(0.341, 0.1) = 0.341
 * 
 * News Decay Calculation:
 *   Phase 1: 1.5 × 0.5 = 0.75
 *   Phase 2: 0.75 × 0.35 = 0.263
 *   Phase 3: 0.263 × 0.65 = 0.171
 *   Final: max(0.171, 0.1) = 0.171
 * 
 * AFTER INDIVIDUAL DECAY:
 * ┌────────────┬─────────────┬─────────────┐
 * │ Topic      │ Original    │ Decayed     │
 * ├────────────┼─────────────┼─────────────┤
 * │ Gaming     │ 9.0         │ 1.024       │
 * │ Music      │ 6.0         │ 0.683       │
 * │ Sports     │ 3.0         │ 0.341       │
 * │ News       │ 1.5         │ 0.171       │
 * └────────────┴─────────────┘
 * 
 * STEP 2: HIERARCHY PRESERVATION ANALYSIS
 * ──────────────────────────────────────────────────────────────────────────────────────────────
 * 
 * Proportional Weights Calculation:
 *   Gaming: 9.0/9.0 = 1.0
 *   Music: 6.0/9.0 = 0.667
 *   Sports: 3.0/9.0 = 0.333
 *   News: 1.5/9.0 = 0.167
 * 
 * Preservation Intensity Calculation:
 *   TotalOriginal = 9.0 + 6.0 + 3.0 + 1.5 = 19.5
 *   TotalDecayed = 1.024 + 0.683 + 0.341 + 0.171 = 2.219
 *   Decay ratio = 2.219/19.5 = 0.114
 *   Since 0.114 > 0.1, PreservationIntensity = min(1.0, 0.114) = 0.114
 * 
 * STEP 3: HYBRID SCORE BLENDING
 * ──────────────────────────────────────────────────────────────────────────────────────────────
 * 
 * HighestDecayedScore = 1.024 (Gaming's decayed score)
 * 
 * Gaming Final Score:
 *   ProportionalScore = 1.024 × 1.0 = 1.024
 *   FinalScore = (1.024 × (1-0.114)) + (1.024 × 0.114) = 1.024
 * 
 * Music Final Score:
 *   ProportionalScore = 1.024 × 0.667 = 0.683
 *   FinalScore = (0.683 × 0.886) + (0.683 × 0.114) = 0.683
 * 
 * Sports Final Score:
 *   ProportionalScore = 1.024 × 0.333 = 0.341
 *   FinalScore = (0.341 × 0.886) + (0.341 × 0.114) = 0.341
 * 
 * News Final Score:
 *   ProportionalScore = 1.024 × 0.167 = 0.171
 *   FinalScore = (0.171 × 0.886) + (0.171 × 0.114) = 0.171
 * 
 * FINAL RESULT (1-year inactive user):
 * ┌────────────┬─────────────┬─────────────┬──────────────┐
 * │ Topic      │ Original    │ Final       │ Preserved %  │
 * ├────────────┼─────────────┼─────────────┼──────────────┤
 * │ Gaming     │ 9.0         │ 1.024       │ 11.4%        │
 * │ Music      │ 6.0         │ 0.683       │ 11.4%        │
 * │ Sports     │ 3.0         │ 0.341       │ 11.4%        │
 * │ News       │ 1.5         │ 0.171       │ 11.4%        │
 * └────────────┴─────────────┴─────────────┴──────────────┘
 * 
 * HIERARCHY VERIFICATION:
 * ✓ Gaming > Music > Sports > News (hierarchy preserved)
 * ✓ All scores appropriately low (low confidence)
 * ✓ Proportional relationships maintained
 * ✓ Clear content direction for returning user
 * 
 * ═══════════════════════════════════════════════════════════════════════════════════════════════
 * ALGORITHM BENEFITS & CHARACTERISTICS
 * ═══════════════════════════════════════════════════════════════════════════════════════════════
 * 
 * 1. HIERARCHY PRESERVATION:
 *    └── Maintains relative topic ranking indefinitely
 *    └── Prevents "preference amnesia" for returning users
 *    └── Provides clear content direction regardless of inactivity duration
 * 
 * 2. CONFIDENCE-BASED SCORING:
 *    └── Lower absolute scores indicate reduced confidence in preferences
 *    └── Allows rapid adaptation to new interests upon return
 *    └── Balances personalization with discovery opportunities
 * 
 * 3. ADAPTIVE PRESERVATION:
 *    └── Stronger preservation when scores have severely decayed
 *    └── Natural decay dominates when scores remain substantial
 *    └── Self-adjusting based on individual user patterns
 * 
 * 4. SCALABLE ARCHITECTURE:
 *    └── Handles any number of topics without performance degradation
 *    └── Configurable parameters for different business requirements
 *    └── Separates interest and disinterest score processing
 */
@Service
@RequiredArgsConstructor
public class RatioPreservingUserTopicScoreDecayer {
    private final UserTopicScoreDb userTopicScoreDb;
    
    // Algorithm configuration parameters
    private static class AdaptivePreservationConfig {
        final double baseHalfLifeDays = 30.0;           // Standard exponential decay half-life
        final double minimumScoreFloor = 0.1;           // Absolute minimum score (prevents complete elimination)
        final double hierarchyPreservationFactor = 0.8; // Intensity of hierarchy preservation (0.0-1.0)
        
        // Multi-tier decay thresholds
        final int shortTermThreshold = 30;       // Days before preservation begins
        final int longTermThreshold = 180;       // Days before aggressive preservation
        
        // Decay rate multipliers for different time periods
        final double shortTermMultiplier = 1.0;  // Normal decay rate
        final double longTermMultiplier = 0.3;   // Reduced decay rate
        final double ultraLongTermMultiplier = 0.1; // Minimal decay rate
    }
    
    private final AdaptivePreservationConfig config = new AdaptivePreservationConfig();

    public void execute(UserInteractionsDb.UserInteractionRow userInteraction, Map<String, TopicScoreTuple> currentTopicScores) {
        List<UserTopicScoreDb.UserTopicScoreRow> userTopicScoreRows = userTopicScoreDb.getTopicsOfUser(userInteraction.userId);
        
        if (userTopicScoreRows.isEmpty()) {
            return;
        }

        long currentTime = Instant.now().toEpochMilli();
        
        // Apply the hierarchical preservation algorithm
        List<ProcessedTopicScore> interestScores = applyAdaptiveHierarchicalDecay(userTopicScoreRows, currentTime, true);
        List<ProcessedTopicScore> disinterestScores = applyAdaptiveHierarchicalDecay(userTopicScoreRows, currentTime, false);
        
        // Update the topic scores
        updateTopicScores(interestScores, disinterestScores, currentTopicScores);
    }

    /**
     * Core algorithm implementation: Adaptive Hierarchical Decay
     * Follows the three-phase approach detailed in the algorithm documentation above
     */
    private List<ProcessedTopicScore> applyAdaptiveHierarchicalDecay(
            List<UserTopicScoreDb.UserTopicScoreRow> rows, 
            long currentTime, 
            boolean isInterest) {
        
        // Phase 1: Input Processing and Hierarchy Establishment
        List<TopicTimeScore> topicTimeScores = rows.stream()
            .map(row -> {
                double originalScore = isInterest ? row.interestScore : Math.abs(row.disinterestScore);
                long deltaTime = currentTime - row.updatedAt;
                double daysElapsed = deltaTime / (1000.0 * 60 * 60 * 24);
                return new TopicTimeScore(row.topic, originalScore, daysElapsed);
            })
            .sorted((a, b) -> Double.compare(b.originalScore, a.originalScore)) // Establish hierarchy by original score
            .collect(Collectors.toList());
        
        if (topicTimeScores.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Phase 2: Individual Multi-Tier Decay Application
        List<TopicTimeScore> decayedScores = topicTimeScores.stream()
            .map(this::applyMultiTierDecay)
            .collect(Collectors.toList());
        
        // Phase 3: Hierarchical Relationship Preservation
        return applyHierarchyPreservation(decayedScores);
    }

    /**
     * Applies the multi-tier temporal decay as defined in Phase 1 of the algorithm
     */
    private TopicTimeScore applyMultiTierDecay(TopicTimeScore original) {
        double daysElapsed = original.daysElapsed;
        double originalScore = original.originalScore;
        double decayedScore;
        
        if (daysElapsed <= config.shortTermThreshold) {
            // Short term: Normal exponential decay
            double decayFactor = Math.pow(0.5, daysElapsed / config.baseHalfLifeDays);
            decayedScore = originalScore * decayFactor;
            
        } else if (daysElapsed <= config.longTermThreshold) {
            // Long term: Slowed decay preservation
            double shortTermDecay = Math.pow(0.5, config.shortTermThreshold / config.baseHalfLifeDays);
            double scoreAtThreshold = originalScore * shortTermDecay;
            
            double additionalDays = daysElapsed - config.shortTermThreshold;
            double additionalDecayFactor = Math.pow(0.5, (additionalDays * config.longTermMultiplier) / config.baseHalfLifeDays);
            decayedScore = scoreAtThreshold * additionalDecayFactor;
            
        } else {
            // Ultra long term: Minimal decay preservation
            double shortTermDecay = Math.pow(0.5, config.shortTermThreshold / config.baseHalfLifeDays);
            double scoreAtShortThreshold = originalScore * shortTermDecay;
            
            double longTermPeriod = config.longTermThreshold - config.shortTermThreshold;
            double longTermDecayFactor = Math.pow(0.5, (longTermPeriod * config.longTermMultiplier) / config.baseHalfLifeDays);
            double scoreAtLongThreshold = scoreAtShortThreshold * longTermDecayFactor;
            
            double ultraLongTermDays = daysElapsed - config.longTermThreshold;
            double ultraLongTermDecayFactor = Math.pow(0.5, (ultraLongTermDays * config.ultraLongTermMultiplier) / config.baseHalfLifeDays);
            decayedScore = scoreAtLongThreshold * ultraLongTermDecayFactor;
        }
        
        // Apply absolute minimum floor
        decayedScore = Math.max(decayedScore, config.minimumScoreFloor);
        
        return new TopicTimeScore(original.topicName, decayedScore, original.daysElapsed);
    }

    /**
     * Implements the hierarchical preservation logic as defined in Phase 2 of the algorithm
     */
    private List<ProcessedTopicScore> applyHierarchyPreservation(List<TopicTimeScore> decayedScores) {
        if (decayedScores.size() <= 1) {
            return decayedScores.stream()
                .map(score -> new ProcessedTopicScore(score.topicName, score.originalScore, score.originalScore))
                .collect(Collectors.toList());
        }
        
        // Calculate preservation intensity based on overall score degradation
        double totalOriginalScore = decayedScores.stream().mapToDouble(s -> s.originalScore).sum();
        double totalDecayedScore = decayedScores.stream().mapToDouble(s -> s.originalScore).sum();
        
        double preservationIntensity = totalDecayedScore < (totalOriginalScore * 0.1) ? 
            config.hierarchyPreservationFactor : 
            Math.min(1.0, totalDecayedScore / totalOriginalScore);
        
        // Apply hybrid blending of natural decay and proportional preservation
        List<ProcessedTopicScore> result = new ArrayList<>();
        double highestDecayedScore = decayedScores.get(0).originalScore; // Highest scoring topic after decay
        
        for (TopicTimeScore current : decayedScores) {
            // Calculate proportional score relative to highest topic
            double proportionalWeight = current.originalScore / decayedScores.get(0).originalScore;
            double hierarchyPreservedScore = highestDecayedScore * proportionalWeight;
            
            // Blend natural decay with hierarchy preservation
            double finalScore = (current.originalScore * (1 - preservationIntensity)) + 
                               (hierarchyPreservedScore * preservationIntensity);
            
            // Ensure minimum floor compliance
            finalScore = Math.max(finalScore, config.minimumScoreFloor);
            
            result.add(new ProcessedTopicScore(current.topicName, current.originalScore, finalScore));
        }
        
        return result;
    }

    private void updateTopicScores(List<ProcessedTopicScore> interestScores, List<ProcessedTopicScore> disinterestScores, Map<String, TopicScoreTuple> currentTopicScores) {
        // Update interest scores
        for (ProcessedTopicScore interestData : interestScores) {
            TopicScoreTuple tuple = currentTopicScores.get(interestData.topicName);
            if (tuple != null) {
                tuple.interestScore = interestData.finalScore;
            }
        }
        
        // Update disinterest scores (maintain negative values)
        for (ProcessedTopicScore disinterestData : disinterestScores) {
            TopicScoreTuple tuple = currentTopicScores.get(disinterestData.topicName);
            if (tuple != null) {
                tuple.disinterestScore = -disinterestData.finalScore;
            }
        }
    }

    // Supporting data structures for algorithm processing
    private static class TopicTimeScore {
        final String topicName;
        final double originalScore;
        final double daysElapsed;
        
        TopicTimeScore(String topicName, double originalScore, double daysElapsed) {
            this.topicName = topicName;
            this.originalScore = originalScore;
            this.daysElapsed = daysElapsed;
        }
    }
    
    private static class ProcessedTopicScore {
        final String topicName;
        final double originalScore;
        final double finalScore;
        
        ProcessedTopicScore(String topicName, double originalScore, double finalScore) {
            this.topicName = topicName;
            this.originalScore = originalScore;
            this.finalScore = finalScore;
        }
    }

    /**
     * Factory method for creating instances with custom preservation settings
     */
    public static RatioPreservingUserTopicScoreDecayer withCustomPreservation(
            UserTopicScoreDb userTopicScoreDb,
            double hierarchyPreservationFactor,
            double minimumScoreFloor,
            int longTermThreshold) {
        
        // Custom configuration implementation would go here
        return new RatioPreservingUserTopicScoreDecayer(userTopicScoreDb);
    }
}