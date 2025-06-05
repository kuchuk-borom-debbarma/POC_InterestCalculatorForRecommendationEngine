# User Topic Score Decay Algorithm

## Overview
A time-based decay algorithm for managing user interest scores that maintains temporal relevance while preserving important interests based on recent interaction patterns and user activity levels. Works in conjunction with the scoring algorithm to provide fresh, relevant recommendations.

## Core Components

### 1. Base Decay Calculation

**Purpose:** Natural degradation of interest over time without user interaction.

**Formula:**
```
deltaTime = currentTime (UTC milliseconds) - lastUpdated
baseDecay = decayRate × deltaTime
```

**Why Delta Time Works:**
- **Automatic Scaling:** Longer periods without interaction result in proportionally higher decay
- **Self-Reinforcing:** Topics users care about get refreshed through interaction, while forgotten topics fade naturally
- **Computational Efficiency:** Single timestamp comparison rather than complex time-based rules
- **Natural Pruning:** Very old topics can decay completely, allowing automatic cleanup

**Mathematical Properties:**
- **Linear Decay:** Predictable, consistent behavior
- **Monotonic Increase:** Decay always increases with time, never decreases without interaction
- **Unbounded Growth:** Enables complete topic removal when appropriate

---

### 2. Interaction-Based Multiplier

**Purpose:** Reduce decay rates based on recent user engagement with the topic, focusing on temporal relevance rather than interest strength (which is handled by the scoring system).

**Formula:**
```
interactionMultiplier = weeklyFactor × monthlyFactor
```

**Note:** Frequency multipliers are not applied here since the scoring system already handles interest strength through frequency bonuses. Decay focuses purely on temporal relevance.

#### Weekly Factor (Primary Recent Activity)
**Purpose:** Captures immediate user interest and current topic relevance
**Weight Range:** `0.1 - 1.0` (90% maximum decay reduction)

**Calculation:**
```
weeklyFactor = max(0.1, 1.0 - (weeklyInteractions × averageInteractionWeight))
```

**Why Weekly is Primary:**
- Reflects current user priorities and immediate interests
- Most sensitive to user behavior changes
- Prevents decay of actively engaged topics

#### Monthly Factor (Medium-term Pattern)
**Purpose:** Balances recent spikes with sustained engagement patterns
**Weight Range:** `0.2 - 1.0` (80% maximum decay reduction)

**Calculation:**
```
monthlyFactor = max(0.2, 1.0 - (monthlyInteractions × averageInteractionWeight × 0.7))
```

**Why Monthly Matters:**
- Prevents topics from decaying during brief inactive periods
- Captures cyclical interests (monthly recurring topics)
- Provides stability for work-related or structured learning topics

#### Interaction Type Weights (Aligned with Scoring System)

**Passive Interactions:**
- **View/Impression:** Weight = 0.1
- **Extended Dwell:** Weight = 0.15

**Engagement Interactions:**
- **Like/Reaction:** Weight = 0.2
- **Save/Bookmark:** Weight = 0.25
- **Comment/Reply:** Weight = 0.3

**High-Commitment Interactions:**
- **Share/Forward:** Weight = 0.35
- **Download/Subscribe:** Weight = 0.4
- **Create Related Content:** Weight = 0.5 (capped for decay purposes)

**Why These Weights:** Proportional to scoring system multipliers but capped at 0.5 to prevent complete decay elimination, ensuring temporal relevance is maintained.

---

### 3. User Activity Multiplier

**Purpose:** Adjust decay rates based on overall user activity patterns to ensure fair treatment across different user types.

**Standardized Activity Classifications (Aligned with Scoring System):**

#### Super Low Activity (Multiplier: 0.0 - Frozen Decay)
- **Daily:** 0-1 interactions, **Weekly:** 0-3, **Monthly:** 0-10, **Yearly:** 0-50
- **Why Frozen:** Limited interaction data is extremely valuable; each interaction represents significant intent
- **Scenario:** User who checks app weekly but saves important articles

#### Low Activity (Multiplier: 0.2 - Very Slow Decay)
- **Daily:** 2-3 interactions, **Weekly:** 4-8, **Monthly:** 11-25, **Yearly:** 51-120
- **Why Slow:** Preserve most interest data while allowing gradual pruning of truly irrelevant topics
- **Scenario:** Weekend reader who engages with 2-3 articles per session

