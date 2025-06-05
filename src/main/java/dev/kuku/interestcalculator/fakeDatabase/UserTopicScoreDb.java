package dev.kuku.interestcalculator.fakeDatabase;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class UserTopicScoreDb {


    private final List<UserTopicScoreRow> userTopicScores = new ArrayList<>();

    public List<UserTopicScoreRow> getTopicsOfUser(String userId) {
        return userTopicScores.stream().filter(
                t -> t.userId.equals(userId)
        ).toList();
    }

    @AllArgsConstructor
    public static class UserTopicScoreRow {
        public String userId;
        public String topic;
        public double interestScore;
        public double disinterestScore;
        public long updatedAt;
    }

}
