package dev.kuku.interestcalculator.fakeDatabase;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserTopicScoreDb {

    private final List<UserTopicScoreRow> userTopicScores = new ArrayList<>();

    /**
     * Updates topic scores for a user. If a topic doesn't exist, creates a new entry.
     * If it exists, updates the score and timestamp.
     */
    public void updateTopicScores(String userId, Map<String, Double> topicScores) {
        long currentTime = Instant.now().toEpochMilli();

        for (Map.Entry<String, Double> entry : topicScores.entrySet()) {
            String topic = entry.getKey();
            double newScore = entry.getValue();

            // Find existing score entry
            Optional<UserTopicScoreRow> existingRow = userTopicScores.stream()
                    .filter(row -> row.userId.equals(userId) && row.topic.equals(topic))
                    .findFirst();

            if (existingRow.isPresent()) {
                // Update existing entry
                UserTopicScoreRow row = existingRow.get();
                row.interestScore = newScore;
                row.updatedAt = currentTime;
            } else {
                // Create new entry
                userTopicScores.add(new UserTopicScoreRow(userId, topic, newScore, currentTime));
            }
        }
    }

    /**
     * Gets the current accumulated score for a specific user-topic pair.
     * Returns 0.0 if no score exists yet.
     */
    public double getCurrentScore(String userId, String topic) {
        return userTopicScores.stream()
                .filter(row -> row.userId.equals(userId) && row.topic.equals(topic))
                .findFirst()
                .map(row -> row.interestScore)
                .orElse(0.0);
    }

    public List<UserTopicScoreRow> getUserTopicScores(String userId) {
        return userTopicScores.stream()
                .filter(row -> row.userId.equals(userId))
                .toList();
    }


    @AllArgsConstructor
    public static class UserTopicScoreRow {
        public String userId;
        public String topic;
        public double interestScore;
        public long updatedAt;
    }
}