package dev.kuku.interestcalculator.services;

import dev.kuku.interestcalculator.fakeDatabase.UserInteractionsDb;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 *
 */
@Service
@RequiredArgsConstructor
public class UserTopicScoreAccumulator {

    private double getContentDiscoveryMultiplier(UserInteractionsDb.Discovery discovery) {
        return 0.0;
    }
}
