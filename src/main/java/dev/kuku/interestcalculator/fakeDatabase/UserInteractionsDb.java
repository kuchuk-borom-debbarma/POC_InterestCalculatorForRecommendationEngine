package dev.kuku.interestcalculator.fakeDatabase;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserInteractionsDb {
    private final List<UserInteractionRow> userInteractionRows = new ArrayList<>();
    private final ContentDb contentDb;

    public List<UserInteractionRow> getInteractionsOfUserFromTo(String userId, long from, long to) {
        List<UserInteractionRow> result = new ArrayList<>();
        for (UserInteractionRow interaction : userInteractionRows) {
            if (interaction.userId.equals(userId) && interaction.interactionTime >= from && interaction.interactionTime <= to) {
                result.add(interaction);
            }
        }
        return result;
    }

    public List<UserInteractionRow> getInteractionsOfUserFromTo(String userId, String topic, long from, long to) {
        return userInteractionRows.stream()
                .filter(interaction -> interaction.userId.equals(userId))
                .filter(interaction -> interaction.interactionTime >= from && interaction.interactionTime <= to)
                .filter(interaction -> contentDb.getContentById(interaction.contentId).getTopics().contains(topic))
                .map(interaction -> copyInteraction(interaction)) // Create copies
                .toList();
    }

    //create a function to add interaction to the database
    public void addInteraction(String userId, String contentId, Discovery contentDiscovery, InteractionType interactionType, long interactionTime) {
        userInteractionRows.add(new UserInteractionRow(userId, contentId, contentDiscovery, interactionType, interactionTime));
    }

    // Helper method to create a copy of UserInteractionRow
    private UserInteractionRow copyInteraction(UserInteractionRow original) {
        UserInteractionRow copy = new UserInteractionRow(original.userId, original.contentId, original.contentDiscovery, original.interactionType, original.interactionTime);
        copy.userId = original.userId;
        copy.contentId = original.contentId;
        copy.contentDiscovery = original.contentDiscovery;
        copy.interactionType = original.interactionType;
        copy.interactionTime = original.interactionTime;
        return copy;
    }

    public enum Discovery {
        TRENDING, RECOMMENDATION, SEARCH
    }

    public enum InteractionType {
        LIKE, DISLIKE, COMMENT, REPORT
    }

    @AllArgsConstructor
    @ToString
    public static class UserInteractionRow {
        public String userId;
        public String contentId;
        public Discovery contentDiscovery;
        public InteractionType interactionType;
        public long interactionTime;
    }
}