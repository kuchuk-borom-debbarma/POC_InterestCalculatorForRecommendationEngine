package dev.kuku.interestcalculator.services.ai;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LLMService {
    /**
     * get topics mapped to contentId
     * @param existingTopics existing topics to choose from in valid
     * @param contents key-> contentId, value -> content data
     * @return map of contentId and set of topics. It can be existing or new topic.
     */
    public Map<String, Set<String>> getTopics(List<String> existingTopics, Map<String,String> contents) {

    }
}
