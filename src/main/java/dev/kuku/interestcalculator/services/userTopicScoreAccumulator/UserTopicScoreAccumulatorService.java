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
    private static final double FREQUENCY_SCALE = 1.2;
    private static final int MOMENTUM_WINDOW = 7;
    private static final double MAX_MOMENTUM = 1.3;
    private static final double MAX_CONSECUTIVE = 1.3;
    private static final int MIN_CONSECUTIVE = 5;
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
        // Null safety checks
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (interaction == null) {
            throw new IllegalArgumentException("Interaction cannot be null");
        }
        if (interaction.contentId == null || interaction.contentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Content ID cannot be null or empty");
        }
        if (interaction.interactionType == null) {
            throw new IllegalArgumentException("Interaction type cannot be null");
        }
        if (interaction.contentDiscovery == null) {
            throw new IllegalArgumentException("Content discovery cannot be null");
        }

        Map<String, Object> operationLog = getOperationLog();

        try {
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
            if (content == null) {
                operationLog.put("error", "Content not found for ID: " + interaction.contentId);
                return;
            }

            Set<String> topics = getContentTopics(content, interaction.contentId);
            if (topics == null || topics.isEmpty()) {
                operationLog.put("warning", "No topics found for content ID: " + interaction.contentId);
                return;
            }

            operationLog.put("contentTopics", topics);

            // 3. Calculate momentum for each topic
            Map<String, Map<String, Object>> topicCalculations = new LinkedHashMap<>();
            Map<String, Double> deltaScores = new LinkedHashMap<>();

            for (String topic : topics) {
                if (topic == null || topic.trim().isEmpty()) {
                    continue; // Skip null or empty topics
                }

                Map<String, Object> topicLog = new LinkedHashMap<>();

                try {
                    // Calculate momentum components
                    long now = timeProvider.nowMillis();
                    long from = timeProvider.now().minus(MOMENTUM_WINDOW, ChronoUnit.DAYS).toEpochMilli();
                    List<UserInteractionsDb.UserInteractionRow> history =
                            userInteractionsDb.getInteractionsOfUserFromTo(userId, topic, from, now);

                    // Null safety for history
                    if (history == null) {
                        history = new ArrayList<>();
                    }

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
                } catch (Exception e) {
                    topicLog.put("error", "Failed to calculate momentum for topic: " + topic + " - " + e.getMessage());
                    topicCalculations.put(topic, topicLog);
                }
            }

            operationLog.put("topicCalculations", topicCalculations);

            // 4. Apply delta with saturation
            Map<String, Map<String, Double>> scoreUpdates = new LinkedHashMap<>();
            for (Map.Entry<String, Double> entry : deltaScores.entrySet()) {
                if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }

                String topic = entry.getKey();
                double deltaScore = entry.getValue();

                try {
                    double currentScore = userTopicScoreDb.getCurrentScore(userId, topic);
                    double newTotalScore = currentScore + deltaScore;
                    double saturatedScore = applySaturation(newTotalScore);

                    Map<String, Double> scoreUpdate = new LinkedHashMap<>();
                    scoreUpdate.put("currentScore", currentScore);
                    scoreUpdate.put("delta", deltaScore);
                    scoreUpdate.put("newRawScore", newTotalScore);
                    scoreUpdate.put("newSaturatedScore", saturatedScore);

                    scoreUpdates.put(topic, scoreUpdate);
                } catch (Exception e) {
                    Map<String, Double> scoreUpdate = new LinkedHashMap<>();
                    scoreUpdate.put("error", Double.NaN);
                    scoreUpdates.put(topic, scoreUpdate);
                }
            }

            operationLog.put("scoreUpdates", scoreUpdates);

            // Only update if we have valid delta scores
            if (!deltaScores.isEmpty()) {
                userTopicScoreDb.updateTopicScores(userId, deltaScores);
            }

        } catch (Exception e) {
            operationLog.put("error", "Failed to accumulate scores: " + e.getMessage());
            throw new RuntimeException("Error processing user topic score accumulation", e);
        }
    }

    private Map<String, Object> getOperationLog() {
        if (operationDetailMap == null || operationDetailMap.operationDetailMap == null) {
            return new LinkedHashMap<>();
        }
        return operationDetailMap.operationDetailMap;
    }

    private Set<String> getContentTopics(ContentDb.ContentRow content, String contentId) {
        if (content == null) {
            return Collections.emptySet();
        }

        Set<String> topics = content.topics();
        if (topics == null || topics.isEmpty()) {
            try {
                String contentText = content.content();
                if (contentText == null || contentText.trim().isEmpty()) {
                    return Collections.emptySet();
                }

                Set<String> existingTopics = (topicDb != null && topicDb.topics != null)
                        ? topicDb.topics
                        : new HashSet<>();

                topics = llmService.getTopics(existingTopics, contentText);

                if (topics != null && !topics.isEmpty()) {
                    // Safely add topics to database
                    if (topicDb != null && topicDb.topics != null) {
                        topicDb.topics.addAll(topics);
                    }
                    contentDb.setTopicsOfContent(topics, contentId);
                } else {
                    topics = Collections.emptySet();
                }
            } catch (Exception e) {
                // Log error and return empty set
                return Collections.emptySet();
            }
        }

        // Filter out null or empty topics
        return topics.stream()
                .filter(Objects::nonNull)
                .filter(topic -> !topic.trim().isEmpty())
                .collect(Collectors.toSet());
    }

    private double calculateMomentum(String userId, String topic, boolean isPositive) {
        if (userId == null || userId.trim().isEmpty() || topic == null || topic.trim().isEmpty()) {
            return 1.0;
        }

        try {
            long now = timeProvider.nowMillis();
            long from = timeProvider.now().minus(MOMENTUM_WINDOW, ChronoUnit.DAYS).toEpochMilli();

            List<UserInteractionsDb.UserInteractionRow> history =
                    userInteractionsDb.getInteractionsOfUserFromTo(userId, topic, from, now);

            if (history == null || history.isEmpty()) {
                return 1.0;
            }

            int consecutive = countConsecutive(history, isPositive);
            double freqMultiplier = calculateFrequencyMultiplier(history, now, isPositive);
            double consecMultiplier = calculateConsecutiveMultiplier(consecutive);

            return Math.min(consecMultiplier * freqMultiplier, MAX_MOMENTUM);
        } catch (Exception e) {
            return 1.0; // Return default momentum on error
        }
    }

    private int countConsecutive(List<UserInteractionsDb.UserInteractionRow> history, boolean targetType) {
        if (history == null || history.isEmpty()) {
            return 0;
        }

        // Create a defensive copy and filter out null entries
        List<UserInteractionsDb.UserInteractionRow> validHistory = history.stream()
                .filter(Objects::nonNull)
                .filter(interaction -> interaction.contentDiscovery != null && interaction.interactionType != null)
                .collect(Collectors.toList());

        if (validHistory.isEmpty()) {
            return 0;
        }

        validHistory.sort((a, b) -> Long.compare(b.interactionTime, a.interactionTime));

        int count = 0;
        for (UserInteractionsDb.UserInteractionRow interaction : validHistory) {
            try {
                double score = baseScorer.calculateRawScore(
                        interaction.contentDiscovery, interaction.interactionType);
                if ((score > 0) == targetType) {
                    count++;
                } else {
                    break;
                }
            } catch (Exception e) {
                // Skip invalid interactions
                break;
            }
        }
        return Math.min(count, MAX_CONSECUTIVE_CAP);
    }

    private double calculateConsecutiveMultiplier(int consecutive) {
        if (consecutive < MIN_CONSECUTIVE) {
            return 1.0;
        }
        double boost = Math.log(1 + consecutive) * MOMENTUM_SCALING;
        return Math.min(1.0 + boost, MAX_CONSECUTIVE);
    }

    private double calculateFrequencyMultiplier(List<UserInteractionsDb.UserInteractionRow> history,
                                                long currentTime, boolean targetType) {
        if (history == null || history.isEmpty()) {
            return 1.0;
        }

        // Filter out null entries and calculate total weight
        double totalWeight = history.stream()
                .filter(Objects::nonNull)
                .filter(interaction -> interaction.contentDiscovery != null && interaction.interactionType != null)
                .filter(interaction -> {
                    try {
                        double score = baseScorer.calculateRawScore(
                                interaction.contentDiscovery, interaction.interactionType);
                        return (score > 0) == targetType;
                    } catch (Exception e) {
                        return false; // Skip invalid interactions
                    }
                })
                .mapToDouble(interaction -> {
                    try {
                        double ageDays = (currentTime - interaction.interactionTime) / 86400000.0;
                        return Math.exp(-FREQUENCY_DECAY * ageDays);
                    } catch (Exception e) {
                        return 0.0; // Skip invalid interactions
                    }
                })
                .sum();

        if (totalWeight == 0) {
            return 1.0;
        }

        double avgWeight = totalWeight / history.size();
        double maxFreqMultiplier = MAX_MOMENTUM / MAX_CONSECUTIVE;
        double normalized = Math.tanh(avgWeight * FREQUENCY_SCALE);

        return 1.0 + normalized * (maxFreqMultiplier - 1.0);
    }

    private double applySaturation(double totalScore) {
        // Handle NaN and infinite values
        if (Double.isNaN(totalScore) || Double.isInfinite(totalScore)) {
            return MIN_SCORE;
        }

        // Handle negative scores by clamping to minimum
        if (totalScore <= MIN_SCORE) {
            return MIN_SCORE;
        }

        try {
            // Apply sigmoid saturation to approach MAX_SCORE asymptotically
            double exponent = -SATURATION_STEEPNESS * (totalScore - SATURATION_INFLECTION);
            double sigmoid = 1.0 / (1 + Math.exp(exponent));
            double result = sigmoid * MAX_SCORE;

            // Ensure result is within bounds
            return Math.min(Math.max(result, MIN_SCORE), MAX_SCORE);
        } catch (Exception e) {
            // Return safe default on mathematical errors
            return Math.min(totalScore, MAX_SCORE);
        }
    }
}