package dev.kuku.interestcalculator.repo;

import dev.kuku.interestcalculator.models.entities.TopicRelationshipEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TopicRelationshipRepo {
    private List<TopicRelationshipEntity> topicRelationshipEntities = List.of();
}
