package dev.kuku.interestcalculator.UserTopicScoringSystem.TopicDecayer.subSystem;

import dev.kuku.interestcalculator.fakeDatabase.UserTopicScoreDb;
import dev.kuku.interestcalculator.util.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class TopicScoreTemporalExponentialDecayer {
    private static final double DECAY_RATE = 0.7;
    /**
     * Should return delta
     */
    private final TimeProvider timeProvider;

    public double decay(UserTopicScoreDb.UserTopicScoreRow userTopicScore) {
        long currentTime = timeProvider.nowMillis();
        long topicUpdateTime = userTopicScore.updatedAt;
        if (currentTime < topicUpdateTime) throw new IllegalArgumentException("currentTime < topicUpdateTime");
        long timeElapsed = currentTime - topicUpdateTime;
        // 1 minute in milliseconds
        double TIME_UNIT_MILLIS = 60000;
        double timeUnit = (double) timeElapsed / TIME_UNIT_MILLIS;

        double decayFactor = Math.pow(DECAY_RATE, timeUnit);
        double newScore = userTopicScore.interestScore * decayFactor;
        log.info("Decayed score for {} from {} to {}", userTopicScore.userId, userTopicScore.interestScore, newScore);
        return newScore - userTopicScore.interestScore;
    }
}
