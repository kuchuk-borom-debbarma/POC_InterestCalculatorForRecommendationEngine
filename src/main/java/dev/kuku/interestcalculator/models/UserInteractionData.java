package dev.kuku.interestcalculator.models;

import java.util.Set;

public record UserInteractionData(String userId, Set<InteractionData> interactionData) {
}
