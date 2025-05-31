package dev.kuku.interestcalculator.repo;

import dev.kuku.interestcalculator.models.entities.UserInteractionEntity;
import org.springframework.stereotype.Repository;
import reactor.util.function.Tuple2;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class UserInteractionRepo {
    private List<UserInteractionEntity> userInteractionEntities = List.of();

    /// Get the user interaction data of a user in a timespan
    public List<UserInteractionEntity> getUserInteractionByTimespan(String userId, Tuple2<Long, Long> timespan) {
        long startTime = timespan.getT1();
        long endTime = timespan.getT2();
        
        return userInteractionEntities.stream()
                .filter(interaction -> interaction.userId().equals(userId))
                .filter(interaction -> interaction.timestamp() >= startTime && interaction.timestamp() <= endTime)
                .collect(Collectors.toList());
    }
}