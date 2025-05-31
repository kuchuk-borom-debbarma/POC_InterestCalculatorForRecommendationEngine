package dev.kuku.interestcalculator.services.userInterest;

import dev.kuku.interestcalculator.models.UserInteractionData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserInterestService {
    private final UserInterestRepository userInterestRepository;

    public void calculateUserInterestScore(Set<UserInteractionData>) {

    }
}
