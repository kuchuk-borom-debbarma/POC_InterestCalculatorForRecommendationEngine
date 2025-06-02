package dev.kuku.interestcalculator;

import dev.kuku.interestcalculator.models.entities.*;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.util.Tuple;

import java.util.*;

@Service
public class Db {
    public final List<UserInteractionEntity> userInteractions = new ArrayList<>();
    public final Set<String> topics = new HashSet<>();
    public final List<ContentTopic> contentTopics = new ArrayList<>();
    public final List<PostEntity> posts = new ArrayList<>();
    public final List<TopicRelationshipEntity> topicRelationships = new ArrayList<>();
    public List<UserInterestEntity> userInterestEntities = List.of();

    /**
     * Finds a content topic by content ID
     */
    public ContentTopic findContentTopicByContentId(String contentId) {
        return contentTopics.stream()
                .filter(ct -> ct.contentId().equals(contentId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds a post entity by ID
     */
    public PostEntity findPostById(String id) {
        return posts.stream()
                .filter(e -> e.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Adds topics to the set of topics
     */
    public void addTopics(List<String> newTopics) {
        topics.addAll(newTopics);
    }

    /**
     * Adds a content topic
     */
    public void addContentTopic(ContentTopic contentTopic) {
        contentTopics.add(contentTopic);
    }

    /**
     * Gets user interests for a specific user ID
     */
    public Map<String, Tuple<Long, Long>> getUserInterests(String userId) {
        return userInterestEntities.stream()
                .filter(entity -> entity.userId().equals(userId))
                .findFirst()
                .map(UserInterestEntity::topics)
                .orElse(new HashMap<>());
    }

    /**
     * Updates user interests
     */
    public void updateUserInterests(List<UserInterestEntity> updatedInterests) {
        userInterestEntities = List.copyOf(updatedInterests);
    }

    /**
     * Gets user interactions in a specific time range for a user
     */
    public List<UserInteractionEntity> getUserInteractionsInTimeRange(String userId, long startTime, long endTime) {
        return userInteractions.stream()
                .filter(interaction ->
                        interaction.userId().equals(userId) &&
                        interaction.timestamp() >= startTime &&
                        interaction.timestamp() <= endTime)
                .toList();
    }

    /**
     * Adds a topic relationship
     */
    public void addTopicRelationship(TopicRelationshipEntity relationship) {
        topicRelationships.add(relationship);
    }

    /**
     * Removes a topic relationship
     */
    public void removeTopicRelationship(TopicRelationshipEntity relationship) {
        topicRelationships.remove(relationship);
    }

    /**
     * Finds a topic relationship by topic pair
     */
    public Optional<TopicRelationshipEntity> findTopicRelationship(String topic1, String topic2) {
        return topicRelationships.stream()
                .filter(tr -> tr.topic1().equals(topic1) && tr.topic2().equals(topic2))
                .findFirst();
    }

    /**
     * Updates topic relationships with a new list
     */
    public void updateTopicRelationships(List<TopicRelationshipEntity> updatedRelationships) {
        topicRelationships.clear();
        topicRelationships.addAll(updatedRelationships);
    }
}