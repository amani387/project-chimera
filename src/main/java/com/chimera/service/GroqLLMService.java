package com.chimera.service;

import com.chimera.config.LLMProperties;
import com.chimera.model.GenerationRequest;
import com.chimera.model.GenerationResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class GroqLLMService implements LLMService {

    private final WebClient webClient;
    private final LLMProperties properties;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public GroqLLMService(LLMProperties properties) {
        this.properties = properties;

        this.webClient = WebClient.builder()
            .baseUrl(properties.getBaseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

        log.info("GroqLLMService initialized with model: {}", properties.getModel());
    }

    @Override
    public CompletableFuture<GenerationResult> generate(GenerationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();

            try {
                GroqChatCompletionRequest completionRequest = new GroqChatCompletionRequest(
                    properties.getModel(),
                    List.of(
                        new GroqMessage("system", request.systemPrompt()),
                        new GroqMessage("user", request.userPrompt())
                    ),
                    properties.getTemperature(),
                    properties.getMaxTokens()
                );

                GroqChatCompletionResponse response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(completionRequest)
                    .retrieve()
                    .bodyToMono(GroqChatCompletionResponse.class)
                    .block(Duration.ofSeconds(30));

                if (response == null || response.choices() == null || response.choices().isEmpty()) {
                    throw new IllegalStateException("Received empty response from Groq LLM");
                }

                GroqChoice firstChoice = response.choices().get(0);
                String generatedText = firstChoice.message().content();
                String finishReason = firstChoice.finishReason();

                int promptTokens = response.usage() != null ? response.usage().promptTokens() : 0;
                int completionTokens = response.usage() != null ? response.usage().completionTokens() : 0;
                long latency = System.currentTimeMillis() - startTime;

                log.debug("Generation completed: {} tokens in {}ms", promptTokens + completionTokens, latency);

                return new GenerationResult(
                    generatedText,
                    properties.getModel(),
                    promptTokens,
                    completionTokens,
                    latency,
                    finishReason
                );
            } catch (Exception e) {
                log.error("Groq LLM call failed", e);
                throw new RuntimeException("Failed to generate text from Groq", e);
            }
        }, executor);
    }

    private record GroqChatCompletionRequest(
        String model,
        List<GroqMessage> messages,
        Double temperature,
        @JsonProperty("max_tokens") Integer maxTokens
    ) {}

    private record GroqMessage(String role, String content) {}

    private record GroqChatCompletionResponse(
        List<GroqChoice> choices,
        GroqUsage usage
    ) {}

    private record GroqChoice(
        GroqMessage message,
        @JsonProperty("finish_reason") String finishReason
    ) {}

    private record GroqUsage(
        @JsonProperty("prompt_tokens") int promptTokens,
        @JsonProperty("completion_tokens") int completionTokens,
        @JsonProperty("total_tokens") int totalTokens
    ) {}
}
