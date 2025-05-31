package dev.kuku.interestcalculator.models.entities;

import dev.kuku.interestcalculator.models.InteractionType;

public record UserInteractionEntity(String userId, InteractionType interactionType, String interactionValue,
                                    String contentId, long timestamp) {
}
