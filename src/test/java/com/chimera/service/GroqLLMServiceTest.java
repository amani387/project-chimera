package com.chimera.service;

import com.chimera.config.LLMProperties;
import com.chimera.model.GenerationRequest;
import com.chimera.model.GenerationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "GROQ_API_KEY", matches = ".+")
public class GroqLLMServiceTest {

    @Test
    void testRealGroqGeneration() {
        // Setup real properties – note the "/v1" in baseUrl
        LLMProperties properties = new LLMProperties();
        properties.setBaseUrl("https://api.groq.com/openai/v1");  // ← FIXED
        properties.setApiKey(System.getenv("GROQ_API_KEY"));
        properties.setModel("llama-3.3-70b-versatile");
        properties.setMaxTokens(100);
        properties.setTemperature(0.7);

        GroqLLMService service = new GroqLLMService(properties);

        GenerationRequest request = new GenerationRequest(
            "You are a helpful assistant.",
            "Say hello in one sentence",
            Map.of("temperature", 0.7)
        );

        GenerationResult result = service.generate(request).join();

        assertThat(result.text()).isNotEmpty();
        assertThat(result.finishReason()).isEqualTo("stop");
        assertThat(result.promptTokens()).isGreaterThan(0);
        assertThat(result.completionTokens()).isGreaterThan(0);

        System.out.println("Generated: " + result.text());
        System.out.println("Tokens: " + result.promptTokens() + " prompt + " + result.completionTokens() + " completion");
    }
}