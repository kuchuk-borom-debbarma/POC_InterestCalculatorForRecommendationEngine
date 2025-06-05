# User Topic Interest Scoring Algorithm

## Overview
A comprehensive scoring algorithm for calculating user interest levels in specific topics based on content discovery methods, interaction types, user activity patterns, and past interaction frequency. Designed to feed recommendation engines with accurate, nuanced interest signals that adapt to user behavior patterns.

## Core Components

### 1. Base Score - Content Discovery Method

**Purpose:** Different discovery methods indicate varying levels of user intent and genuine interest.

**Formula:**
```
baseScore = discoveryMethodWeight × 10
```

#### Discovery Method Classifications

**Direct Search (Weight: 1.0)**
- **Why this weight:** User actively sought this content, representing the strongest intent signal
- **Scenario:** User searches for "machine learning tutorials" - they have explicit interest
- **Logic:** This forms our baseline (maximum weight) because deliberate search behavior is the clearest indicator of genuine topic interest

**Recommendation Click (Weight: 0.8)**
- **Why this weight:** User chose recommended content but with algorithmic assistance
- **Scenario:** User clicks on "Recommended for you: Advanced Python Techniques"
- **Logic:** Shows interest but influenced by system suggestions, slightly lower confidence than pure search intent

**Social Media/External Link (Weight: 0.6)**
- **Why this weight:** Interest may be influenced by social proof rather than personal preference
- **Scenario:** User clicks article shared by friend on Twitter
- **Logic:** Could represent trending topics or peer influence rather than intrinsic interest, moderate confidence

**Browse/Explore (Weight: 0.4)**
- **Why this weight:** Serendipitous discovery with minimal commitment
- **Scenario:** User browsing homepage, clicks on trending article
- **Logic:** Exploratory behavior indicates openness but not strong intent, useful for interest expansion

**Notification/Push (Weight: 0.2)**
- **Why this weight:** System-initiated interaction with minimal user effort
- **Scenario:** User clicks push notification about daily tech news
- **Logic:** May indicate convenience rather than strong interest, lowest confidence signal

---

### 2. Interaction Type Multiplier

**Purpose:** Different interaction types reveal the user's sentiment and engagement level with the content.

**Formula:**
```
interactionScore = baseScore × interactionTypeMultiplier
```

#### Passive Interactions

**View/Impression (Multiplier: 1.0)**
- **Why this multiplier:** Baseline interaction requiring minimal effort
- **Scenario:** User opens article, spends normal reading time
- **Logic:** Standard engagement level, forms foundation for other interaction comparisons

**Extended Dwell Time (Multiplier: 1.5)**
- **Why this multiplier:** Indicates content resonance beyond casual browsing
- **Scenario:** User spends 8 minutes on 3-minute average article
- **Logic:** Time investment suggests genuine interest and content value

#### Engagement Interactions

**Like/Reaction (Multiplier: 2.0)**
- **Why this multiplier:** Deliberate positive signal with low friction
- **Scenario:** User clicks heart/thumbs up on tutorial video
- **Logic:** Active approval indicates content satisfaction, reliable positive signal

**Save/Bookmark (Multiplier: 2.5)**
- **Why this multiplier:** Future intent indicates perceived ongoing value
- **Scenario:** User saves "Complete Guide to React Hooks" for later reference
- **Logic:** Planning to return shows higher commitment than momentary approval

**Comment/Reply (Multiplier: 3.0)**
- **Why this multiplier:** High investment requiring thought and time
- **Scenario:** User writes detailed comment explaining their experience with the topic
- **Logic:** Cognitive engagement and community participation indicate strong interest

#### High-Commitment Interactions

**Share/Forward (Multiplier: 3.5)**
- **Why this multiplier:** User stakes their reputation on content quality
- **Scenario:** User shares programming article with their LinkedIn network
- **Logic:** Public endorsement combines personal interest with social validation

**Download/Subscribe (Multiplier: 4.0)**
- **Why this multiplier:** Seeking permanent access or ongoing updates
- **Scenario:** User downloads course materials or subscribes to newsletter
- **Logic:** Ownership intent and long-term engagement commitment

