package dev.kuku.interestcalculator.secondIteration.services.baseScoreCalculator;

import dev.kuku.interestcalculator.secondIteration.fakeDatabase.UserInteractionsDb;
import dev.kuku.interestcalculator.secondIteration.fakeDatabase.UserTopicScoreDb;
import dev.kuku.interestcalculator.secondIteration.models.TopicScoreTuple;
import dev.kuku.interestcalculator.secondIteration.util.TopicScoreUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * SECOND <br>
 * Calculates the base interest score. <br>
 * The base score will be a variable that is determined based on how the content was discovered. <br>
 * The interaction data will need to contain information about how the content that is being interacted with appeared in the user's feed. <br>
 * <p>
 * Example of how base score can be described based on how they were interacted with :- <br>
 * 1. Explicit search :- 5 <br>
 * 2. Trending content :- 3 <br>
 * 3. Recommendation :- 2 <br>
 * <p>
 * We then multiply the base score with a weight that is determined by the type of interaction done. <br>
 * <p>
 * Example :- <br>
 * User is shown a trending offensive meme video. <br>
 * User did not like it and reported it. <br>
 * Since the content was discovered through trending. It's base score is 3. <br>
 * Since the interaction type was "report", the weight to multiple the base score is -2. <br>
 * So we end up with base score of 3 x (-2) = -6 <br>
 * Since it's negative it can be accumulated to the negative score.
 */
@Service
@RequiredArgsConstructor
public class DynamicContentDiscoveryBasedBaseScoreCalculator {
    private final UserTopicScoreDb userTopicScoreDb;

    public void execute(UserInteractionsDb.UserInteractionRow userInteraction, Map<String, TopicScoreTuple> currentTopicScores) {
        double baseScore = getBaseScore(userInteraction.contentDiscovery);
        double interactionWeight = getInteractionWeight(userInteraction.interactionType);
        double finalRawScore = baseScore * interactionWeight;
        double interestScore = 0;
        double disinterestScore = 0;
        if (finalRawScore < 0) {
            disinterestScore = finalRawScore;
        } else {
            interestScore = finalRawScore;
        }
        //Apply base score to current topic
        TopicScoreUtil.addToAllExistingTopicScore(currentTopicScores, interestScore, disinterestScore);
    }

    private double getBaseScore(UserInteractionsDb.Discovery contentDiscovery) {
        return switch (contentDiscovery) {
            case RECOMMENDATION -> 2;
            case TRENDING -> 3;
            case SEARCH -> 4;
        };
    }

    private double getInteractionWeight(UserInteractionsDb.InteractionType interactionType) {
        return switch (interactionType) {
            case LIKE -> 1;
            case DISLIKE -> -1;
            case COMMENT -> 2;
            case REPORT -> -2;
        };
    }
}
