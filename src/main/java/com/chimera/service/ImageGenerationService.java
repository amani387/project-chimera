package com.chimera.service;

import com.chimera.model.ImageRequest;
import com.chimera.model.ImageResult;
import java.util.concurrent.CompletableFuture;

public interface ImageGenerationService {
  CompletableFuture<ImageResult> generateImage(ImageRequest request);
}