**Create Related Content (Multiplier: 5.0)**
- **Why this multiplier:** Highest level of topic engagement and expertise demonstration
- **Scenario:** User writes blog post inspired by machine learning article
- **Logic:** Content creation indicates deep understanding, passion, or teaching intent

---

### 3. User Activity Scaling

**Purpose:** Compensate for varying user activity levels to ensure fair interest representation across all user types.

**Formula:**
```
activityMultiplier = (dailyFactor × 0.4) + (weeklyFactor × 0.3) + (monthlyFactor × 0.2) + (yearlyFactor × 0.1)
activityScore = interactionScore × activityMultiplier
```

**Why Inverse Scaling:** Low-activity users receive higher multipliers because:
- Each interaction represents stronger intent when interactions are rare
- Prevents interest decay for users who engage infrequently
- Maintains recommendation quality across different usage patterns
- Provides cold-start protection for returning users

**Note:** This scaling can be skipped if the separate score decay system adjusts decay rates based on user activity levels.

#### Activity Level Factors

**Super Low Activity (Multiplier: 2.5)**
- **Daily:** 0-1 interactions, **Weekly:** 0-3, **Monthly:** 0-10, **Yearly:** 0-50
- **Why:** Maximum amplification for users who rarely interact
- **Scenario:** User who checks app once per week but saves important articles

**Low Activity (Multiplier: 2.0)**
- **Daily:** 2-3 interactions, **Weekly:** 4-8, **Monthly:** 11-25, **Yearly:** 51-120
- **Why:** High amplification for occasional users
- **Scenario:** Weekend reader who engages with 2-3 articles per session

**Moderate Activity (Multiplier: 1.5)**
- **Daily:** 4-8 interactions, **Weekly:** 9-20, **Monthly:** 26-60, **Yearly:** 121-300
- **Why:** Slight amplification for regular but not heavy users
- **Scenario:** Daily commuter who reads articles during transport

**High Activity (Multiplier: 1.2)**
- **Daily:** 9-15 interactions, **Weekly:** 21-40, **Monthly:** 61-120, **Yearly:** 301-600
- **Why:** Minimal amplification for active users
- **Scenario:** Professional staying updated with industry content

**Very High Activity (Multiplier: 1.0)**
- **Daily:** 16+ interactions, **Weekly:** 41+, **Monthly:** 121+, **Yearly:** 601+
- **Why:** No amplification needed due to abundant interaction data
- **Scenario:** Power user or content curator with constant engagement

**Note:** These activity classifications are standardized with the decay system to ensure consistent user profiling across both algorithms.

---

### 4. Past Interaction Frequency Multiplier

**Purpose:** Reward topics with consistent user interaction history across different time periods.

**Formula:**
```
frequencyMultiplier = (dailyFreq × 0.4) + (weeklyFreq × 0.3) + (monthlyFreq × 0.2) + (yearlyFreq × 0.1)
frequencyScore = activityScore × frequencyMultiplier
```

**Why Multiple Timeframes:** Different patterns reveal different interest types:
- **Daily frequency:** Current active interests
- **Weekly frequency:** Regular recurring interests
- **Monthly frequency:** Sustained medium-term interests
- **Yearly frequency:** Long-term persistent interests

#### Frequency Multiplier Values

**No History (Multiplier: 1.0)**
- **Scenario:** First interaction with topic
- **Logic:** Baseline scoring for new interests

**Low Frequency (Multiplier: 1.2)**
- **Daily:** 2-3 topic interactions, **Weekly:** 2-4, **Monthly:** 2-6, **Yearly:** 8-20
- **Scenario:** Occasional interest in photography topics
- **Logic:** Some historical engagement indicates mild sustained interest

**Moderate Frequency (Multiplier: 1.5)**
- **Daily:** 4-6 topic interactions, **Weekly:** 5-10, **Monthly:** 7-15, **Yearly:** 21-50
- **Scenario:** Regular engagement with cooking content
- **Logic:** Consistent interaction pattern shows reliable interest

