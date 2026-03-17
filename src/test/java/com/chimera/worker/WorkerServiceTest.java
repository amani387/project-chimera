package com.chimera.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chimera.model.Task;
import com.chimera.model.WorkerResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
class WorkerServiceTest {

  @Mock
  private RedisTemplate<String, Task> taskRedisTemplate;

  @Mock
  private RedisTemplate<String, WorkerResult> resultRedisTemplate;

  @Mock
  private ListOperations<String, WorkerResult> resultListOperations;

  @Captor
  private ArgumentCaptor<WorkerResult> resultCaptor;

  private static ExecutorService executor;

  @BeforeAll
  static void beforeAll() {
    executor = Executors.newVirtualThreadPerTaskExecutor();
  }

  @AfterAll
  static void afterAll() {
    executor.shutdownNow();
  }

  @Test
  void processTask_shouldPushWorkerResultToReviewQueue() {
    when(resultRedisTemplate.opsForList()).thenReturn(resultListOperations);

    var worker = new WorkerService(taskRedisTemplate, resultRedisTemplate, executor);
    var task = Task.pending("Generate a test image");
    worker.processTask(task);

    verify(resultListOperations).leftPush(eq("review:queue:test-agent-001"), resultCaptor.capture());
    var result = resultCaptor.getValue();

    assertThat(result.taskId()).isEqualTo(task.taskId());
    assertThat(result.confidenceScore()).isBetween(0.7, 1.0);
    assertThat(result.output().content()).contains("https://example.com/generated-image-");
  }

  @Test
  void processTask_withUnsupportedTaskType_shouldPushFailureResult() {
    when(resultRedisTemplate.opsForList()).thenReturn(resultListOperations);

    var worker = new WorkerService(taskRedisTemplate, resultRedisTemplate, executor);
    var task = new Task(
        "task-x",
        "unknown_type",
        "low",
        new Task.Context("ignore", List.of(), List.of(), List.of()),
        null,
        null,
        Instant.now(),
        "pending",
        Map.of()
    );

    worker.processTask(task);

    verify(resultListOperations).leftPush(eq("review:queue:test-agent-001"), resultCaptor.capture());
    var result = resultCaptor.getValue();
    assertThat(result.taskId()).isEqualTo(task.taskId());
    assertThat(result.confidenceScore()).isEqualTo(0.0);
    assertThat(result.metadata()).containsKey("error");
  }

  @Test
  void processTask_storeMemory_invokesMemoryClientAndPublishesResult() {
    when(resultRedisTemplate.opsForList()).thenReturn(resultListOperations);

    var memoryClient = org.mockito.Mockito.mock(com.chimera.mcp.WeaviateMcpClient.class);
    when(memoryClient.storeMemory("test-agent-001", "hello", Map.of("foo", "bar")))
        .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

    var worker = new WorkerService(taskRedisTemplate, resultRedisTemplate, executor);
    worker.setMemoryClient(memoryClient);
    var task = Task.storeMemory("hello", Map.of("foo", "bar"));

    worker.processTask(task);

    verify(memoryClient).storeMemory("test-agent-001", "hello", Map.of("foo", "bar"));
    verify(resultListOperations).leftPush(eq("review:queue:test-agent-001"), resultCaptor.capture());
    var result = resultCaptor.getValue();
    assertThat(result.output().contentType()).isEqualTo("memory_store");
    assertThat(result.metadata()).containsEntry("stored", true);
  }

  @Test
  void processTask_searchMemory_returnsRetrievedMemories() {
    when(resultRedisTemplate.opsForList()).thenReturn(resultListOperations);

    var memoryEntry = new com.chimera.model.MemoryEntry(
        "id-1",
        "test-agent-001",
        "semantic",
        "hello world",
        List.of(),
        Map.of(),
        Instant.now(),
        0,
        Instant.now()
    );
    var memoryClient = org.mockito.Mockito.mock(com.chimera.mcp.WeaviateMcpClient.class);
    when(memoryClient.searchMemories("test-agent-001", "hello", 5))
        .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(List.of(memoryEntry)));

    var worker = new WorkerService(taskRedisTemplate, resultRedisTemplate, executor);
    worker.setMemoryClient(memoryClient);
    var task = Task.searchMemory("hello", 5);

    worker.processTask(task);

    verify(memoryClient).searchMemories("test-agent-001", "hello", 5);
    verify(resultListOperations).leftPush(eq("review:queue:test-agent-001"), resultCaptor.capture());
    var result = resultCaptor.getValue();
    assertThat(result.output().contentType()).isEqualTo("memory_search");
    assertThat(result.metadata()).containsKey("memories");
  }
}
