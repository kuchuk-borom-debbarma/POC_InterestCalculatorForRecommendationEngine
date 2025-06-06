package dev.kuku.interestcalculator.services.userTopicScoreAccumulator;

import dev.kuku.interestcalculator.fakeDatabase.ContentDb;
import dev.kuku.interestcalculator.fakeDatabase.TopicDb;
import dev.kuku.interestcalculator.fakeDatabase.UserInteractionsDb;
import dev.kuku.interestcalculator.services.LLMService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UserTopicScoreAccumulatorService {
    // Momentum calculation parameters
    private static final double MOMENTUM_SCALING_FACTOR = 0.2;
    private static final double FREQUENCY_DECAY_RATE = 0.15; // per day
    private static final int MOMENTUM_WINDOW_DAYS = 7;
    private static final double MAX_MOMENTUM_MULTIPLIER = 1.5;
    private static final int MIN_CONSECUTIVE_FOR_MOMENTUM = 2;

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

    public UserTopicScoreAccumulatorService(ContentDb contentDb, TopicDb topicDb,
                                            LLMService llmService, TopicSetBaseScorer topicSetBaseScorer,
                                            UserInteractionsDb userInteractionsDb) {
        this.contentDb = contentDb;
        this.topicDb = topicDb;
        this.llmService = llmService;
        this.topicSetBaseScorer = topicSetBaseScorer;
        this.userInteractionsDb = userInteractionsDb;
    }

    public Map<String, Double> accumulate(String userId, UserInteractionsDb.UserInteractionRow interaction) {
        double rawBaseScore = topicSetBaseScorer.calculateRawScore(
                interaction.contentDiscovery, interaction.interactionType);

        ContentDb.ContentRow content = contentDb.getContentById(interaction.contentId);
        Set<String> topics = content.topics();
        if (topics == null || topics.isEmpty()) {
            topics = llmService.getTopics(topicDb.topics, content.content());
        }

        Map<String, Double> topicToScoreDelta = new HashMap<>();
        double baseScoreMagnitude = Math.abs(rawBaseScore);
        boolean isPositive = rawBaseScore > 0;

        for (String topic : topics) {
            double momentum = calculateMomentum(userId, topic, isPositive);
            double amplifiedScore = baseScoreMagnitude * momentum;
            double finalScore = applySaturation(amplifiedScore);

            // Apply direction and ensure score is within bounds
            double scoreDelta = Math.min(MAX_SCORE_DELTA,
                    Math.max(-MAX_SCORE_DELTA,
                            isPositive ? finalScore : -finalScore));
            topicToScoreDelta.put(topic, scoreDelta);
        }

        return topicToScoreDelta;
    }

    private double calculateMomentum(String userId, String topic, boolean isCurrentPositive) {
        long currentTime = Instant.now().toEpochMilli();
        long fromTime = Instant.now().minus(MOMENTUM_WINDOW_DAYS, ChronoUnit.DAYS).toEpochMilli();

        List<UserInteractionsDb.UserInteractionRow> recentHistory =
                userInteractionsDb.getInteractionsOfUserFromTo(userId, topic, fromTime, currentTime);

        int consecutive = countConsecutiveSameType(recentHistory, isCurrentPositive);
        double frequencyWeight = calculateFrequencyWeight(recentHistory, currentTime, isCurrentPositive);

        // Only apply momentum after minimum consecutive threshold
        double baseMultiplier = consecutive >= MIN_CONSECUTIVE_FOR_MOMENTUM ?
                1.0 + Math.log(1 + consecutive) * MOMENTUM_SCALING_FACTOR : 1.0;

        // Normalized momentum calculation
        double momentum = baseMultiplier * frequencyWeight;
        return Math.min(momentum, MAX_MOMENTUM_MULTIPLIER);
    }

    private double calculateFrequencyWeight(List<UserInteractionsDb.UserInteractionRow> recentHistory,
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
        // Normalized to smoothly approach MAX_MOMENTUM_MULTIPLIER
        return 1.0 + (avgWeight / (1 + avgWeight)) * (MAX_MOMENTUM_MULTIPLIER - 1.0);
    }

    private int countConsecutiveSameType(List<UserInteractionsDb.UserInteractionRow> recentHistory,
                                         boolean targetType) {
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
        // Sigmoid saturation adjusted to MAX_SCORE_DELTA range
        return MAX_SCORE_DELTA / (1 + Math.exp(-SATURATION_STEEPNESS * (score - SATURATION_INFLECTION)));
    }
}