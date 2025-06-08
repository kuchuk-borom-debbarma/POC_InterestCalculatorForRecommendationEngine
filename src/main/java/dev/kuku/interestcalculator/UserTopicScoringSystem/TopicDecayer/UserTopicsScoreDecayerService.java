package dev.kuku.interestcalculator.UserTopicScoringSystem.TopicDecayer;

import dev.kuku.interestcalculator.fakeDatabase.UserTopicScoreDb;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserTopicsScoreDecayerService {
    private final UserTopicScoreDb userTopicScoreDb;

    public void decayScore(String userId) {
        Map<String, Double> topicScores = userTopicScoreDb.getUserTopicScores(userId).stream()
                .collect(Collectors.toMap(r -> r.topic, r -> r.interestScore));

    }
}
