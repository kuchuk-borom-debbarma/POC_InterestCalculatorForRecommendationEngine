package dev.kuku.interestcalculator.secondIteration.services.userTopicScoreDecayer;

import dev.kuku.interestcalculator.secondIteration.fakeDatabase.UserInteractionsDb;
import dev.kuku.interestcalculator.secondIteration.fakeDatabase.UserTopicScoreDb;
import dev.kuku.interestcalculator.secondIteration.models.TopicScoreTuple;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Decay topics score over time based on topic's updatedAt and current time
 * Inverse logarithmic decay function. Decays slow for fresh content, as they their time delta increases decays faster.
 * <p>
 * grace period based on updatedAt and user activity scaling for that time frame to not decay new topics
 * <p>
 * We do not want FRESH user which means all its topic score has reached 0 because user was inactive for longtime
 * Solutions
 * 1. decay rate based on user acitvity for short and long time frame such as weekly active, monthly active, quater-monthly active
 * by determining actiity rate for week, month, etc we can decide if the user is weekly, monthly or etc type of user
 * 2. min floor such as "keep 0.8% of original score". This preserves relativity too
 * 3. Pause decay after if time delta is huge
 * <p>
 * Do we do it periodically?
 * yes
 */
@Service
@RequiredArgsConstructor
public class RatioPreservingUserTopicScoreDecayer {
    private final UserTopicScoreDb userTopicScoreDb;

    public void execute(UserInteractionsDb.UserInteractionRow userInteraction, Map<String, TopicScoreTuple> currentTopicScores) {
        List<UserTopicScoreDb.UserTopicScoreRow> userTopicScoreRows = userTopicScoreDb.getTopicsOfUser(userInteraction.userId);
        for (var userTopicScore : userTopicScoreRows) {
            var originalInterest = userTopicScore.interestScore;
            var originalDisinterest = userTopicScore.disinterestScore;
        }
    }

}