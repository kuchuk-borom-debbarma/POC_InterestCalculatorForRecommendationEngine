package dev.kuku.interestcalculator.fakeDatabase;

import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Set;

@Repository
public class TopicDb {
    public Set<String> topics = new HashSet<>();
}
