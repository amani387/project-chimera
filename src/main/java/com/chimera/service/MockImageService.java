package com.chimera.service;

import com.chimera.model.ImageRequest;
import com.chimera.model.ImageResult;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MockImageService implements ImageGenerationService {

  @Override
  public CompletableFuture<ImageResult> generateImage(ImageRequest request) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        Thread.sleep(500); // Simulate latency
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
      var start = Instant.now();
      var imageUrl = String.format("https://via.placeholder.com/%dx%d?text=%s", request.width(), request.height(), 
                     java.net.URLEncoder.encode(request.prompt().substring(0, Math.min(20, request.prompt().length())), java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"));
      var latency = Duration.between(start, Instant.now()).toMillis();
      var metadata = Map.<String, Object>of("model", "mock", "size", request.width() + "x" + request.height());
      return new ImageResult(imageUrl, request.prompt(), latency, metadata);
    });
  }
}