**High Frequency (Multiplier: 2.0)**
- **Daily:** 7-10 topic interactions, **Weekly:** 11-20, **Monthly:** 16-30, **Yearly:** 51-100
- **Scenario:** Frequent interaction with programming tutorials
- **Logic:** Strong historical pattern indicates core interest area

**Very High Frequency (Multiplier: 2.5)**
- **Daily:** 11+ topic interactions, **Weekly:** 21+, **Monthly:** 31+, **Yearly:** 101+
- **Scenario:** Professional developer constantly engaging with tech content
- **Logic:** Topic is central to user's interests, maximum historical boost

---

### 5. Saturation Function

**Purpose:** Implement diminishing returns as scores approach the maximum cap, enabling easier interest shifts for users.

**Formula:**
```
saturatedScore = maxScore × (1 - e^(-frequencyScore / saturationConstant))
where saturationConstant = maxScore / 3 ≈ 33.33
```

**Why Saturation:**
- **High scores:** Harder to increase further (diminishing returns)
- **Low scores:** Increase rapidly with new interactions
- **Interest shifts:** Users can develop new interests more easily
- **Score distribution:** Prevents extreme concentration in few topics

**Saturation Behavior:**
- **Score 0-30:** Nearly linear growth (easy to build new interests)
- **Score 30-60:** Moderate growth curve (established interests)
- **Score 60-85:** Slow growth (strong established interests)
- **Score 85-100:** Very slow growth (dominant interests)

**Example Scenarios:**
- **New Interest (score 5):** Next interaction easily raises to 15-20
- **Moderate Interest (score 40):** Next interaction raises to 45-50
- **Strong Interest (score 80):** Next interaction raises to 82-83
- **Dominant Interest (score 95):** Next interaction raises to 95.5-96

---

## Final Scoring Formula

```
finalScore = saturate(frequencyScore)
where:
baseScore = discoveryMethodWeight × 10
interactionScore = baseScore × interactionTypeMultiplier  
activityScore = interactionScore × activityMultiplier
frequencyScore = activityScore × frequencyMultiplier
saturatedScore = 100 × (1 - e^(-frequencyScore / 33.33))
```

**Score Range:** 0-100 (capped with saturation function)

## Example Calculations

### Scenario 1: New User Direct Search
- **Discovery:** Direct Search (1.0)
- **Interaction:** View (1.0)
- **Activity:** Super Low (2.5)
- **Frequency:** No History (1.0)

```
baseScore = 1.0 × 10 = 10
interactionScore = 10 × 1.0 = 10
activityScore = 10 × 2.5 = 25
frequencyScore = 25 × 1.0 = 25
finalScore = 100 × (1 - e^(-25/33.33)) ≈ 53
```

### Scenario 2: Active User Creating Content
- **Discovery:** Recommendation (0.8)
- **Interaction:** Create Content (5.0)
- **Activity:** High (1.2)
- **Frequency:** Very High (2.5)

```
baseScore = 0.8 × 10 = 8
interactionScore = 8 × 5.0 = 40
activityScore = 40 × 1.2 = 48  
frequencyScore = 48 × 2.5 = 120
finalScore = 100 × (1 - e^(-120/33.33)) ≈ 97
```

### Scenario 3: Casual User Social Share
- **Discovery:** Social Media (0.6)
- **Interaction:** Share (3.5)
- **Activity:** Moderate (1.5)
- **Frequency:** Low (1.2)

```
baseScore = 0.6 × 10 = 6
interactionScore = 6 × 3.5 = 21
activityScore = 21 × 1.5 = 31.5
frequencyScore = 31.5 × 1.2 = 37.8
finalScore = 100 × (1 - e^(-37.8/33.33)) ≈ 68
```

## Expected Behaviors

**New Users:** High initial scores due to activity scaling, enabling quick interest profile building

**Returning Users:** Frequency multipliers preserve historical interests while allowing new interest development

**Power Users:** Consistent scoring without artificial amplification, relying on interaction frequency and type

**Interest Evolution:** Saturation function enables users to develop new interests without being locked into historical patterns