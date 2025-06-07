package dev.kuku.interestcalculator.services.userTopicScoreAccumulator;

import dev.kuku.interestcalculator.dto.OperationDetailMap;
import dev.kuku.interestcalculator.fakeDatabase.ContentDb;
import dev.kuku.interestcalculator.fakeDatabase.TopicDb;
import dev.kuku.interestcalculator.fakeDatabase.UserInteractionsDb;
import dev.kuku.interestcalculator.fakeDatabase.UserTopicScoreDb;
import dev.kuku.interestcalculator.services.LLMService;
import dev.kuku.interestcalculator.util.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserTopicScoreAccumulatorService {
    // Configuration constants
    private static final double MOMENTUM_SCALING = 0.2;
    private static final double FREQUENCY_DECAY = 0.15;
    private static final double FREQUENCY_SCALE = 1.5;
    private static final int MOMENTUM_WINDOW = 7;
    private static final double MAX_MOMENTUM = 1.5;
    private static final double MAX_CONSECUTIVE = 1.3;
    private static final int MIN_CONSECUTIVE = 2;
    private static final int MAX_CONSECUTIVE_CAP = 10;

    private static final double MAX_SCORE = 10.0;
    private static final double MIN_SCORE = 0.0;
    private static final double SATURATION_STEEPNESS = 0.5;
    private static final double SATURATION_INFLECTION = MAX_SCORE * 0.6;

    private final ContentDb contentDb;
    private final TopicDb topicDb;
    private final LLMService llmService;
    private final TopicSetBaseScorer baseScorer;
    private final UserInteractionsDb userInteractionsDb;
    private final UserTopicScoreDb userTopicScoreDb;
    private final TimeProvider timeProvider;
    private final OperationDetailMap operationDetailMap;

    public void accumulate(String userId, UserInteractionsDb.UserInteractionRow interaction) {
        Map<String, Object> operationLog = operationDetailMap.operationDetailMap;

        // 1. Calculate raw base score
        double rawScore = baseScorer.calculateRawScore(interaction.contentDiscovery, interaction.interactionType);
        boolean isPositive = rawScore > 0;
        double scoreMagnitude = Math.abs(rawScore);

        operationLog.put("userId", userId);
        operationLog.put("contentId", interaction.contentId);
        operationLog.put("interactionType", interaction.interactionType);
        operationLog.put("discoveryMethod", interaction.contentDiscovery);
        operationLog.put("rawBaseScore", rawScore);

        // 2. Get topics for this content
        ContentDb.ContentRow content = contentDb.getContentById(interaction.contentId);
        Set<String> topics = getContentTopics(content, interaction.contentId);
        operationLog.put("contentTopics", topics);

        // 3. Calculate momentum for each topic
        Map<String, Map<String, Object>> topicCalculations = new LinkedHashMap<>();
        Map<String, Double> deltaScores = new LinkedHashMap<>();

        for (String topic : topics) {
            Map<String, Object> topicLog = new LinkedHashMap<>();

            // Calculate momentum components
            long now = timeProvider.nowMillis();
            long from = timeProvider.now().minus(MOMENTUM_WINDOW, ChronoUnit.DAYS).toEpochMilli();
            List<UserInteractionsDb.UserInteractionRow> history =
                    userInteractionsDb.getInteractionsOfUserFromTo(userId, topic, from, now);

            int consecutive = countConsecutive(history, isPositive);
            double freqMultiplier = calculateFrequencyMultiplier(history, now, isPositive);
            double consecMultiplier = calculateConsecutiveMultiplier(consecutive);
            double momentum = Math.min(consecMultiplier * freqMultiplier, MAX_MOMENTUM);

            topicLog.put("historySize", history.size());
            topicLog.put("consecutiveActions", consecutive);
            topicLog.put("frequencyMultiplier", freqMultiplier);
            topicLog.put("consecutiveMultiplier", consecMultiplier);
            topicLog.put("momentum", momentum);

            double amplifiedScore = scoreMagnitude * momentum;
            double deltaScore = isPositive ? amplifiedScore : -amplifiedScore;

            topicLog.put("amplifiedScore", amplifiedScore);
            topicLog.put("deltaScore", deltaScore);

            topicCalculations.put(topic, topicLog);
            deltaScores.put(topic, deltaScore);
        }

        operationLog.put("topicCalculations", topicCalculations);

        // 4. Apply delta with saturation
        Map<String, Map<String, Double>> scoreUpdates = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : deltaScores.entrySet()) {
            String topic = entry.getKey();
            double deltaScore = entry.getValue();
            double currentScore = userTopicScoreDb.getCurrentScore(userId, topic);
            double newTotalScore = currentScore + deltaScore;
            double saturatedScore = applySaturation(newTotalScore);

            Map<String, Double> scoreUpdate = new LinkedHashMap<>();
            scoreUpdate.put("currentScore", currentScore);
            scoreUpdate.put("delta", deltaScore);
            scoreUpdate.put("newRawScore", newTotalScore);
            scoreUpdate.put("newSaturatedScore", saturatedScore);

            scoreUpdates.put(topic, scoreUpdate);
        }

        operationLog.put("scoreUpdates", scoreUpdates);
        userTopicScoreDb.updateTopicScores(userId, deltaScores);
    }

    private Set<String> getContentTopics(ContentDb.ContentRow content, String contentId) {
        Set<String> topics = content.topics();
        if (topics == null || topics.isEmpty()) {
            topics = llmService.getTopics(topicDb.topics, content.content());
            topicDb.topics.addAll(topics);
            contentDb.setTopicsOfContent(topics, contentId);
        }
        return topics;
    }

    private double calculateMomentum(String userId, String topic, boolean isPositive) {
        long now = timeProvider.nowMillis();
        long from = timeProvider.now().minus(MOMENTUM_WINDOW, ChronoUnit.DAYS).toEpochMilli();

        List<UserInteractionsDb.UserInteractionRow> history =
                userInteractionsDb.getInteractionsOfUserFromTo(userId, topic, from, now);

        if (history.isEmpty()) return 1.0;

        int consecutive = countConsecutive(history, isPositive);
        double freqMultiplier = calculateFrequencyMultiplier(history, now, isPositive);
        double consecMultiplier = calculateConsecutiveMultiplier(consecutive);

        return Math.min(consecMultiplier * freqMultiplier, MAX_MOMENTUM);
    }

    private int countConsecutive(List<UserInteractionsDb.UserInteractionRow> history, boolean targetType) {
        history.sort((a, b) -> Long.compare(b.interactionTime, a.interactionTime));
        int count = 0;
        for (UserInteractionsDb.UserInteractionRow interaction : history) {
            double score = baseScorer.calculateRawScore(
                    interaction.contentDiscovery, interaction.interactionType);
            if ((score > 0) == targetType) count++;
            else break;
        }
        return Math.min(count, MAX_CONSECUTIVE_CAP);
    }

    private double calculateConsecutiveMultiplier(int consecutive) {
        if (consecutive < MIN_CONSECUTIVE) return 1.0;
        double boost = Math.log(1 + consecutive) * MOMENTUM_SCALING;
        return Math.min(1.0 + boost, MAX_CONSECUTIVE);
    }

    private double calculateFrequencyMultiplier(List<UserInteractionsDb.UserInteractionRow> history,
                                                long currentTime, boolean targetType) {
        double totalWeight = history.stream()
                .filter(interaction -> {
                    double score = baseScorer.calculateRawScore(
                            interaction.contentDiscovery, interaction.interactionType);
                    return (score > 0) == targetType;
                })
                .mapToDouble(interaction -> {
                    double ageDays = (currentTime - interaction.interactionTime) / 86400000.0;
                    return Math.exp(-FREQUENCY_DECAY * ageDays);
                })
                .sum();

        if (totalWeight == 0) return 1.0;

        double avgWeight = totalWeight / history.size();
        double maxFreqMultiplier = MAX_MOMENTUM / MAX_CONSECUTIVE;
        double normalized = Math.tanh(avgWeight * FREQUENCY_SCALE);

        return 1.0 + normalized * (maxFreqMultiplier - 1.0);
    }

    private double applySaturation(double totalScore) {
        // Handle negative scores by clamping to minimum
        if (totalScore <= MIN_SCORE) return MIN_SCORE;
        // Apply sigmoid saturation to approach MAX_SCORE asymptotically
        double sigmoid = 1.0 / (1 + Math.exp(-SATURATION_STEEPNESS * (totalScore - SATURATION_INFLECTION)));
        return sigmoid * MAX_SCORE;
    }
}