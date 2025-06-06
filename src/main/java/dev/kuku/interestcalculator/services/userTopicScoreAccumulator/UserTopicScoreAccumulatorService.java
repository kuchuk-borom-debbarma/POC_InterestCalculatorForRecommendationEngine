package dev.kuku.interestcalculator.services.userTopicScoreAccumulator;

import dev.kuku.interestcalculator.fakeDatabase.ContentDb;
import dev.kuku.interestcalculator.fakeDatabase.TopicDb;
import dev.kuku.interestcalculator.fakeDatabase.UserInteractionsDb;
import dev.kuku.interestcalculator.services.LLMService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserTopicScoreAccumulatorService {
    // Momentum calculation parameters
    private static final double MOMENTUM_SCALING_FACTOR = 0.2;
    private static final double FREQUENCY_DECAY_RATE = 0.15; // per day
    private static final double FREQUENCY_SCALE_FACTOR = 1.5; // Controls frequency weight sensitivity
    private static final int MOMENTUM_WINDOW_DAYS = 7;
    private static final double MAX_MOMENTUM_MULTIPLIER = 1.5;
    private static final double MAX_CONSECUTIVE_MULTIPLIER = 1.3; // Separate cap for consecutive bonus
    private static final int MIN_CONSECUTIVE_FOR_MOMENTUM = 2;
    private static final int MAX_CONSECUTIVE_CAP = 10; // Prevent extreme consecutive counts

    // Score saturation parameters
    private static final double MAX_SCORE_DELTA = 10.0;
    private static final double SATURATION_STEEPNESS = 0.5;
    private static final double SATURATION_INFLECTION_RATIO = 0.6; // of MAX_SCORE_DELTA
    private static final double SATURATION_INFLECTION = MAX_SCORE_DELTA * SATURATION_INFLECTION_RATIO;

    private final ContentDb contentDb;
    private final TopicDb topicDb;
    private final LLMService llmService;
    private final TopicSetBaseScorer topicSetBaseScorer;
    private final UserInteractionsDb userInteractionsDb;

    public Map<String, Double> accumulate(String userId, UserInteractionsDb.UserInteractionRow interaction) {
        double rawBaseScore = topicSetBaseScorer.calculateRawScore(
                interaction.contentDiscovery, interaction.interactionType);

        ContentDb.ContentRow content = contentDb.getContentById(interaction.contentId);
        Set<String> topics = content.topics();
        if (topics == null || topics.isEmpty()) {
            topics = llmService.getTopics(topicDb.topics, content.content());
            topicDb.topics.addAll(topics);
            contentDb.setTopicsOfContent(topics, interaction.contentId);
        }

        Map<String, Double> topicToScoreDelta = new HashMap<>();
        double baseScoreMagnitude = Math.abs(rawBaseScore);
        boolean isPositive = rawBaseScore > 0;

        for (String topic : topics) {
            double momentum = calculateMomentum(userId, topic, isPositive);
            double amplifiedScore = baseScoreMagnitude * momentum;
            double finalScore = applySaturation(amplifiedScore);
            double scoreDelta = isPositive ? finalScore : -finalScore;
            topicToScoreDelta.put(topic, scoreDelta);
        }

        return topicToScoreDelta;
    }

    private double calculateMomentum(String userId, String topic, boolean isCurrentPositive) {
        long currentTime = Instant.now().toEpochMilli();
        long fromTime = Instant.now().minus(MOMENTUM_WINDOW_DAYS, ChronoUnit.DAYS).toEpochMilli();

        List<UserInteractionsDb.UserInteractionRow> recentHistory =
                userInteractionsDb.getInteractionsOfUserFromTo(userId, topic, fromTime, currentTime);

        // Handle edge case: no history
        if (recentHistory.isEmpty()) {
            return 1.0;
        }

        int consecutive = countConsecutiveSameType(recentHistory, isCurrentPositive);
        double frequencyMultiplier = calculateFrequencyMultiplier(recentHistory, currentTime, isCurrentPositive);
        double consecutiveMultiplier = calculateConsecutiveMultiplier(consecutive);

        // Multiplicative momentum calculation with proper bounds
        double momentum = consecutiveMultiplier * frequencyMultiplier;
        return Math.min(momentum, MAX_MOMENTUM_MULTIPLIER);
    }

    private double calculateConsecutiveMultiplier(int consecutive) {
        // Only apply momentum after minimum consecutive threshold
        if (consecutive < MIN_CONSECUTIVE_FOR_MOMENTUM) {
            return 1.0;
        }

        // Cap consecutive count to prevent extreme values
        int cappedConsecutive = Math.min(consecutive, MAX_CONSECUTIVE_CAP);

        // Bounded logarithmic scaling with separate maximum
        double consecutiveBoost = Math.log(1 + cappedConsecutive) * MOMENTUM_SCALING_FACTOR;
        double multiplier = 1.0 + consecutiveBoost;

        return Math.min(multiplier, MAX_CONSECUTIVE_MULTIPLIER);
    }

    private double calculateFrequencyMultiplier(List<UserInteractionsDb.UserInteractionRow> recentHistory,
                                                long currentTime, boolean targetType) {
        if (recentHistory.isEmpty()) return 1.0;

        double totalWeight = 0.0;
        int relevantCount = 0;

        for (UserInteractionsDb.UserInteractionRow interaction : recentHistory) {
            double rawScore = topicSetBaseScorer.calculateRawScore(
                    interaction.contentDiscovery, interaction.interactionType);

            if ((rawScore > 0) == targetType) {
                double ageInDays = (currentTime - interaction.interactionTime) / (1000.0 * 60 * 60 * 24);
                totalWeight += Math.exp(-FREQUENCY_DECAY_RATE * ageInDays);
                relevantCount++;
            }
        }

        if (relevantCount == 0) return 1.0;

        double avgWeight = totalWeight / relevantCount;

        // Use hyperbolic tangent for principled bounded scaling
        // tanh provides smooth S-curve from 0 to 1, then scaled to [1, MAX_FREQUENCY_MULTIPLIER]
        double maxFrequencyMultiplier = MAX_MOMENTUM_MULTIPLIER / MAX_CONSECUTIVE_MULTIPLIER; // Balanced allocation
        double normalizedWeight = Math.tanh(avgWeight * FREQUENCY_SCALE_FACTOR);

        return 1.0 + normalizedWeight * (maxFrequencyMultiplier - 1.0);
    }

    private int countConsecutiveSameType(List<UserInteractionsDb.UserInteractionRow> recentHistory,
                                         boolean targetType) {
        if (recentHistory.isEmpty()) return 0;

        // Sort by interaction time (most recent first)
        recentHistory.sort((a, b) -> Long.compare(b.interactionTime, a.interactionTime));

        int count = 0;
        for (UserInteractionsDb.UserInteractionRow interaction : recentHistory) {
            double rawScore = topicSetBaseScorer.calculateRawScore(
                    interaction.contentDiscovery, interaction.interactionType);

            if ((rawScore > 0) == targetType) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    private double applySaturation(double score) {
        // Handle edge case: negative or zero scores
        if (score <= 0) return 0.0;

        double sigmoid = 1.0 / (1 + Math.exp(-SATURATION_STEEPNESS * (score - SATURATION_INFLECTION)));
        return sigmoid * MAX_SCORE_DELTA;
    }
}