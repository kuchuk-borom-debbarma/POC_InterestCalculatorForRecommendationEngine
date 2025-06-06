package dev.kuku.interestcalculator.controller;

import dev.kuku.interestcalculator.services.LLMService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class AiTestController {
    private final LLMService llmService;

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public Set<String> test(
            @RequestParam(required = false, defaultValue = "") String existingTopics,
            @RequestParam String content) {

        // Parse existing topics from comma-separated string
        Set<String> topicsList = existingTopics.isEmpty() ?
                Set.of() :
                Arrays.stream(existingTopics.split(","))
                        .map(String::trim)
                        .filter(t -> !t.isEmpty())
                        .collect(Collectors.toSet());

        // Call LLM service to extract topics
        return llmService.getTopics(topicsList, content);
    }
}