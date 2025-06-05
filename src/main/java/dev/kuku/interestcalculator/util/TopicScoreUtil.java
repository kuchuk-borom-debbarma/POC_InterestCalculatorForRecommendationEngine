package dev.kuku.interestcalculator.util;


import dev.kuku.interestcalculator.models.TopicScoreTuple;

import java.util.Map;

public class TopicScoreUtil {
    /**
     * Sums the interest and disinterest score to all topic.
     */
    public static void addToAllExistingTopicScore(Map<String, TopicScoreTuple> topicScoreMap, double interestScore, double disinterestScore) {
        for (Map.Entry<String, TopicScoreTuple> entry : topicScoreMap.entrySet()) {
            TopicScoreTuple topicScoreTuple = entry.getValue();
            topicScoreTuple.interestScore += interestScore;
            topicScoreTuple.disinterestScore += disinterestScore;
        }
    }

}
