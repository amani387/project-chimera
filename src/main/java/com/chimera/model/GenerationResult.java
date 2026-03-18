package com.chimera.model;

public record GenerationResult(
    String text,
    String model,
    int promptTokens,
    int completionTokens,
    long latencyMs,
    String finishReason
) {}
