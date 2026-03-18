package com.chimera.model;

import java.util.Map;

public record GenerationRequest(
    String systemPrompt,
    String userPrompt,
    Map<String, Object> parameters
) {}
