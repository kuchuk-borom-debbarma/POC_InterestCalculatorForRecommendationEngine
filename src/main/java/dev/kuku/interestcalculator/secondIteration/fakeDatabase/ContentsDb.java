package dev.kuku.interestcalculator.secondIteration.fakeDatabase;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class ContentsDb {
    public static class ContentRow {
        String userId;
        String contentId;
        String content;
    }

    private final List<ContentRow> contentRows = new ArrayList<>();
}
