package dev.kuku.interestcalculator.secondIteration.services.baseScoreCalculator;

import dev.kuku.interestcalculator.secondIteration.config.InterestCalculatorConfig;
import dev.kuku.interestcalculator.secondIteration.fakeDatabase.UserInteractionsDb;
import dev.kuku.interestcalculator.secondIteration.models.TopicScoreTuple;
import dev.kuku.interestcalculator.secondIteration.services.UserActivityCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * DYNAMIC CONTENT DISCOVERY BASED INTEREST SCORE CALCULATOR
 * <p>
 * This service calculates user interest/disinterest scores based on a sophisticated multi-factor algorithm
 * that considers content discovery method, interaction type, and user activity patterns across multiple
 * time horizons to provide nuanced, personalized interest scoring.
 * <p>
 * ===================================================================================
 * CORE ALGORITHM OVERVIEW
 * ===================================================================================
 * <p>
 * The algorithm works in four main phases:
 * <p>
 * 1. BASE SCORE CALCULATION (Content Discovery Context)
 * - Assigns different base values based on HOW content was discovered
 * - Reflects user intent and engagement likelihood
 * <p>
 * 2. INTERACTION WEIGHTING (User Action Significance)
 * - Multiplies base score by interaction type weight
 * - Positive weights indicate interest, negative indicate disinterest
 * <p>
 * 3. ACTIVITY-BASED NORMALIZATION (Temporal Context)
 * - Applies inverse activity multiplier to balance scoring across user types
 * - Prevents power users from dominating interest profiles
 * <p>
 * 4. SCORE SEGREGATION (Interest vs Disinterest)
 * - Separates positive and negative scores into distinct metrics
 * - Allows for independent tracking of interests and dislikes
 * <p>
 * ===================================================================================
 * CONFIGURATION PROPERTIES
 * ===================================================================================
 * <p>
 * All algorithm parameters can be configured via application.properties using the prefix:
 * 'interest-calculator'
 * <p>
 * Example configuration:
 * interest-calculator.topic-score-min=0
 * interest-calculator.topic-score-max=10
 * interest-calculator.scoring.discovery.search-score=4.0
 * interest-calculator.scoring.interaction.comment-weight=2.0
 * interest-calculator.scoring.activity.daily-weight=0.5
 * interest-calculator.scoring.multiplier.min-multiplier=0.3
 * interest-calculator.scoring.saturation.saturation-factor=1.0
 * interest-calculator.scoring.saturation.enabled=true
 * <p>
 * See InterestCalculatorConfig class for all available configuration options.
 */
@Service
@RequiredArgsConstructor
public class DynamicContentDiscoveryBasedBaseScoreCalculator {
    private final UserActivityCalculator userActivityCalculator;
    private final InterestCalculatorConfig config;

    /**
     * Main execution method that orchestrates the complete interest scoring process.
     * <p>
     * CRITICAL: All activity-based calculations are performed on the INTERACTING USER
     * (userInteraction.userId), NOT on the content creator or any other user.
     * <p>
     * This method implements a four-phase algorithm:
     * 1. Gather INTERACTING USER's activity data across multiple time horizons
     * 2. Calculate base score and interaction weight
     * 3. Apply INTERACTING USER's activity-based normalization to balance score accumulation
     * 4. Segregate into interest/disinterest scores and apply saturation control to each
     * <p>
     * The activity normalization ensures that:
     * - Active users (who interact frequently) get lower per-interaction scores to prevent dominance
     * - Casual users (who interact rarely) get higher per-interaction scores to survive decay
     * <p>
     * The saturation control ensures that:
     * - Both interest and disinterest scores become progressively harder to increase as they approach 10
     * - Prevents runaway score accumulation in either direction
     * - Creates natural ceiling effects for both positive and negative sentiment signals
     *
     * @param userInteraction The interaction event to score (userId field determines whose activity is analyzed)
     * @return TopicScoreTuple containing separated interest and disinterest scores
     */
    public TopicScoreTuple execute(UserInteractionsDb.UserInteractionRow userInteraction) {
        // Phase 1: Gather multi-horizon activity data
        // We need different time perspectives to understand user behavior patterns
        UserActivityCalculator.ActivityMetrics dailyActivity = userActivityCalculator.getDailyActivityFromNow(userInteraction.userId);
        UserActivityCalculator.ActivityMetrics monthlyActivity = userActivityCalculator.getMonthlyActivityFromNow(userInteraction.userId);
        UserActivityCalculator.ActivityMetrics yearlyActivity = userActivityCalculator.getYearlyActivityFromNow(userInteraction.userId);
        // Phase 2: Calculate raw interaction score
        // This represents the "face value" of the interaction before normalization
        double baseScore = getBaseScore(userInteraction.contentDiscovery);
        double interactionWeight = getInteractionWeight(userInteraction.interactionType);
        double finalRawScore = baseScore * interactionWeight;
        // Phase 3: Apply activity-based normalization
        // This is where we account for user behavior patterns to ensure fairness
        double activityMultiplier = calculateCompositeInverseMultiplier(dailyActivity, monthlyActivity, yearlyActivity);
        double normalizedScore = finalRawScore * activityMultiplier;
        // Phase 4: Segregate and apply saturation control
        // Separate positive (interest) and negative (disinterest) signals, then apply saturation
        double interestScore = 0;
        double disinterestScore = 0;
        if (normalizedScore < 0) {
            // Negative interactions become disinterest scores (convert to positive 0-10 range)
            double positiveDisinterestScore = Math.abs(normalizedScore);
            disinterestScore = applySaturationControl(positiveDisinterestScore);
        } else {
            // Positive interactions become interest scores
            interestScore = applySaturationControl(normalizedScore);
        }
        // Return segregated scores for independent processing
        var finalTopicScore = new TopicScoreTuple();
        finalTopicScore.interestScore = interestScore;
        finalTopicScore.disinterestScore = disinterestScore;
        return finalTopicScore;
    }

