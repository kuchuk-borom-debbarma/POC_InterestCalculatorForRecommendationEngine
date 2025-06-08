package dev.kuku.interestcalculator.UserTopicScoringSystem.TopicScorer;

import dev.kuku.interestcalculator.UserTopicScoringSystem.TopicScorer.subSystem.InteractionScorer;
import dev.kuku.interestcalculator.UserTopicScoringSystem.TopicScorer.subSystem.InteractionTopicScorer;
import dev.kuku.interestcalculator.fakeDatabase.ContentDb;
import dev.kuku.interestcalculator.fakeDatabase.TopicDb;
import dev.kuku.interestcalculator.fakeDatabase.UserInteractionsDb;
import dev.kuku.interestcalculator.fakeDatabase.UserTopicScoreDb;
import dev.kuku.interestcalculator.services.LLMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calculates topic score based on interaction.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserTopicInteractionScorer {
    private static final double MIN_SCORE = 0.0;
    private static final double MAX_SCORE = 10.0;
    public static final double SATURATION_STRENGTH = 0.2;

    //Base interaction score
    private final InteractionScorer interactionScorer;
    //Topic Specific scoring
    private final InteractionTopicScorer interactionTopicScorer;
    private final ContentDb contentDb;
    private final LLMService llmService;
    private final TopicDb topicDb;
    private final UserTopicScoreDb userTopicScoreDb;

    public void scoreInteraction(String userId, UserInteractionsDb.UserInteractionRow interaction) {
        log.info("Scoring interaction: {}", interaction);
        ContentDb.ContentRow contentRow = contentDb.getContentById(interaction.contentId);
        Set<String> topics = contentRow.getTopics();
        if (topics == null || topics.isEmpty()) {
            topics = llmService.getTopics(topicDb.topics, contentRow.getContent());
            contentRow.setTopics(topics);
            topicDb.topics.addAll(topics);
        }
        //Interaction scoring. Applied to all topics.
        double delta = interactionScorer.calculateInteractionScoreDelta(interaction.contentDiscovery, interaction.interactionType);
        log.info("Delta: {}", delta);
        //Per topic scoring
        Map<String, Double> scoreMap = topics.stream()
                .collect(Collectors.toMap(t -> t, t -> {
                    double topicScore = interactionTopicScorer.scoreTopic(userId, t);
                    double topicDelta = topicScore * delta;
                    double currentScore = userTopicScoreDb.getCurrentScore(userId, t);
                    // Apply saturation using current score and the delta
                    return applySaturation(currentScore, topicDelta);
                }));
        userTopicScoreDb.updateTopicScores(userId, scoreMap);
    }

    /**
     * Apply saturation to score updates based on current score and delta.
     * Makes it progressively harder to reach extremes, with positive deltas
     * having more resistance near MAX_SCORE than negative deltas near MIN_SCORE.
     *
     * @param currentScore The current score before applying the delta
     * @param delta The change to apply (can be positive or negative)
     * @return The new score with saturation applied
     */
    private double applySaturation(double currentScore, double delta) {
        if (delta == 0) return currentScore;

        if (delta > 0) {
            // Logarithmic approach to max - gets harder as we approach MAX_SCORE
            double remaining = MAX_SCORE - currentScore;
            double saturationFactor = (remaining / MAX_SCORE) * SATURATION_STRENGTH;
            double saturatedDelta = delta * saturationFactor;
            return Math.min(MAX_SCORE, currentScore + saturatedDelta);
        } else {
            // Allow full negative delta effectiveness - can reach 0 (neutral)
            // Only apply minimal saturation when very close to 0 to prevent overshooting
            double distanceFromMin = currentScore - MIN_SCORE;
            if (distanceFromMin < 0.5) {
                // Light saturation only in the last 0.5 points to prevent overshooting
                double saturationFactor = Math.max(0.7, distanceFromMin / 0.5);
                double saturatedDelta = delta * saturationFactor;
                return Math.max(MIN_SCORE, currentScore + saturatedDelta);
            } else {
                // Full effectiveness when not near minimum
                return Math.max(MIN_SCORE, currentScore + delta);
            }
        }
    }
}