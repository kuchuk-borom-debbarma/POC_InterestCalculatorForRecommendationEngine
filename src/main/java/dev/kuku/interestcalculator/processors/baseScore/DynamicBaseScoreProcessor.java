package dev.kuku.interestcalculator.processors.baseScore;

import dev.kuku.interestcalculator.models.InteractionData;
import dev.kuku.interestcalculator.processors.UserInterestScoreProcessorBase;

import java.util.List;

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
public class DynamicBaseScoreProcessor extends UserInterestScoreProcessorBase {

    @Override
    public void process(String userId, List<InteractionData> userInteractions) {

    }
}