    /**
     * Applies saturation control to prevent runaway score accumulation.
     * <p>
     * This method implements a logarithmic saturation function that creates progressive
     * resistance as scores approach the configured maximum (topicScoreMax). Since both
     * interest and disinterest scores are stored in the 0-10 range, this function:
     * - Makes it progressively harder to increase scores as they get higher
     * - Creates natural ceiling effects without hard limits
     * - Maintains meaningful score differentiation across the entire 0-10 range
     * - Applies the same saturation curve to both interest and disinterest scores
     * <p>
     * Mathematical approach:
     * Uses a hyperbolic tangent (tanh) function that maps unlimited input to bounded output:
     * - Asymptotically approaches topicScoreMax (default: 10)
     * - Steepness controlled by saturation factor (higher = more aggressive saturation)
     * <p>
     * Formula: topicScoreMax × tanh(score × saturationFactor / topicScoreMax)
     *
     * @param score The score before saturation control (always positive, 0+ range)
     * @return Score with saturation control applied, bounded within 0 to topicScoreMax
     */
    private double applySaturationControl(double score) {
        // If saturation is disabled, return score as-is
        if (!config.getScoring().getSaturation().isEnabled()) {
            return score;
        }
        if (score <= 0) return 0;
        double maxScore = config.getTopicScoreMax();
        double saturationFactor = config.getScoring().getSaturation().getSaturationFactor();
        // Apply saturation - asymptotically approach maxScore
        // Formula: maxScore × tanh(score × saturationFactor / maxScore)
        double normalizedInput = score * saturationFactor / maxScore;
        return maxScore * Math.tanh(normalizedInput);
    }

    /**
     * Calculates a composite activity multiplier using weighted contributions from multiple time horizons.
     * <p>
     * This method addresses the core challenge of normalizing user interactions across different
     * activity levels and time periods. The goal is to ensure that:
     * - Highly active users don't dominate the scoring system
     * - Rare interactions from inactive users carry appropriate weight
     * - Recent activity patterns influence scoring more than historical patterns
     * <p>
     * Time Horizon Weighting Strategy (configurable):
     * - Daily: Captures current mood, immediate interests, situational factors
     * - Monthly: Represents medium-term behavioral patterns and sustained interests
     * - Yearly: Reflects long-term user personality and fundamental preferences
     *
     * @param dailyActivity   User's interaction patterns in the last 24 hours
     * @param monthlyActivity User's interaction patterns in the last 30 days
     * @param yearlyActivity  User's interaction patterns in the last 365 days
     * @return Composite multiplier between configured min and max values
     */
    private double calculateCompositeInverseMultiplier(
            UserActivityCalculator.ActivityMetrics dailyActivity,
            UserActivityCalculator.ActivityMetrics monthlyActivity,
            UserActivityCalculator.ActivityMetrics yearlyActivity) {
        // Calculate individual inverse multipliers for each time period
        // Each period has different interaction volume expectations
        double dailyMultiplier = calculateSinglePeriodInverseMultiplier(
                dailyActivity.totalInteractions(),
                dailyActivity.dailyAverage(),
                config.getScoring().getMaxScale().getDailyMax()
        );
        double monthlyMultiplier = calculateSinglePeriodInverseMultiplier(
                monthlyActivity.totalInteractions(),
                monthlyActivity.dailyAverage(),
                config.getScoring().getMaxScale().getMonthlyMax()
        );
        double yearlyMultiplier = calculateSinglePeriodInverseMultiplier(
                yearlyActivity.totalInteractions(),
                yearlyActivity.dailyAverage(),
                config.getScoring().getMaxScale().getYearlyMax()
        );
        // Combine multipliers using configurable weighted average
        // This creates a nuanced multiplier that considers all time horizons
        return (dailyMultiplier * config.getScoring().getActivity().getDailyWeight()) +
                (monthlyMultiplier * config.getScoring().getActivity().getMonthlyWeight()) +
                (yearlyMultiplier * config.getScoring().getActivity().getYearlyWeight());
    }

