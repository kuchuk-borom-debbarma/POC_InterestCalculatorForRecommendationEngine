package dev.kuku.interestcalculator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LLMService {
    private final ChatModel chatModel;

    @Value("${topic.extraction.max-topics:3}")
    private int maxTopics;

    /**
     * Extracts relevant topics from content data by using an LLM.
     * This method prioritizes selecting from existing topics when possible,
     * only generating new topics when none of the existing ones are suitable.
     *
     * @param existingTopics List of existing topics to prioritize matching against
     * @param contentData    The content text to analyze
     * @return List of up to 3 topics that best describe the content
     */
    public List<String> getTopics(List<String> existingTopics, String contentData) {
        try {
            // Truncate content if too large to avoid token limits
            String truncatedContent = contentData.length() > 2000
                    ? contentData.substring(0, 2000) + "..."
                    : contentData;

            // System message defines the task and constraints
            Message systemMessage = new SystemMessage(
                    "You are a topic extraction system. " +
                            "Your task is to identify the most relevant topics for the given content. " +
                            "First, try to select from the provided list of existing topics. " +
                            "Only create new topics if none of the existing topics are sufficiently relevant. " +
                            "Return exactly " + maxTopics + " topics maximum, fewer if appropriate. " +
                            "Format your response as a comma-separated list of topics with no explanation or additional text."
            );

            // Format existing topics as a comma-separated string
            String existingTopicsString = String.join(", ", existingTopics);

            // User message provides the content and existing topics
            Message userMessage = new UserMessage(
                    "EXISTING TOPICS: " + existingTopicsString + "\n\n" +
                            "CONTENT: " + truncatedContent + "\n\n" +
                            "Based on the content above, provide the most relevant topics (maximum " + maxTopics +
                            "). Prefer existing topics when possible. Response format: topic1, topic2, topic3"
            );

            // Create prompt with messages and options, then send to LLM
            Prompt prompt = new Prompt(List.of(systemMessage, userMessage),
                    OllamaOptions.builder()
                            .temperature(0.2) // Lower temperature for more deterministic topic extraction
                            .build());
            
            ChatResponse response = chatModel.call(prompt);

            // Parse the response - use getText() method instead of getContent()
            String topicsResponse = response.getResult().getOutput().getText().trim();

            // Split by comma and clean up each topic
            List<String> extractedTopics = Arrays.stream(topicsResponse.split(","))
                    .map(String::trim)
                    .filter(topic -> !topic.isEmpty())
                    .limit(maxTopics)
                    .collect(Collectors.toList());

            log.info("Extracted topics: {}", extractedTopics);

            // Return extracted topics or empty list if none were found
            return extractedTopics.isEmpty() ? new ArrayList<>() : extractedTopics;

        } catch (Exception e) {
            log.error("Error extracting topics: {}", e.getMessage(), e);
            // In case of failure, return a single generic topic to avoid breaking the application
            return List.of("general");
        }
    }
}