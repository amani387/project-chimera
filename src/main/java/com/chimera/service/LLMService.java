package com.chimera.service;

import com.chimera.model.GenerationRequest;
import com.chimera.model.GenerationResult;
import java.util.concurrent.CompletableFuture;

public interface LLMService {
    CompletableFuture<GenerationResult> generate(GenerationRequest request);
}
