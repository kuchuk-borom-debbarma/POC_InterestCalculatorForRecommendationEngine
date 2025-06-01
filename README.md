# User Interest Score Calculator
*Proof of Concept for Dynamic Content Personalization*

## Overview

The User Interest Score Calculator is a sophisticated algorithm designed for social media platforms to personalize content delivery based on user behavior patterns. By analyzing interaction history and applying complex weighting mechanisms, it creates dynamic interest profiles that evolve with user preferences over time.

## (IGNORE FOR NOW) Logic for determining which user to update the interest of
In the pipeline we have an event Queue in which the interactions record get stored and popped later in batch for calculating interest of the users. We only want to process users based on activity this will help us avoid calculation topic interest for users that are not active

First solution is to use a partitioned database to store high , mid, low activity users
Then users with same id can be fetched and processed by first selecting the FIRSt user we find and then using it's ID to fetch the other entries that it may have.

## Core Algorithm: Calculate User Interest Score
NOTE : The numbers are placeholders and are not realistic.
### 🎯 Key Components

#### 1. **Interaction Weight System**
*Different actions carry different significance levels*

Each user interaction is assigned a weight based on its engagement depth:

- **Comments & Replies**: `High weight (3.0-5.0)`
  - Indicates strong engagement and interest
  - Requires time investment and active participation
- **Shares & Reposts**: `Medium-High weight (2.5-3.5)`
  - Shows content resonates enough to share with others
  - Implies endorsement and topic relevance
- **Likes & Reactions**: `Medium weight (1.0-2.0)`
  - Quick engagement indicator
  - Most common but least committal interaction
- **Views & Clicks**: `Low weight (0.1-0.5)`
  - Passive consumption metric
  - Helps identify browsing patterns

**Example**: A user commenting on a photography post receives 4.0 points, while simply viewing it adds only 0.2 points to their photography interest score.

#### 2. **User Activity Scaling**
*Prevents score inflation for heavy users while accelerating discovery for casual users*

The system dynamically adjusts impact based on overall user activity:

- **Heavy Users** (>100 interactions/week): `Reduced multiplier (0.7-0.9x)`
  - Prevents artificial score inflation
  - Maintains balanced interest distribution
- **Regular Users** (20–100 interactions/week): `Standard multiplier (1.0x)`
  - Baseline scoring mechanism
- **Casual Users** (<20 interactions/week): `Boosted multiplier (1.2-1.5x)`
  - Speeds up interest pattern detection
  - Ensures responsive personalization

**Example**: When a casual user likes a cooking video, their cooking interest increases by `1.5 × 1.5 = 2.25 points`, while a heavy user's same action adds only `1.5 × 0.8 = 1.2 points`.

#### 3. **Temporal Decay Mechanism**
*Reduces the influence of outdated interactions while preserving long-term preferences*

Interest scores naturally decay over time to reflect changing preferences:

- **Recent interactions** (0–7 days): `No decay (1.0x)`
- **Moderate age** (1–4 weeks): `Slight decay (0.8-0.9x)`
- **Older interactions** (1–6 months): `Significant decay (0.3-0.7x)`
- **Historical data** (6+ months): `Minimal impact (0.1-0.2x)`

**Important**: Decay is applied only during interest score calculation, **not** during feed generation. This ensures returning users see familiar content types initially, then adapt based on new interactions. User activity rate is taken into account during calculation to reduce decay for users who do not regularly engage with content.

**Example**: A user who heavily engaged with fitness content 3 months ago will see some fitness posts upon return, but if they don't interact with them, those topics will quickly fade from their feed as decay reduces their historical fitness score.

#### 4. **Cross-Topic Influence Network**
*Leverages topic relationships to enhance discovery and relevance through dynamic co-occurrence analysis*

Topics are interconnected through weighted relationships that create cascading score effects. The system maintains a global topic relationship database that continuously evolves based on real-world content patterns.

**Benefits:**
- **Content Discovery**: Users discover related topics they haven't directly engaged with
- **Trend Awareness**: Algorithm adapts to emerging topic relationships in real-time
- **Reduced Cold Start**: New or niche topics gain visibility through established relationships
- **Natural Evolution**: Topic network evolves organically with user behavior patterns


Design idea. This one is a heavy task so it needs to be performed separately. It can make use of a queue from which it processing periodically. The main function to calculate user interest score will push data in this queue.
The function for this operation will determine the topic's relationship and batch in its current batch and then increment them to existing scores. Then we take the highest and lowest score in the table to normalize the score in specific range.
We first decay the scores based on time though and then add the score and stuff.

There will be a range, if it exceeds that range it is going viral

##### **Storage and Performance**

**Database Structure:**
- Topic relationships stored in relational database (vector database planned for future)
- Daily batch updates prevent real-time calculation overhead
- Automatic cleanup of weak relationships maintains performance

**Scalability Considerations:**
- Batch processing limits computational load
- Sliding window prevents indefinite data growth
- Threshold filtering reduces storage requirements

#### 5. **Topic Saturation Control**
*Prevents interest tunnel vision and encourages content diversity*

As users repeatedly engage with similar topics, the rate of score increase diminishes:

**Saturation Levels**:
- **Fresh Interest** (0–50 interactions): `Full impact (1.0x)`
- **Developing Interest** (51–150 interactions): `Reduced impact (0.8x)`
- **Established Interest** (151–500 interactions): `Diminished impact (0.6x)`
- **Saturated Interest** (500+ interactions): `Minimal impact (0.3x)`

**Benefits**:
- Prevents algorithmic "rabbit holes"
- Encourages exploration of new topics
- Maintains content diversity in user feeds
- Facilitates natural interest evolution

**Example**: A user who has interacted with 200 cooking posts will see reduced cooking score increases (0.6x) for new cooking interactions, making it easier for other interests (like travel or music) to compete for feed prominence.

