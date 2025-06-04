package dev.kuku.interestcalculator.secondIteration.fakeDatabase;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class UserInteractionsDb {
    public enum Discovery {
        TRENDING, RECOMMENDATION, SEARCH
    }

    public enum InteractionType {
        LIKE, DISLIKE, COMMENT, REPORT
    }

    public static class UserInteractionRow {
        public String userId;
        public String contentId;
        public Discovery contentDiscovery;
        public InteractionType interactionType;
    }

    private final List<UserInteractionRow> userInteractionRows = new ArrayList<>();
}
