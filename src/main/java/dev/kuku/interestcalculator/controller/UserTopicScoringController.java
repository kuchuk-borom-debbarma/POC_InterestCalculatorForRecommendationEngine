package dev.kuku.interestcalculator.controller;

import dev.kuku.interestcalculator.fakeDatabase.UserInteractionsDb;
import dev.kuku.interestcalculator.services.userTopicScoreAccumulator.UserTopicScoreAccumulatorService;
import jakarta.websocket.server.PathParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserTopicScoringController {
    private final UserTopicScoreAccumulatorService userTopicScoreAccumulatorService;

    @PostMapping("/interact")
    public Object interact(@PathParam("interactionType") UserInteractionsDb.InteractionType interactionType,
                           @PathParam("contentId") String contentId,
                           @PathParam("discoveryMethod") UserInteractionsDb.Discovery discovery, @PathParam("time") long time) {
        userTopicScoreAccumulatorService.accumulate("123", new UserInteractionsDb.UserInteractionRow("123", contentId, discovery, interactionType, time));
        return null;
    }
}
