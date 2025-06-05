package dev.kuku.interestcalculator.services;

import dev.kuku.interestcalculator.fakeDatabase.UserTopicScoreDb;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TemporalUserTopicScoreDecay {
    private final UserTopicScoreDb userTopicScoreDb;
    private final UserActivityCalculator userActivityCalculator;

    public void decayPerUser(String userId) {
        List<UserTopicScoreDb.UserTopicScoreRow> topicScoreRows = userTopicScoreDb.getTopicsOfUser(userId);
        long currentTime = Instant.now().toEpochMilli();
        UserActivityCalculator.ActivityMetrics dailyActivity = userActivityCalculator.getDailyActivityFromNow(userId);
        UserActivityCalculator.ActivityMetrics monthlyActivity = userActivityCalculator.getMonthlyActivityFromNow(userId);
        UserActivityCalculator.ActivityMetrics yearlyActivity = userActivityCalculator.getYearlyActivityFromNow(userId);
        for (var topicScore : topicScoreRows) {
            long timeDelta = currentTime - topicScore.updatedAt;
            //base decay based on timeDelta
            double decay = calculateTemporalDecay(timeDelta);
        }
    }

    private double calculateTemporalDecay(long timeDelta) {
        return 0.99;
    }
}
