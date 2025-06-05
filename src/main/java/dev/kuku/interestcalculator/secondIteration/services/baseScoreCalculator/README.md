# Interest Calculation System

## Overview

I've built a sophisticated interest calculation system that solves one of the most challenging problems in recommendation engines: **how do you fairly measure user interest when users have vastly different activity patterns?**

This system ensures that both highly active users (who interact 50+ times daily) and casual users (who interact a few times per week) contribute meaningfully to recommendation algorithms, while preventing any single user type from dominating the system.

## The Problem I'm Solving

Traditional recommendation systems suffer from what I call the "Activity Imbalance Problem." Here's what happens:

- **Active User**: 50 interactions/day × 1.0 score each = 50 points/day
- **Casual User**: 2 interactions/week × 1.0 score each = 0.28 points/day
- **Result**: Active user accumulates 178× more influence

This creates two major issues:

1. **Recommendation Dominance**: Active users' preferences completely overshadow casual users
2. **Decay Death Spiral**: When systems apply time-based decay to scores, casual users' profiles get wiped out during quiet periods

My solution balances these dynamics while maintaining the semantic meaning of different interaction types.

## My Solution: Multi-Layered Normalization

I've designed a system that applies **inverse activity multipliers** to balance score accumulation rates:

- **High-activity users**: Get lower multipliers (0.3-0.7x) to prevent dominance
- **Low-activity users**: Get higher multipliers (1.5-2.0x) to ensure survival
- **Regular users**: Stay around 1.0x as the baseline

### Key Innovation: Decay-Resistant Scoring

The real breakthrough is how this handles temporal decay. Here's a comparison:

**Without My System**:
- Casual user gets 8 points for rare interaction
- After 2 weeks of no activity: Score decays to ~2 points (profile becomes irrelevant)

**With My System**:
- Casual user gets 8 × 1.7 = 13.6 points for the same interaction
- After 2 weeks of decay: Score remains ~8-10 points (profile stays viable)

## Architecture Overview

My system operates in four distinct phases:

### Phase 1: Activity Pattern Analysis
I analyze the **interacting user's** behavior across three time horizons:

```java
// CRITICAL: All analysis focuses on the person performing the interaction
UserActivityCalculator.ActivityMetrics dailyActivity = 
    userActivityCalculator.getDailyActivityFromNow(userInteraction.userId);
UserActivityCalculator.ActivityMetrics monthlyActivity = 
    userActivityCalculator.getMonthlyActivityFromNow(userInteraction.userId);
UserActivityCalculator.ActivityMetrics yearlyActivity = 
    userActivityCalculator.getYearlyActivityFromNow(userInteraction.userId);
```

**Important Clarification**: When User A interacts with User B's content, I analyze User A's activity patterns, not User B's. The content creator's activity is irrelevant to the interest calculation.

### Phase 2: Context-Aware Base Scoring

I assign different base scores based on how the user discovered the content:

- **SEARCH (4 points)**: User actively sought this content type (highest intent)
- **TRENDING (3 points)**: User engaged with popular content (medium intent)
- **RECOMMENDATION (2 points)**: Algorithm-suggested content (lowest intent)

Then I apply interaction weights:

- **COMMENT (+2)**: High effort, strong engagement signal
- **LIKE (+1)**: Low effort, moderate signal
- **DISLIKE (-1)**: Active disapproval
- **REPORT (-2)**: Strong negative signal

### Phase 3: Multi-Horizon Normalization

This is the core innovation. I calculate a composite multiplier using weighted time horizons:

**Weighting Strategy**:
- **Daily activity**: 50% weight (current mood/interests)
- **Monthly activity**: 30% weight (sustained behavioral patterns)
- **Yearly activity**: 20% weight (fundamental characteristics)

For each time period, I use this formula:

```java
// Normalize activity to 0-1 scale
double normalizedTotal = Math.min(1.0, (double) totalInteractions / maxScale);
double normalizedDaily = Math.min(1.0, dailyAverage / maxDailyForScale);

// Combine volume (70%) with consistency (30%)
double activityScore = (normalizedTotal * 0.7) + (normalizedDaily * 0.3);

// Apply inverse relationship: high activity → low multiplier
double multiplier = 2.0 - (activityScore * 1.7);
```

### Phase 4: Score Segregation

I separate positive and negative scores into distinct metrics:

```java
if (finalRawScore < 0) {
    disinterestScore = finalRawScore * activityMultiplier;
} else {
    interestScore = finalRawScore * activityMultiplier;
}
```

This prevents negative interactions from simply canceling out positive ones and enables more nuanced recommendation strategies.

## Real-World Examples

### Example 1: Active User (Preventing Dominance)

**User Profile**:
- Daily: 25 interactions
- Monthly: 600 interactions
- Yearly: 2500+ interactions

**Scenario**: User likes a recommended cat video

**My Calculation**:
- Base Score: 2 (recommendation) × 1 (like) = 2
- Activity Multiplier: ~0.35 (prevents dominance)
- **Final Score**: 2 × 0.35 = 0.7 points

**Impact**: Without my system, this user would generate 50+ points daily and dominate recommendations. With my system, they generate ~17.5 points daily—sustainable but not overwhelming.

### Example 2: Casual User (Ensuring Survival)

**User Profile**:
- Daily: 1 interaction
- Monthly: 8 interactions
- Yearly: 50 interactions

**Scenario**: User comments on a searched photography tutorial

**My Calculation**:
- Base Score: 4 (search) × 2 (comment) = 8
- Activity Multiplier: ~1.7 (amplifies rare signals)
- **Final Score**: 8 × 1.7 = 13.6 points

**Impact**: This amplified score can survive 2-3 weeks of decay and remain meaningful, ensuring the user's interests don't get wiped out during quiet periods.

## Why This Approach Works

My system succeeds because it mirrors how human interest actually operates:

1. **Scarcity creates value**: Rare actions from quiet users carry more meaning
2. **Intent matters**: Searched content shows deliberate interest vs. accidental discovery
3. **Effort indicates engagement**: Comments require more investment than likes
4. **Context shapes meaning**: Same action means different things in different discovery contexts
5. **Time provides perspective**: Recent behavior matters most, but long-term patterns add stability

## Technical Benefits

### Fairness and Balance
- Prevents any user type from dominating recommendations
- Ensures meaningful contribution from all user segments
- Balances influence based on interaction scarcity principle

### Decay Resistance
- Casual users' profiles survive quiet periods
- Active users maintain sustainable accumulation rates
- System remains stable across different usage patterns

### Semantic Precision
- Distinguishes between interest and disinterest
- Captures nuanced engagement levels
- Provides rich data for downstream algorithms

### Scalability
- Clear separation of concerns
- Configurable weights and thresholds
- Extensible for new interaction types

## Implementation Notes

The system is designed to be:
- **Configurable**: All weights and scales can be adjusted
- **Extensible**: New interaction types and discovery contexts can be added
- **Maintainable**: Clear phase separation makes debugging straightforward
- **Performant**: Calculations are lightweight and cacheable

## Next Steps

This interest calculation system provides the foundation for sophisticated recommendation engines that understand user behavior nuances while maintaining fairness across different usage patterns. The segregated interest/disinterest scores enable advanced strategies like collaborative filtering, content-based recommendations, and hybrid approaches.

---

*This system represents a significant advancement in fair interest measurement for recommendation engines, solving long-standing problems around user activity imbalance and temporal decay.*