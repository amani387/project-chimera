package com.chimera.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import com.chimera.model.ImageRequest;
import com.chimera.model.ImageResult;
import com.chimera.model.Task;
import com.chimera.model.WorkerResult;
import com.chimera.service.ImageGenerationService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkerServiceImageTest {

  @Mock
  private ImageGenerationService imageService;

  private WorkerService workerService;

  @BeforeEach
  void setUp() {
    workerService = new WorkerService(null, null, null);
  }

  @Test
  void processGenerateImage_success() throws Exception {
    Field imageField = WorkerService.class.getDeclaredField("imageService");
    imageField.setAccessible(true);
    imageField.set(workerService, imageService);

    Map<String, Object> params = Map.of("prompt", "test image", "width", 1024, "height", 1024);
    Task task = new Task("test-task-id", "generate_image", "medium", null, null, null, Instant.now(), "pending", params);

    ImageResult imageResult = new ImageResult("https://test.image/url.jpg", "test image", 500L, Map.of("provider", "pollinations"));
    when(imageService.generateImage(any(ImageRequest.class))).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(imageResult));

    Instant start = Instant.now();
    Method method = WorkerService.class.getDeclaredMethod("processGenerateImage", Task.class, Instant.class);
    method.setAccessible(true);
    WorkerResult result = (WorkerResult) method.invoke(workerService, task, start);

    assertThat(result.taskId()).isEqualTo("test-task-id");
    assertThat(result.workerId()).isEqualTo("worker-001");
    assertThat(result.output().contentType()).isEqualTo("image");
    assertThat(result.output().content()).isEqualTo("https://test.image/url.jpg");
    assertThat(result.confidenceScore()).isEqualTo(1.0);
    assertThat(result.metadata()).containsEntry("provider", "pollinations");

  }

  @Test
  void processGenerateImage_noService() throws Exception {
    Map<String, Object> params = Map.of();
    Task task = new Task("test-task-id", "generate_image", "medium", null, null, null, Instant.now(), "pending", params);

    Instant start = Instant.now();
    Method method = WorkerService.class.getDeclaredMethod("processGenerateImage", Task.class, Instant.class);
    method.setAccessible(true);
    WorkerResult result = (WorkerResult) method.invoke(workerService, task, start);

    assertThat(result.confidenceScore()).isEqualTo(0.0);
    assertThat(result.reasoningTrace()).contains("Image service not configured");
  }


  @Test
  void processGenerateImage_serviceError() throws Exception {
    Field imageField = WorkerService.class.getDeclaredField("imageService");
    imageField.setAccessible(true);
    imageField.set(workerService, imageService);

    Map<String, Object> params = Map.of("prompt", "error prompt");
    Task task = new Task("test-task-id", "generate_image", "medium", null, null, null, Instant.now(), "pending", params);

    when(imageService.generateImage(any(ImageRequest.class))).thenThrow(new RuntimeException("Service error"));

    Instant start = Instant.now();
    Method method = WorkerService.class.getDeclaredMethod("processGenerateImage", Task.class, Instant.class);
    method.setAccessible(true);
    WorkerResult result = (WorkerResult) method.invoke(workerService, task, start);

    assertThat(result.confidenceScore()).isEqualTo(0.0);
    assertThat(result.reasoningTrace()).contains("Image generation failed");
  }
}


