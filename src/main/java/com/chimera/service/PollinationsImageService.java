package com.chimera.service;

import com.chimera.model.ImageRequest;
import com.chimera.model.ImageResult;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import com.chimera.config.ImageProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PollinationsImageService implements ImageGenerationService {

  private final WebClient webClient;

  public PollinationsImageService() {
    this(WebClient.builder()
        .baseUrl("https://image.pollinations.ai")
        .build());
  }

  public PollinationsImageService(WebClient webClient) {
    this.webClient = webClient;
  }

public PollinationsImageService(ImageProperties props) {
    this.webClient = WebClient.builder()
        .baseUrl("https://image.pollinations.ai")
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(props.getWidth() * props.getHeight() * 4)) // Rough bytes estimate
        .build();
  }

  @Override
  public java.util.concurrent.CompletableFuture<ImageResult> generateImage(ImageRequest request) {
    var start = Instant.now();
    var encodedPrompt = URLEncoder.encode(request.prompt(), StandardCharsets.UTF_8);
    var uri = UriComponentsBuilder.fromPath("/prompt/" + encodedPrompt)
        .queryParam("width", request.width())
        .queryParam("height", request.height())
        .queryParam("nologo", true)
        .queryParam("seed", java.util.UUID.randomUUID().toString().hashCode())
        .build()
        .toUriString();

    return webClient.get()
        .uri(uri)
        .retrieve()
        .toBodilessEntity()
        .map(response -> {
          var latency = Duration.between(start, Instant.now()).toMillis();
          var location = response.getHeaders().getFirst("Location");
          var imageUrl = location != null ? location : "https://image.pollinations.ai" + uri;
          var metadata = Map.<String, Object>of("provider", "pollinations", "width", request.width(), "height", request.height());
          log.info("Generated image: {} (latency: {}ms)", imageUrl, latency);
          return new ImageResult(imageUrl, request.prompt(), latency, metadata);
        })
        .onErrorMap(e -> new RuntimeException("Image generation failed: " + e.getMessage(), e))
        .toFuture();
  }
}

