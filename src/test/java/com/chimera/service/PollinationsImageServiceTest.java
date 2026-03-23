package com.chimera.service;

import com.chimera.config.ImageProperties;
import com.chimera.model.ImageRequest;
import com.chimera.model.ImageResult;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PollinationsImageServiceTest {

  private MockWebServer mockWebServer;
  private PollinationsImageService service;
  private ImageProperties props;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    props = new ImageProperties();
    props.setWidth(512);
    props.setHeight(512);
    service = new PollinationsImageService(props);
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Test
  void generateImage_SuccessWithRedirect() {
    // Arrange
    String expectedPrompt = "test prompt";
    String expectedImageUrl = "https://image.pollinations.ai/generated/image.jpg";
    ImageRequest request = new ImageRequest(expectedPrompt, "", "", 512, 512);

    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(302)
        .addHeader("Location", expectedImageUrl));

    // Act
    ImageResult result = service.generateImage(request).join();

    // Assert
    assertEquals(expectedImageUrl, result.imageUrl());
    assertEquals(expectedPrompt, result.prompt());
    assertTrue(result.latencyMs() > 0);
    assertEquals("pollinations", result.metadata().get("provider"));
    assertEquals(512, result.metadata().get("width"));
    assertEquals(512, result.metadata().get("height"));
  }

  @Test
  void generateImage_ErrorResponse() {
    // Arrange
    ImageRequest request = new ImageRequest("test", "", "", 512, 512);

    mockWebServer.enqueue(new MockResponse().setResponseCode(500));

    // Act & Assert
    assertThrows(RuntimeException.class, () -> service.generateImage(request).join());
  }

  @Test
  void generateImage_NoLocationFallback() {
    // Arrange
    ImageRequest request = new ImageRequest("test prompt", "", "", 512, 512);

    mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(""));

    // Act
    ImageResult result = service.generateImage(request).join();

    // Assert
    assertTrue(result.imageUrl().startsWith("https://image.pollinations.ai/prompt/"));
    // other asserts...
  }
}

