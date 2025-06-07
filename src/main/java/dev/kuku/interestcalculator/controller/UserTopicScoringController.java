package dev.kuku.interestcalculator.controller;

import dev.kuku.interestcalculator.fakeDatabase.UserInteractionsDb;
import dev.kuku.interestcalculator.services.userTopicScoreAccumulator.UserTopicScoreAccumulatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserTopicScoringController {
    private final UserTopicScoreAccumulatorService userTopicScoreAccumulatorService;

    @PostMapping("/content/{contentId}/{interactionType}")
    public ResponseEntity<String> interact(
            @PathVariable("contentId") String contentId,
            @PathVariable("interactionType") String interactionType,
            @RequestParam(value = "discoveryMethod", defaultValue = "TRENDING") String discoveryMethod,
            @RequestParam(value = "userId", defaultValue = "123") String userId) {

        try {
            UserInteractionsDb.InteractionType interaction = UserInteractionsDb.InteractionType.valueOf(interactionType.toUpperCase());
            UserInteractionsDb.Discovery discovery = UserInteractionsDb.Discovery.valueOf(discoveryMethod.toUpperCase());
            long currentTime = System.currentTimeMillis();

            UserInteractionsDb.UserInteractionRow interactionRow = new UserInteractionsDb.UserInteractionRow(
                    userId, contentId, discovery, interaction, currentTime
            );

            userTopicScoreAccumulatorService.accumulate(userId, interactionRow);

            return ResponseEntity.ok("Interaction recorded successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid interaction type or discovery method");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error processing interaction");
        }
    }
}