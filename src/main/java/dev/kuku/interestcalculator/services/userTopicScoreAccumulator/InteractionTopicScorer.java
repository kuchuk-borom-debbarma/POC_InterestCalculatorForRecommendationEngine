package dev.kuku.interestcalculator.services.userTopicScoreAccumulator;

import org.springframework.stereotype.Component;

@Component
public class InteractionTopicScorer {

    public double scoreTopic(String userId, String t) {
        return 1.0;
    }
}
