package com.chimera.worker;

import com.chimera.mcp.WeaviateMcpClient;
import com.chimera.model.MemoryEntry;
import com.chimera.model.Task;
import com.chimera.model.WorkerResult;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Worker service that consumes tasks from Redis, processes them, and publishes results.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "chimera.worker.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(name = "chimera.worker.mcp.enabled", havingValue = "false", matchIfMissing = true)
public class WorkerService {

  private static final String TASK_QUEUE = "task:queue:test-agent-001";
  private static final String REVIEW_QUEUE = "review:queue:test-agent-001";
  private static final String WORKER_ID = "worker-001";

  private final RedisTemplate<String, Task> taskRedisTemplate;
  private final RedisTemplate<String, WorkerResult> resultRedisTemplate;
  private final ExecutorService executor;

  // Optional: only provided when MCP memory is enabled.
  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private WeaviateMcpClient memoryClient;

  private volatile boolean running = true;
  private Future<?> listenerFuture;

  public WorkerService(
      @Qualifier("taskRedisTemplate") RedisTemplate<String, Task> taskRedisTemplate,
      @Qualifier("resultRedisTemplate") RedisTemplate<String, WorkerResult> resultRedisTemplate,
      ExecutorService workerExecutor) {
    this.taskRedisTemplate = taskRedisTemplate;
    this.resultRedisTemplate = resultRedisTemplate;
    this.executor = workerExecutor;
  }

  // Exposed for tests or manual wiring.
  public void setMemoryClient(WeaviateMcpClient memoryClient) {
    this.memoryClient = memoryClient;
  }

  @PostConstruct
  public void start() {
    listenerFuture = executor.submit(() -> {
      log.info("Worker loop started");
      while (running && !Thread.currentThread().isInterrupted()) {
        try {
          Task task = taskRedisTemplate.opsForList().rightPop(TASK_QUEUE, 1, TimeUnit.SECONDS);
          if (task != null) {
            executor.submit(() -> processTask(task));
          }
        } catch (IllegalStateException e) {
          // Connection factory may have been stopping/stopped during shutdown.
          if (e.getMessage() != null && e.getMessage().toUpperCase().contains("STOPP")) {
            log.debug("Redis connection factory stopping/stopped; exiting worker loop");
            break;
          }
          log.error("Error in worker loop", e);
        } catch (Exception e) {
          log.error("Error in worker loop", e);
        }
      }
      log.info("Worker loop stopped");
    });
  }

  @PreDestroy
  public void stop() {
    running = false;
    if (listenerFuture != null) {
      listenerFuture.cancel(true);
    }
    executor.shutdown();
    try {
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  void processTask(Task task) {
    var start = Instant.now();
    log.info("Processing task {} (type={})", task.taskId(), task.taskType());

    WorkerResult result;
    try {
      result = switch (task.taskType()) {
        case "generate_content" -> processGenerateContent(task, start);
        case "store_memory" -> processStoreMemory(task, start);
        case "search_memory" -> processSearchMemory(task, start);
        default -> {
          String message = "Unsupported task type: " + task.taskType();
          log.warn(message);
          yield buildFailureResult(task, message);
        }
      };
    } catch (Exception e) {
      log.error("Failed to process task {}", task.taskId(), e);
      result = buildFailureResult(task, e.getMessage());
    }

    resultRedisTemplate.opsForList().leftPush(REVIEW_QUEUE, result);
    log.info("Published result for task {} to review queue", task.taskId());
  }

  private WorkerResult processGenerateContent(Task task, Instant start) throws InterruptedException {
    // Simulate work
    var sleepMs = 100 + ThreadLocalRandom.current().nextInt(400);
    try {
      Thread.sleep(sleepMs);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw e;
    }

    double confidence = ThreadLocalRandom.current().nextDouble(0.7, 1.0);
    var output = new WorkerResult.WorkerOutput(
        "image",
        "https://example.com/generated-image-" + java.util.UUID.randomUUID(),
        List.of()
    );
    return new WorkerResult(
        task.taskId(),
        WORKER_ID,
        output,
        confidence,
        "Generated mock content for task: " + task.taskId(),
        List.of(),
        Duration.between(start, Instant.now()).toMillis(),
        Map.of()
    );
  }

  private WorkerResult processStoreMemory(Task task, Instant start) {
    if (memoryClient == null) {
      String message = "Memory client not configured";
      log.warn(message);
      return buildFailureResult(task, message);
    }

    var params = task.parameters();
    var content = (String) params.getOrDefault("content", "");
    @SuppressWarnings("unchecked")
    var metadata = (Map<String, Object>) params.getOrDefault("metadata", Map.of());

    try {
      memoryClient.storeMemory("test-agent-001", content, metadata).join();
      var output = new WorkerResult.WorkerOutput(
          "memory_store",
          "stored",
          List.of()
      );
      return new WorkerResult(
          task.taskId(),
          WORKER_ID,
          output,
          1.0,
          "Stored memory successfully",
          List.of(),
          Duration.between(start, Instant.now()).toMillis(),
          Map.of("stored", true)
      );
    } catch (Exception e) {
      log.error("Failed to store memory", e);
      return buildFailureResult(task, "Failed to store memory: " + e.getMessage());
    }
  }

  private WorkerResult processSearchMemory(Task task, Instant start) {
    if (memoryClient == null) {
      String message = "Memory client not configured";
      log.warn(message);
      return buildFailureResult(task, message);
    }

    var params = task.parameters();
    var query = (String) params.getOrDefault("query", "");
    var limit = (int) params.getOrDefault("limit", 5);

    try {
      List<MemoryEntry> memories = memoryClient.searchMemories("test-agent-001", query, limit).join();
      var output = new WorkerResult.WorkerOutput(
          "memory_search",
          "found " + memories.size() + " entries",
          List.of()
      );
      return new WorkerResult(
          task.taskId(),
          WORKER_ID,
          output,
          1.0,
          "Retrieved memories",
          List.of(),
          Duration.between(start, Instant.now()).toMillis(),
          Map.of("memories", memories)
      );
    } catch (Exception e) {
      log.error("Failed to search memory", e);
      return buildFailureResult(task, "Failed to search memory: " + e.getMessage());
    }
  }

  private WorkerResult buildFailureResult(Task task, String errorMessage) {
    var output = new WorkerResult.WorkerOutput("error", "", List.of());
    return new WorkerResult(
        task.taskId(),
        WORKER_ID,
        output,
        0.0,
        "Task processing failed: " + errorMessage,
        List.of(),
        0,
        Map.of("error", errorMessage)
    );
  }
}
