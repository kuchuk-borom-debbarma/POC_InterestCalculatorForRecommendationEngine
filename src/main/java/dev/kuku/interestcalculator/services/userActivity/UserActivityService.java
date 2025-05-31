package dev.kuku.interestcalculator.services.userActivity;

import dev.kuku.interestcalculator.models.ActivityLevel;
import dev.kuku.interestcalculator.models.InteractionType;
import dev.kuku.interestcalculator.models.entities.UserInteractionEntity;
import dev.kuku.interestcalculator.repo.UserInteractionRepo;
import dev.kuku.interestcalculator.services.platform.PlatformService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.util.function.Tuple2;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserActivityService {
    private final UserInteractionRepo userInteractionRepo;
    private final PlatformService platformService;
    
    // Activity level thresholds based on weighted interaction scores
    private static final Map<ActivityLevel, Integer> ACTIVITY_THRESHOLDS = Map.of(
            ActivityLevel.NO_ACTIVITY, 0,
            ActivityLevel.LOW_ACTIVITY, 50,
            ActivityLevel.LOW_MID_ACTIVITY, 200,
            ActivityLevel.MID_ACTIVITY, 500,
            ActivityLevel.MID_HIGH_ACTIVITY, 1000,
            ActivityLevel.HIGH_ACTIVITY, 2000,
            ActivityLevel.NOLIFER_ACTIVITY, 5000
    );

    public ActivityLevel getActivityLevelOfUser(String userId, Tuple2<Long, Long> timespan) {
        // Get user interactions for the specified timespan
        List<UserInteractionEntity> userInteractions = userInteractionRepo.getUserInteractionByTimespan(userId, timespan);
        
        // If no interactions, return NO_ACTIVITY
        if (userInteractions.isEmpty()) {
            return ActivityLevel.NO_ACTIVITY;
        }
        
        // Get interaction weights from PlatformService
        Map<InteractionType, Integer> interactionWeights = platformService.getInteractionWeight(
                reactor.util.function.Tuples.of(1, 10)  // Configurable weight range, adjust as needed
        );
        
        // Calculate the weighted activity score based on interaction types
        int activityScore = calculateActivityScore(userInteractions, interactionWeights);
        
        // Determine activity level based on thresholds
        return determineActivityLevel(activityScore);
    }
    
    private int calculateActivityScore(List<UserInteractionEntity> interactions, 
                                     Map<InteractionType, Integer> weights) {
        return interactions.stream()
                .mapToInt(interaction -> weights.getOrDefault(interaction.interactionType(), 0))
                .sum();
    }
    
    private ActivityLevel determineActivityLevel(int activityScore) {
        if (activityScore >= ACTIVITY_THRESHOLDS.get(ActivityLevel.NOLIFER_ACTIVITY)) {
            return ActivityLevel.NOLIFER_ACTIVITY;
        } else if (activityScore >= ACTIVITY_THRESHOLDS.get(ActivityLevel.HIGH_ACTIVITY)) {
            return ActivityLevel.HIGH_ACTIVITY;
        } else if (activityScore >= ACTIVITY_THRESHOLDS.get(ActivityLevel.MID_HIGH_ACTIVITY)) {
            return ActivityLevel.MID_HIGH_ACTIVITY;
        } else if (activityScore >= ACTIVITY_THRESHOLDS.get(ActivityLevel.MID_ACTIVITY)) {
            return ActivityLevel.MID_ACTIVITY;
        } else if (activityScore >= ACTIVITY_THRESHOLDS.get(ActivityLevel.LOW_MID_ACTIVITY)) {
            return ActivityLevel.LOW_MID_ACTIVITY;
        } else if (activityScore >= ACTIVITY_THRESHOLDS.get(ActivityLevel.LOW_ACTIVITY)) {
            return ActivityLevel.LOW_ACTIVITY;
        } else {
            return ActivityLevel.NO_ACTIVITY;
        }
    }
}