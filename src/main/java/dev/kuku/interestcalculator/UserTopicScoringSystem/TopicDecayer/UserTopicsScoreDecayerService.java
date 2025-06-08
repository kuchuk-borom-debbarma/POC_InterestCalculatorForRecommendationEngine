package dev.kuku.interestcalculator.UserTopicScoringSystem.TopicDecayer;

import dev.kuku.interestcalculator.UserTopicScoringSystem.TopicDecayer.subSystem.TopicScoreTemporalExponentialDecayer;
import dev.kuku.interestcalculator.fakeDatabase.UserTopicScoreDb;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserTopicsScoreDecayerService {
    private final UserTopicScoreDb userTopicScoreDb;
    private final TopicScoreTemporalExponentialDecayer exponentialDecayer;

    public void decayScore(String userId) {
        userTopicScoreDb.getUserTopicScores(userId).forEach(exponentialDecayer::decay);

    }
}
