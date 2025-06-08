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

    public void updateTopicScoresByValue(String userId, Map<String, Double> value) {
        long currentTime = Instant.now().toEpochMilli();

        for (Map.Entry<String, Double> entry : value.entrySet()) {
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

    public void updateTopicScoresByDelta(String userId, Map<String, Double> delta) {
        long currentTime = Instant.now().toEpochMilli();

        for (Map.Entry<String, Double> entry : delta.entrySet()) {
            String topic = entry.getKey();
            double deltaValue = entry.getValue();

            // Find existing score entry
            Optional<UserTopicScoreRow> existingRow = userTopicScores.stream()
                    .filter(row -> row.userId.equals(userId) && row.topic.equals(topic))
                    .findFirst();

            if (existingRow.isPresent()) {
                // Update existing entry by adding delta to current score
                UserTopicScoreRow row = existingRow.get();
                row.interestScore += deltaValue;
                row.updatedAt = currentTime;
            } else {
                // Create new entry with delta as initial score
                userTopicScores.add(new UserTopicScoreRow(userId, topic, deltaValue, currentTime));
            }
        }
    }

    public double getTopicScoreOfUser(String userId, String topic) {
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