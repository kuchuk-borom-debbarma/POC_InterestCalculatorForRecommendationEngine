package dev.kuku.interestcalculator.UserTopicScoringSystem.TopicScorer.subSystem;

import org.springframework.stereotype.Component;

@Component
public class InteractionTopicScorer {
    /**
     * Should return delta
     * @param userId
     * @param t
     * @return
     */
    public double scoreTopic(String userId, String t) {
        return 1.0;
    }
}
