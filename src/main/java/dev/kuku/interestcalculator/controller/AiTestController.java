package dev.kuku.interestcalculator.controller;

import dev.kuku.interestcalculator.services.LLMService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class AiTestController {
    private final LLMService llmService;

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> test(
            @RequestParam(required = false, defaultValue = "") String existingTopics,
            @RequestParam String content) {

        // Parse existing topics from comma-separated string
        List<String> topicsList = existingTopics.isEmpty() ?
                List.of() :
                Arrays.stream(existingTopics.split(","))
                        .map(String::trim)
                        .filter(t -> !t.isEmpty())
                        .collect(Collectors.toList());

        // Call LLM service to extract topics
        return llmService.getTopics(topicsList, content);
    }
}