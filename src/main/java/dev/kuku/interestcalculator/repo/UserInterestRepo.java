package dev.kuku.interestcalculator.repo;

import dev.kuku.interestcalculator.models.entities.UserInterestEntity;
import org.springframework.stereotype.Repository;
import org.yaml.snakeyaml.util.Tuple;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.ArrayList;

@Repository
public class UserInterestRepo {
    private List<UserInterestEntity> userInterestEntities = List.of();

    public Map<String, Tuple<Integer, Long>> getUserInterests(String userId) {
        return userInterestEntities.stream()
                .filter(entity -> entity.userId().equals(userId))
                .findFirst()
                .map(UserInterestEntity::topics)
                .orElse(new HashMap<>());
    }

    public void saveUserInterests(UserInterestEntity updatedEntity) {
        // Convert the immutable List to a mutable ArrayList
        List<UserInterestEntity> mutableList = new ArrayList<>(userInterestEntities);
        
        // Find the index of the user's existing entity (if any)
        int existingIndex = -1;
        for (int i = 0; i < mutableList.size(); i++) {
            if (mutableList.get(i).userId().equals(updatedEntity.userId())) {
                existingIndex = i;
                break;
            }
        }
        
        // Update or add the entity
        if (existingIndex != -1) {
            // Replace the existing entity
            mutableList.set(existingIndex, updatedEntity);
        } else {
            // Add a new entity
            mutableList.add(updatedEntity);
        }
        
        // Update the repository with the modified list
        userInterestEntities = List.copyOf(mutableList);
    }
}