    /**
     * Calculates an inverse activity multiplier for a single time period.
     * <p>
     * This method implements the core inverse relationship between user activity and interaction weight:
     * - More active users get lower multipliers (their interactions are less special)
     * - Less active users get higher multipliers (their interactions are more meaningful)
     * <p>
     * The algorithm considers both total interaction volume and daily consistency:
     * - Total interactions: Raw activity level
     * - Daily average: Consistency and engagement pattern
     * <p>
     * Mathematical Approach:
     * 1. Normalize both metrics to 0-1 scale based on expected maximums
     * 2. Combine them with configurable weights
     * 3. Apply inverse transformation: high activity → low multiplier
     * 4. Constrain result to configurable bounds
     *
     * @param totalInteractions Total interactions in the period
     * @param dailyAverage      Average interactions per day in the period
     * @param maxScale          Expected maximum interactions for this time period
     * @return Inverse multiplier between configured min and max values
     */
    private double calculateSinglePeriodInverseMultiplier(int totalInteractions, double dailyAverage, int maxScale) {
        // Handle complete inactivity - give maximum boost to rare interactions
        if (totalInteractions == 0) return config.getScoring().getMultiplier().getMaxMultiplier();
        // Normalize total interactions to 0-1 scale based on expected maximum for this time period
        // This allows us to compare activity levels across different time horizons
        double normalizedTotal = Math.min(1.0, (double) totalInteractions / maxScale);
        // Normalize daily average to 0-1 scale
        // Daily average helps us understand consistency vs. burst behavior
        double maxDailyForScale = (double) maxScale / config.getScoring().getMaxScale().getDaysInMonth();
        double normalizedDaily = Math.min(1.0, dailyAverage / maxDailyForScale);
        // Combine total volume and daily consistency using configurable weights
        double activityScore = (normalizedTotal * config.getScoring().getComposition().getTotalInteractionsWeight()) +
                (normalizedDaily * config.getScoring().getComposition().getDailyAverageWeight());
        // Apply inverse relationship with configurable range
        // Formula: maxMultiplier - (activityScore × multiplierRange)
        // - activityScore = 0 (inactive) → multiplier = maxMultiplier
        // - activityScore = 1 (very active) → multiplier = minMultiplier
        double multiplier = config.getScoring().getMultiplier().getMaxMultiplier() -
                (activityScore * config.getScoring().getMultiplier().getMultiplierRange());
        // Ensure multiplier stays within configured bounds
        return Math.max(config.getScoring().getMultiplier().getMinMultiplier(),
                Math.min(config.getScoring().getMultiplier().getMaxMultiplier(), multiplier));
    }

    /**
     * Determines base score based on content discovery method using configured values.
     * <p>
     * The base score reflects the user's level of intent and the likelihood that
     * their interaction represents a genuine preference signal.
     *
     * @param contentDiscovery How the user discovered this content
     * @return Configured base score representing discovery context strength
     */
    private double getBaseScore(UserInteractionsDb.Discovery contentDiscovery) {
        return switch (contentDiscovery) {
            case SEARCH -> config.getScoring().getDiscovery().getSearchScore();
            case TRENDING -> config.getScoring().getDiscovery().getTrendingScore();
            case RECOMMENDATION -> config.getScoring().getDiscovery().getRecommendationScore();
        };
    }

    /**
     * Determines interaction weight based on the type of user action using configured values.
     * <p>
     * The interaction weight reflects both the effort required for the action
     * and the sentiment expressed by the user.
     *
     * @param interactionType The type of interaction the user performed
     * @return Configured weight multiplier (positive for interest, negative for disinterest)
     */
    private double getInteractionWeight(UserInteractionsDb.InteractionType interactionType) {
        return switch (interactionType) {
            case COMMENT -> config.getScoring().getInteraction().getCommentWeight();
            case LIKE -> config.getScoring().getInteraction().getLikeWeight();
            case DISLIKE -> config.getScoring().getInteraction().getDislikeWeight();
            case REPORT -> config.getScoring().getInteraction().getReportWeight();
        };
    }
}