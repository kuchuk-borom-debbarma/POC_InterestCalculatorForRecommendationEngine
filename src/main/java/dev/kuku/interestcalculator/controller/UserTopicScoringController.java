package dev.kuku.interestcalculator.controller;

import dev.kuku.interestcalculator.dto.OperationDetailMap;
import dev.kuku.interestcalculator.fakeDatabase.ContentDb;
import dev.kuku.interestcalculator.fakeDatabase.UserInteractionsDb;
import dev.kuku.interestcalculator.services.userTopicScoreAccumulator.UserTopicScoreAccumulatorService;
import dev.kuku.interestcalculator.util.TestTimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Profile("test")
@Slf4j
@CrossOrigin(origins = "*")  // Add this annotation
public class UserTopicScoringController {
    private final UserTopicScoreAccumulatorService userTopicScoreAccumulatorService;
    private final TestTimeProvider testTimeProvider;
    private final UserInteractionsDb userInteractionsDb;
    private final ContentDb contentDb;
    private final OperationDetailMap operationDetailMap;

    @GetMapping("/content")
    public ResponseEntity<List<ContentDb.ContentRow>> getAllContents() {
        log.debug("Getting all contents");
        return ResponseEntity.ok(contentDb.getAllContents());
    }

    @PostMapping("/content/{contentId}/{interactionType}")
    public ResponseEntity<Map<String, Object>> interact(
            @PathVariable("contentId") String contentId,
            @PathVariable("interactionType") String interactionType,
            @RequestParam(value = "discoveryMethod", defaultValue = "TRENDING") String discoveryMethod,
            @RequestParam(value = "userId", defaultValue = "123") String userId) {

        try {
            UserInteractionsDb.InteractionType interaction = UserInteractionsDb.InteractionType.valueOf(interactionType.toUpperCase());
            UserInteractionsDb.Discovery discovery = UserInteractionsDb.Discovery.valueOf(discoveryMethod.toUpperCase());
            var currentTime = testTimeProvider.nowMillis();
            UserInteractionsDb.UserInteractionRow interactionRow = new UserInteractionsDb.UserInteractionRow(
                    userId, contentId, discovery, interaction, currentTime
            );

            userTopicScoreAccumulatorService.accumulate(userId, interactionRow);
            userInteractionsDb.addInteraction(userId, contentId, discovery, interaction, currentTime);
            return ResponseEntity.ok(operationDetailMap.operationDetailMap);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error while processing interaction", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/api/advance-time")
    public ResponseEntity<String> advanceTime(
            @RequestParam(value = "days", defaultValue = "0") int days,
            @RequestParam(value = "hours", defaultValue = "0") int hours,
            @RequestParam(value = "minutes", defaultValue = "0") int minutes) {

        try {
            testTimeProvider.advanceDays(days);
            testTimeProvider.advanceHours(hours);
            testTimeProvider.advanceMinutes(minutes);
            return ResponseEntity.ok("Time advanced successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error advancing time");
        }
    }

    @GetMapping("/api/current-time")
    public ResponseEntity<String> getCurrentTime() {
        try {
            var currentTime = testTimeProvider.now();
            return ResponseEntity.ok(currentTime.toString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error getting current time");
        }
    }
}