package dev.kuku.interestcalculator;

import dev.kuku.interestcalculator.models.entities.ContentTopic;
import dev.kuku.interestcalculator.models.entities.PostEntity;
import dev.kuku.interestcalculator.models.entities.TopicRelationshipEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class Db {
    public final Set<String> topics = new HashSet<>();
    public final List<ContentTopic> contentTopics = new ArrayList<>();
    public final List<PostEntity> posts = new ArrayList<>();
    public final List<TopicRelationshipEntity> topicRelationships = new ArrayList<>();

}
