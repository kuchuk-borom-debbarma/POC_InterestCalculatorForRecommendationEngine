package dev.kuku.interestcalculator.UserTopicScoringSystem.TopicDecayer.subSystem;

import dev.kuku.interestcalculator.fakeDatabase.UserTopicScoreDb;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class TopicScoreTemporalExponentialDecayer {
    public void decay(UserTopicScoreDb.UserTopicScoreRow userTopicScore) {

    }
}
