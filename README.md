# User Scoring System

## Overview

This system calculates and maintains user interest scores across different topics to personalize content recommendations. It consists of four main components that work together to track user preferences, decay outdated interests, and maintain topic relationships.

## Components

### 1. User Activity Classification

A dynamic classification system that categorizes users based on their interaction frequency within specified time periods.

#### Classification Algorithm
**Input:** Time span (from-to date range)  
**Process:** Analyze user interaction/activity data within the specified period  
**Output:** Activity level classification

#### Activity Levels
- **Very Active**: High frequency interactions
- **Active**: Regular interaction patterns
- **Moderately Active**: Occasional interactions
- **Low Active**: Infrequent interactions
- **Inactive**: Minimal to no interactions

#### Time Period Classifications
The system can classify activity across multiple time spans:
- **Daily Activity**: Last 24 hours interaction pattern
- **Weekly Activity**: Last 7 days interaction pattern
- **Monthly Activity**: Last 30 days interaction pattern
- **Custom Period**: Any specified date range

**Purpose:**
- Scale topic scores inversely to activity level (less active = higher score multiplier)
- Adjust decay rates based on user engagement patterns
- Prioritize processing resources for different user segments

### 2. User Score Matrix

Core algorithm for calculating and maintaining user topic interest scores with separate positive and negative sentiment tracking.
suggestion :- how popular the user is on the platform, drop user activity scaling if its complex at start

#### Process Flow
1. **Calculate Dynamic Base Score**: Generate score using content discovery, interaction type, and activity scaling
2. **Sentiment Classification**: Determine if interaction indicates interest or disinterest
3. **Score Storage**: Update separate interest (0-10) and disinterest (0-10) scores
4. **Queue Processing**: Send topic set to `topicRelationship queue` for relationship analysis

#### Dynamic Base Score Calculation

**Formula:**
```
Base Score = Content Discovery Weight × Interaction Type Weight × User Activity Scale
```

**Content Discovery Methods:**
- **Search**: User actively searched for content (Weight: High)
- **Recommendation**: System-suggested content (Weight: Medium)
- **Trending**: Popular/viral content discovery (Weight: Low)
- **Direct**: Shared/linked content (Weight: Medium-High)

**Interaction Types & Weights:**
- **Positive Interactions** (Interest):
    - View/Read: 1.0
    - Like/Upvote: 2.0
    - Share: 3.0
    - Comment (positive): 2.5
    - Save/Bookmark: 3.5

- **Negative Interactions** (Disinterest):
    - Skip/Dismiss: 1.0
    - Dislike/Downvote: 2.0
    - Report: 4.0
    - Block/Hide: 3.0
    - Comment (negative): 2.0

#### User Activity Scaling Strategy

**Inverse Activity Scaling** - Less active users receive higher score multipliers to compensate for infrequent interactions.

**Proposed Scaling Formula:**
```
Activity Scale = Base Multiplier / (Activity Level + Smoothing Factor)

Where:
- Base Multiplier: 10 (adjustable)
- Smoothing Factor: 1 (prevents division by zero)
- Activity Level: Normalized activity count (1-10 scale)
```

**Example Scaling:**
- **Very Active** (Level 10): Scale = 10/(10+1) = 0.91
- **Active** (Level 7): Scale = 10/(7+1) = 1.25
- **Moderately Active** (Level 4): Scale = 10/(4+1) = 2.0
- **Low Active** (Level 2): Scale = 10/(2+1) = 3.33
- **Inactive** (Level 1): Scale = 10/(1+1) = 5.0

**Benefits:**
- Casual users' scores decay slower due to higher initial scores
- Prevents casual users from losing all topic relevance during inactive periods
- Maintains recommendation quality across different user types

#### Dual Score Philosophy
- **Separate Storage**: Interest (0-10) and Disinterest (0-10) in different columns
- **Advantages over Combined Scoring (-10 to +10):**
    - More intuitive for algorithmic processing
    - Clearer sentiment analysis and debugging
    - Prevents negative score complications in recommendation algorithms
    - Allows independent decay rates for positive/negative sentiments

#### Scheduled Operations (CRON Jobs)
- **User Topic Score Decay**: Reduces scores over time to reflect changing interests
- **Topic Relationship Decay**: Weakens topic connections to reflect trend changes

### 3. User Topic Score Decay

Gradually reduces topic scores over time to account for changing user interests while protecting casual users from losing established preferences.

#### Multi-Tiered Grace Period System

**Grace Period Types:**
- **Recent Interaction Grace**: 7-14 days for recently interacted topics
- **High Interest Grace**: 30-60 days for topics with scores >7
- **User Activity Grace**: Extended periods for low-activity users

**Implementation:**
```
Grace Period = Base Period × Topic Score Multiplier × User Activity Modifier

Where:
- Base Period: 7 days (minimum)
- Topic Score Multiplier: (Topic Score / 10) × 2
- User Activity Modifier: Inverse of activity level (1-5x)
```

#### Dynamic Decay Rates

**Decay Rate Calculation:**
```
Decay Rate = Base Decay Rate × Activity Modifier × Engagement Recency

Activity Modifiers:
- Very Active: 1.5x (faster adaptation to changing interests)
- Active: 1.2x  
- Moderately Active: 1.0x (baseline)
- Low Active: 0.5x (preserve established interests)
- Inactive: 0.1x (minimal decay to preserve profile)
```

**Daily Decay Examples:**
- **Very Active User**: 3% daily decay (rapid interest change detection)
- **Active User**: 2.4% daily decay
- **Moderate User**: 2% daily decay (baseline)
- **Low Active User**: 1% daily decay (preserve interests longer)
- **Inactive User**: 0.2% daily decay (maintain historical profile)

