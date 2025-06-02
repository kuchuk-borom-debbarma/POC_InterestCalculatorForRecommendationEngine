package dev.kuku.interestcalculator.processors;

import dev.kuku.interestcalculator.models.InteractionData;

import java.util.List;

public abstract class UserInterestScoreProcessorBase {
    public abstract void process(String userId, List<InteractionData> userInteractions);
}
