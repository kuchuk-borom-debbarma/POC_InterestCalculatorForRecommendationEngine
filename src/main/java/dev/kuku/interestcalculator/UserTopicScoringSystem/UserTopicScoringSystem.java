package dev.kuku.interestcalculator.UserTopicScoringSystem;

import dev.kuku.interestcalculator.UserTopicScoringSystem.TopicDecayer.UserTopicsScoreDecayer;
import dev.kuku.interestcalculator.UserTopicScoringSystem.TopicScorer.UserTopicInteractionScorer;
import dev.kuku.interestcalculator.fakeDatabase.UserInteractionsDb;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Decays existing topic scores and calculates new ones.
 */
@Service
@RequiredArgsConstructor
public class UserTopicScoringSystem {
    private final UserTopicInteractionScorer userTopicInteractionScorer;
    private final UserTopicsScoreDecayer userTopicsScoreDecayer;

    public void updateUserTopicScores(String userId, UserInteractionsDb.UserInteractionRow interaction) {
        userTopicsScoreDecayer.decayScore(userId);
        userTopicInteractionScorer.scoreInteraction(userId, interaction);
    }
}
