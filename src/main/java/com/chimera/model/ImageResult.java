package com.chimera.model;

import java.util.Map;

public record ImageResult(
    String imageUrl,
    String prompt,
    long latencyMs,
    Map<String, Object> metadata
) {}
