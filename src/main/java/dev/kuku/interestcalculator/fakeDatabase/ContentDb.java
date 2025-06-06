package dev.kuku.interestcalculator.fakeDatabase;

import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class ContentDb {
    private final Map<String, ContentRow> contentTopicRows = new HashMap<>();

    public Set<String> getTopicsOfContent(String contentId) {
        return contentTopicRows.get(contentId).topics;
    }

    public ContentRow getContentById(String contentId) {
        return contentTopicRows.get(contentId);
    }

    public static record ContentRow(String contentId, String content, Set<String> topics, String userId,
                                    long timestamp) {
    }
}