#### Challenge Resolution: Fake New User Issue
**Problem:** Long-inactive users return with zero topic scores, losing all historical interest data.

**Solution:** Multi-layered protection system combining grace periods with minimal decay rates ensures topic relevance preservation during extended inactive periods.

### 4. Topic Relationship Graph

Maintains weighted connections between topics based on co-occurrence frequency in user interactions.

#### Topic Relationship Queue

**Queue Structure:**
- **Input**: Sets of topics from user interactions
- **Processing**: Asynchronous batch processing of topic combinations
- **Consumer**: Topic Relationship Calculator service
- **Output**: Updated topic connection weights in relationship graph

**Queue Benefits:**
- Decouples real-time user interactions from relationship calculations
- Enables batch processing for efficiency
- Handles high-volume topic updates without blocking user experience

#### Weight Calculation with Growth Limiting

**Logarithmic Scaling Implementation:**
```java
private long calculateClampedWeight(long currentWeight, long increment) {
    // Soft cap using logarithmic scaling
    double scalingFactor = 1.0 / (1.0 + Math.log10(currentWeight + 1));
    long scaledIncrement = Math.round(increment * scalingFactor);
    
    // Hard cap enforcement
    long newWeight = currentWeight + scaledIncrement;
    return Math.min(newWeight, HARD_CAP_LIMIT); // e.g., 10000
}
```

**Growth Limitation Strategy:**
- **Soft Cap**: Logarithmic scaling reduces increment effectiveness as weight increases
- **Hard Cap**: Absolute maximum weight limit prevents any single relationship from dominating
- **Natural Balance**: Ensures diverse topic representation in recommendation algorithms

#### Processing Workflow
1. **Batch Collection**: Gather topic sets from interaction queue
2. **Pair Generation**: Create all possible topic pair combinations
3. **Weight Updates**: Apply logarithmic scaling to existing relationships
4. **New Relationships**: Initialize new topic pairs with base weight
5. **Graph Optimization**: Periodic cleanup of low-weight relationships

### 5. Topic Relationship Decay

Implements dynamic decay rates based on relationship volatility and engagement patterns to reflect real-world trend changes.

#### Velocity-Based Decay Philosophy

The system analyzes topic relationship patterns to determine appropriate decay rates, ensuring that ephemeral trends fade quickly while stable relationships persist.

**Real-World Application:**
- **Past Trend**: `Fortnite → Gaming` (historically strong, now declining)
- **Emerging Trend**: `Gaming → GTA` (growing strength)
- **Stable Relationship**: `Programming → JavaScript` (consistent over time)

#### Advanced Decay Method

**1. Pattern Analysis (Rolling Window)**
- **Time Windows**: 7-day buckets over 4-week periods
- **Tracking Metrics**: Co-occurrence frequency, interaction volume, user diversity
- **Volatility Calculation**: Standard deviation of weekly activity levels

**2. Relationship Classification**
```
Volatility Score = StandardDeviation(WeeklyActivity) / Average(WeeklyActivity)
Recent Activity Ratio = RecentActivity(7days) / HistoricalActivity(21days)
```

**3. Dynamic Decay Rate Assignment**

| Classification | Volatility | Recent Activity | Daily Decay Rate | Example |
|---|---|---|---|---|
| **Viral Trends** | High (>0.8) | Low (<0.5) | 15% | Viral meme topics |
| **Growing Trends** | Low (<0.3) | High (>1.5) | 1% | Emerging technologies |
| **Stable Relationships** | Low (<0.3) | Normal (0.8-1.2) | 2% | Core domain connections |
| **Declining Trends** | Medium (0.3-0.8) | Declining (0.5-0.8) | 8% | Fading interests |
| **Dormant Topics** | Low (<0.3) | Very Low (<0.3) | 5% | Inactive but preserved |

**4. Cleanup Process**
- **Minimum Weight Threshold**: ~0.1 (configurable)
- **Removal Criteria**: Relationships below threshold after decay application
- **Batch Processing**: Periodic cleanup to maintain graph efficiency

#### Decay Rate Justification
- **Viral Trends**: Aggressive decay (15%) enables rapid trend transitions
- **Growing Trends**: Minimal decay (1%) allows organic relationship strengthening
- **Stable Relationships**: Moderate decay (2%) maintains long-term connections
- **Declining Trends**: Accelerated decay (8%) facilitates natural fade-out

## Implementation Recommendations

### User Activity Scaling Refinement
**Suggested Enhancement:**
```
Activity Scale = (Base × Consistency Bonus) / (Activity Level + Smoothing)

Where:
- Consistency Bonus: 1.0-1.5x based on interaction pattern regularity
- Base: 8-12 (tunable based on system performance)
- Smoothing Factor: 0.5-2.0 (prevents extreme scaling)
```

### Grace Period Configuration
**Recommended Starting Values:**
- **Base Grace Period**: 7-14 days
- **High Interest Multiplier**: 2-4x for scores >7
- **Low Activity Multiplier**: 2-10x based on inactivity duration
- **Topic Category Modifiers**: 0.5-3x based on content type (news vs. evergreen)

### Minimum Weight Threshold
**Suggested Implementation:**
- **Primary Threshold**: 0.1 (relationship removal)
- **Warning Threshold**: 0.5 (decay monitoring)
- **Archive Threshold**: 0.05 (historical data retention)

## System Benefits

1. **Adaptive Personalization**: Accounts for different user behavior patterns
2. **Interest Preservation**: Protects casual users from losing established preferences
3. **Trend Responsiveness**: Rapidly adapts to changing topic popularity
4. **Scalable Architecture**: Efficient processing through queue-based design
5. **Data Integrity**: Prevents loss of valuable historical relationship data
6. **Balanced Recommendations**: Maintains diverse topic representation across user types