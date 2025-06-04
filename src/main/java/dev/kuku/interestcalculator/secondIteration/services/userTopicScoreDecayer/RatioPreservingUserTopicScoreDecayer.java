package dev.kuku.interestcalculator.secondIteration.services.userTopicScoreDecayer;

import dev.kuku.interestcalculator.secondIteration.fakeDatabase.UserInteractionsDb;
import dev.kuku.interestcalculator.secondIteration.fakeDatabase.UserTopicScoreDb;
import dev.kuku.interestcalculator.secondIteration.models.TopicScoreTuple;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * What does it do? <br>
 * Applies ratio-preserving decay to all user topic scores based on interaction time gaps,
 * maintaining relative preference hierarchy while reducing absolute values over time.
 * <p>
 * Why do we need it? <br>
 * Traditional decay methods either reset interests to zero (losing user preferences) or
 * apply uniform decay (breaking relative rankings). This approach preserves what users
 * care about most while naturally filtering out weak interests and allowing room for
 * new discoveries when they return after long periods of inactivity.
 * <p>
 * Method: <br>
 * 1. Calculate time elapsed since user's last interaction per topic. cuz every topic will have it's own timestamp<br>
 * 2. Find min/max scores across all user's topics to establish current range <br>
 * 3. Apply proportional decay: normalize each score to 0-1 ratio, apply time-based decay
 * to the maximum, then reconstruct scores maintaining exact ratios <br>
 * 4. Result: lowest topic becomes 0, highest retains meaningful value, hierarchy preserved
 * <p>
 * Formula: <br>
 * {@code new_score = ((original_score - min_score) / (max_score - min_score)) × (max_score × decay_factor)} <br>
 * where {@code decay_factor = 0.98^days_elapsed}
 * <p>
 * Example: <br>
 * User inactive for 60 days with topics [Gaming:9, Tech:6, Music:3, Sports:1] <br>
 * After decay: [Gaming:4.05, Tech:2.7, Music:1.35, Sports:0] <br>
 * Ratios preserved: Gaming still 1.5x Tech, Tech still 2x Music, Sports filtered out
 * <p>
 * Benefits: <br>
 * - Strong preferences survive long inactivity periods <br>
 * - Weak interests get naturally pruned (become 0) <br>
 * - Relative rankings remain intact <br>
 * - Fast rebuilding when user returns (boost existing strong interests) <br>
 * - No computational waste during inactive periods (on-demand calculation)
 * <p>
 * Has to be calculated per interaction
 */
@Service
@RequiredArgsConstructor
public class RatioPreservingUserTopicScoreDecayer {
    private final UserTopicScoreDb userTopicScoreDb;
    private final int TopicWithZeroScoreAllowed = 2;  // Number of topics allowed to reach 0 in case of long inactivity.

    public void execute(UserInteractionsDb.UserInteractionRow userInteraction, Map<String, TopicScoreTuple> currentTopicScores) {
        List<UserTopicScoreDb.UserTopicScoreRow> userTopicScoreRows = userTopicScoreDb.getTopicsOfUser(userInteraction.userId);

        // Collect all interest and disinterest scores separately
        List<Double> allInterestScores = new ArrayList<>();
        List<Double> allDisinterestScores = new ArrayList<>();

        for (UserTopicScoreDb.UserTopicScoreRow row : userTopicScoreRows) {
            allInterestScores.add(row.interestScore);
            allDisinterestScores.add(Math.abs(row.disinterestScore)); // Use absolute value for sorting
        }

        // Sort to find Xth lowest scores
        Collections.sort(allInterestScores);
        Collections.sort(allDisinterestScores);

        // Find new minimums (Xth lowest becomes the baseline)
        double newMinInterestScore = allInterestScores.size() > TopicWithZeroScoreAllowed ? allInterestScores.get(TopicWithZeroScoreAllowed) : 0;
        double newMinDisinterestScore = allDisinterestScores.size() > TopicWithZeroScoreAllowed ? allDisinterestScores.get(TopicWithZeroScoreAllowed) : 0;
        double maxInterestScore = allInterestScores.get(allInterestScores.size() - 1);
        double maxDisinterestScore = allDisinterestScores.get(allDisinterestScores.size() - 1);

        // Process each topic
        for (UserTopicScoreDb.UserTopicScoreRow userTopicScoreRow : userTopicScoreRows) {
            double originalInterestScore = userTopicScoreRow.interestScore;
            double originalDisinterestScore = Math.abs(userTopicScoreRow.disinterestScore);

            long deltaTime = Instant.now().toEpochMilli() - userTopicScoreRow.updatedAt;
            double daysElapsed = deltaTime / (1000.0 * 60 * 60 * 24);
            double halfLifeDays = 30.0;
            double decayFactor = Math.pow(0.5, daysElapsed / halfLifeDays);

            // Calculate target max scores after decay
            double targetMaxInterestScore = maxInterestScore * decayFactor;
            double targetMaxDisinterestScore = maxDisinterestScore * decayFactor;

            // Apply ratio-preserving transformation for interest
            double decayedInterestScore;
            if (originalInterestScore <= newMinInterestScore) {
                decayedInterestScore = 0;
            } else {
                decayedInterestScore = ((originalInterestScore - newMinInterestScore) /
                        (maxInterestScore - newMinInterestScore)) * targetMaxInterestScore;
            }

            // Apply ratio-preserving transformation for disinterest
            double decayedDisinterestScore;
            if (originalDisinterestScore <= newMinDisinterestScore) {
                decayedDisinterestScore = 0;
            } else {
                decayedDisinterestScore = ((originalDisinterestScore - newMinDisinterestScore) /
                        (maxDisinterestScore - newMinDisinterestScore)) * targetMaxDisinterestScore;
                decayedDisinterestScore = -decayedDisinterestScore; // Convert back to negative
            }

            // Update the currentTopicScores map
            TopicScoreTuple tuple = currentTopicScores.get(userTopicScoreRow.topicName);
            if (tuple != null) {
                tuple.interestScore = decayedInterestScore;
                tuple.disinterestScore = decayedDisinterestScore;
            }
        }
    }
}
