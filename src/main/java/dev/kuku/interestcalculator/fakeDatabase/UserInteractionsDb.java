package dev.kuku.interestcalculator.fakeDatabase;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Repository
public class UserInteractionsDb {
    private final List<UserInteractionRow> userInteractionRows = new ArrayList<>();
    public List<UserInteractionRow> getInteractionsOfUserFromTo(String userId, long from, long to) {
        List<UserInteractionRow> result = new ArrayList<>();
        for (UserInteractionRow interaction : userInteractionRows) {
            if (interaction.userId.equals(userId) && interaction.interactionTime >= from && interaction.interactionTime <= to) {
                result.add(interaction);
            }
        }
        return result;
    }

    public List<UserInteractionRow> getInteractionsOfUserFromTo(String userId, Set<String> topics, long from, long to) {
        List<UserInteractionRow> result = new ArrayList<>();
        for (UserInteractionRow interaction : userInteractionRows) {
            topics =
            if (interaction.userId.equals(userId) &&
                    topics.contains(interaction.topicId) &&
                    interaction.interactionTime >= from &&
                    interaction.interactionTime <= to) {
                result.add(interaction);
            }
        }
        return result;
    }

    public enum Discovery {
        TRENDING, RECOMMENDATION, SEARCH
    }

    public enum InteractionType {
        LIKE, COMMENT, REPORT
    }

    public static class UserInteractionRow {
        public String userId;
        public String contentId;
        public Discovery contentDiscovery;
        public InteractionType interactionType;
        public long interactionTime;
    }
}
