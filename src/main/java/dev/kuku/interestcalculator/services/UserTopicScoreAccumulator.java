package dev.kuku.interestcalculator.services;

import dev.kuku.interestcalculator.config.UserTopicScoreConfiguration;
import dev.kuku.interestcalculator.fakeDatabase.ContentDb;
import dev.kuku.interestcalculator.fakeDatabase.TopicDb;
import dev.kuku.interestcalculator.fakeDatabase.UserInteractionsDb;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 *
 */
@Service
@RequiredArgsConstructor
public class UserTopicScoreAccumulator {
    private final UserTopicScoreConfiguration config;
    private final UserInteractionsDb userInteractionsDb;
    private final ContentDb contentDb;
    private final TopicDb topicDb;
    private final LLMService llmService;

    public void accumulate(String userId, UserInteractionsDb.UserInteractionRow interaction) {
        //Set the base score for the interaction
        double baseScore = calculateBaseScore(interaction.contentDiscovery, interaction.interactionType);
        ContentDb.ContentRow content = contentDb.getContentById(interaction.contentId);
        //Get topics for the content
        Set<String> topics = content.topics();
        if (topics == null || topics.isEmpty()) {
            topics = llmService.getTopics(topicDb.topics, content.content());
        }
        //Per topic scaling
        calculateTopicScore(userId, topics);
    }

    public void calculateTopicScore(String userId, Set<String> topics) {
        if (config.getAccumulator().isUserActivityScalingMultiplierEnabled()) {
            //TODO
        }
        if (config.getAccumulator().isTopicRecencyMultiplier()) {
            //TODO move this section into its own function
        }
    }

    public double calculateBaseScore(UserInteractionsDb.Discovery contentDiscovery, UserInteractionsDb.InteractionType interactionType) {
        double currentScore = 0.0;
        if (config.getAccumulator().isContentDiscoveryMultiplierEnabled()) {
            currentScore += getContentDiscoveryMultiplier(contentDiscovery);
        }
        if (config.getAccumulator().isInteractionTypeMultiplierEnabled()) {
            currentScore += getInteractionMultiplier(interactionType);
        }
        return currentScore;
    }

    private double getContentDiscoveryMultiplier(UserInteractionsDb.Discovery discovery) {
        return switch (discovery) {
            case SEARCH ->
                    config.getAccumulator().getMultiplier().getContentDiscoveryMultiplier().getSearchMultiplier();
            case TRENDING ->
                    config.getAccumulator().getMultiplier().getContentDiscoveryMultiplier().getTrendingMultiplier();
            case RECOMMENDATION ->
                    config.getAccumulator().getMultiplier().getContentDiscoveryMultiplier().getRecommendationMultiplier();
        };
    }

    private double getInteractionMultiplier(UserInteractionsDb.InteractionType interactionType) {
        return switch (interactionType) {
            case COMMENT ->
                    config.getAccumulator().getMultiplier().getInteractionTypeMultiplier().getCommentMultiplier();
            case LIKE -> config.getAccumulator().getMultiplier().getInteractionTypeMultiplier().getLikeMultiplier();
            case REPORT -> config.getAccumulator().getMultiplier().getInteractionTypeMultiplier().getReportMultiplier();
        };
    }

    private double applySaturation(double score) {
        //Sigmoid
        double k = 0.1; // Controls steepness
        double midpoint = config.getTopicScoreMax() / 2;
        return config.getTopicScoreMax() / (1 + Math.exp(-k * (score - midpoint)));
    }

    private void getTopicRecencyMultiplier(String userId, Set<String> topics) {
        List<UserInteractionsDb.UserInteractionRow> interactions = userInteractionsDb.getTopicInteractionOfUserFromTo(userId, topics);
    }
}
