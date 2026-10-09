package com.weatherwise.intelligence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class IntelligenceChatService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String apiKey;
    private final String model;

    public IntelligenceChatService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.ai.enabled:false}") boolean enabled,
            @Value("${app.ai.api-key:}") String apiKey,
            @Value("${app.ai.base-url:https://api.openai.com/v1/chat/completions}") String baseUrl,
            @Value("${app.ai.model:gpt-4o-mini}") String model
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.model = model;
    }

    public String answer(IntelligenceChatRequest request) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AI provider is not configured.");
        }

        String context = request.journeyContext() == null
                ? "No journey has been analyzed yet."
                : request.journeyContext();

        Map<String, Object> payload = Map.of(
                "model", model,
                "temperature", 0.2,
                "max_tokens", 450,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", "You are Roamora Intelligence, a concise and friendly travel-planning copilot. " +
                                        "Use only the journey context supplied by the application. Never invent live weather, routes, schedules, prices or safety guarantees. " +
                                        "If information is unavailable, say so. Explain trade-offs clearly and keep answers practical."
                        ),
                        Map.of(
                                "role", "user",
                                "content", "Journey context:\n" + context + "\n\nUser question:\n" + request.question()
                        )
                )
        );

        try {
            String response = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText("")
                    .trim();

            if (content.isBlank()) {
                throw new IllegalStateException("The AI provider returned an empty response.");
            }

            return content;
        } catch (RestClientException | IOException exception) {
            throw new IllegalStateException("The AI provider is temporarily unavailable.", exception);
        }
    }
}