#### Moderate Activity (Multiplier: 0.5 - Moderate Decay)
- **Daily:** 4-8 interactions, **Weekly:** 9-20, **Monthly:** 26-60, **Yearly:** 121-300
- **Why Moderate:** Standard decay behavior with protection for established interests
- **Scenario:** Daily commuter who reads articles during transport

#### High Activity (Multiplier: 0.8 - Normal Decay)
- **Daily:** 9-15 interactions, **Weekly:** 21-40, **Monthly:** 61-120, **Yearly:** 301-600
- **Why Normal:** Sufficient data for reliable interest inference with regular pruning
- **Scenario:** Professional staying updated with industry content

#### Very High Activity (Multiplier: 1.0 - Full Decay)
- **Daily:** 16+ interactions, **Weekly:** 41+, **Monthly:** 121+, **Yearly:** 601+
- **Why Full:** Abundant interaction data allows aggressive pruning for fresh recommendations
- **Scenario:** Power user or content curator with constant engagement

---

## Final Decay Formula

```
finalDecay = baseDecay × interactionMultiplier × userActivityMultiplier
newScore = max(0, currentScore - finalDecay)
```

**Formula Breakdown:**
1. **Base Decay:** Time-based natural degradation since last interaction
2. **Interaction Multiplier:** Recent engagement reduces decay (temporal relevance)
3. **User Activity Multiplier:** Adjust for user behavior patterns (fairness across user types)
4. **Score Floor:** Prevents negative scores, enables clean removal at zero

## Integration with Scoring System

**Division of Responsibilities:**
- **Scoring System:** Builds accurate interest signals with frequency bonuses and interaction strength
- **Decay System:** Maintains temporal relevance without duplicating interest strength calculations
- **Combined Effect:** High-scoring topics from frequent interactions naturally resist decay better

**Workflow:**
1. User interacts with content → Scoring system calculates new score
2. Time passes → Decay system reduces score based on time elapsed
3. User interacts again → Scoring system boosts score, decay timer resets
4. Cycle continues with each system handling its core responsibility

## Expected Behaviors

### Scenario Analysis

**New User First Week:**
- All topics start with minimal base decay
- No interaction history = standard decay rates
- First interactions immediately reduce decay through weekly factors

**Casual User (Low Activity):**
- Slower decay preserves interest profile during inactive periods
- Weekly interactions provide strong decay protection
- Monthly patterns maintain medium-term interests

**Power User (Very High Activity):**
- Full decay rates encourage fresh topic discovery
- Recent interactions strongly protect current interests
- Quick adaptation to changing preferences

**Returning Inactive User:**
- Super low activity classification freezes decay
- Preserved interest profile enables quick re-personalization
- Gradual transition back to normal decay as activity resumes

## Example Calculations

### Scenario 1: Casual User Recent Interaction
- **Activity Level:** Low (0.2 multiplier)
- **Weekly Interactions:** 3 with average weight 0.25
- **Monthly Interactions:** 8 with average weight 0.2
- **Time Since Last:** 2 days

```
weeklyFactor = max(0.1, 1.0 - (3 × 0.25)) = max(0.1, 0.25) = 0.25
monthlyFactor = max(0.2, 1.0 - (8 × 0.2 × 0.7)) = max(0.2, 0.88) = 0.88
interactionMultiplier = 0.25 × 0.88 = 0.22
finalDecay = baseDecay × 0.22 × 0.2 = baseDecay × 0.044
```
Very low decay due to recent activity and low user activity level.

### Scenario 2: Power User Old Topic
- **Activity Level:** Very High (1.0 multiplier)
- **Weekly Interactions:** 0
- **Monthly Interactions:** 2 with average weight 0.1
- **Time Since Last:** 3 weeks

```
weeklyFactor = max(0.1, 1.0 - 0) = 1.0
monthlyFactor = max(0.2, 1.0 - (2 × 0.1 × 0.7)) = max(0.2, 0.86) = 0.86
interactionMultiplier = 1.0 × 0.86 = 0.86
finalDecay = baseDecay × 0.86 × 1.0 = baseDecay × 0.86
```
Higher decay due to no recent weekly activity and high user activity level, enabling quick interest shifts.