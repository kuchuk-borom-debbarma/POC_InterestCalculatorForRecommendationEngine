package dev.kuku.interestcalculator.services.userInterest;

import dev.kuku.interestcalculator.models.UserInteractionData;
import dev.kuku.interestcalculator.models.entities.UserInterestEntity;
import dev.kuku.interestcalculator.repo.UserInteractionRepo;
import dev.kuku.interestcalculator.repo.UserInterestRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.util.Tuple;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserInterestService {
    private final UserInteractionRepo userInterestRepository;
    private final UserInterestRepo userInterestRepo;

    public void calculateUserInterestScore(Set<UserInteractionData> userInteractions) {

    }

    public void decayInterestScore(String userId) {
        var userInterestsMap = userInterestRepo.getUserInterests(userId);
        var currentTime = Instant.now().toEpochMilli();

        // If no interests for user, nothing to decay
        if (userInterestsMap.isEmpty()) {
            return;
        }

        // Create a map to hold the updated scores
        Map<String, Tuple<Integer, Long>> updatedTopics = new HashMap<>();

        // Process each topic and apply appropriate decay factor
        for (Map.Entry<String, Tuple<Integer, Long>> entry : userInterestsMap.entrySet()) {
            String topic = entry.getKey();
            Integer score = entry.getValue()._1();
            Long lastUpdatedTimestamp = entry.getValue()._2();
            long timeElapsed = currentTime - lastUpdatedTimestamp;
            long daysElapsed = timeElapsed / (24 * 60 * 60 * 1000);
            // Apply decay factor based on elapsed time as per README
            double decayFactor = 1.0; // Default: no decay
            if (daysElapsed <= 7) {
                // Recent interactions (0-7 days): No decay (1.0x)
            } else if (daysElapsed <= 28) {
                // Moderate age (1-4 weeks): Slight decay (0.8-0.9x)
                // Linear interpolation between 0.9 and 0.8
                decayFactor = 0.9 - ((daysElapsed - 7) / 21.0) * 0.1;
            } else if (daysElapsed <= 180) {
                // Older interactions (1-6 months): Significant decay (0.3-0.7x)
                // Linear interpolation between 0.7 and 0.3
                decayFactor = 0.7 - ((daysElapsed - 28) / 152.0) * 0.4;
            } else {
                // Historical data (6+ months): Minimal impact (0.1-0.2x)
                // Linear interpolation between 0.2 and 0.1
                decayFactor = 0.2 - Math.min(((daysElapsed - 180) / 180.0) * 0.1, 0.1);
            }
            // Apply decay to score
            int decayedScore = (int) Math.max(0, Math.round(score * decayFactor));

            // Add to updated topics with the original timestamp (we're only decaying score, not updating timestamp)
            updatedTopics.put(topic, new Tuple<>(decayedScore, lastUpdatedTimestamp));
        }

        // Update the user's interests with decayed scores
        UserInterestEntity updatedEntity = new UserInterestEntity(userId, updatedTopics);

        userInterestRepo.saveUserInterests(updatedEntity);
    }
}