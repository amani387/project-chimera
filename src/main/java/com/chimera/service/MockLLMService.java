package com.chimera.service;

import com.chimera.model.GenerationRequest;
import com.chimera.model.GenerationResult;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class MockLLMService implements LLMService {

    @Override
    public CompletableFuture<GenerationResult> generate(GenerationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate API delay
                Thread.sleep(100);

                String mockResponse = String.format(
                    "[MOCK] Generated for: %s\nSystem: %.30s...",
                    request.userPrompt(),
                    request.systemPrompt()
                );

                return new GenerationResult(
                    mockResponse,
                    "mock-model",
                    10,
                    20,
                    100,
                    "stop"
                );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });
    }
}
