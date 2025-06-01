package dev.kuku.interestcalculator.models;

import java.util.List;
import java.util.Objects;

public record UserInteractionData(String userId, List<InteractionData> interactionData) {
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserInteractionData that = (UserInteractionData) o;
        return Objects.equals(userId, that.userId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}