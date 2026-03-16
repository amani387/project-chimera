package com.chimera.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.chimera.model.Task;
import com.chimera.model.WorkerResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
    "chimera.redis.enabled=false",
    "chimera.planner.enabled=false",
    "chimera.worker.enabled=true"
})
class WorkerIntegrationTest {

  @Autowired
  private WorkerService workerService;

  @Autowired
  private List<WorkerResult> capturedResults;

  @Autowired
  private List<Task> publishedTasks;

  @Autowired
  private ApplicationContext context;

  @Test
  void workerShouldProcessTaskFromQueueAndPublishResult() throws Exception {
    assertThat(publishedTasks).isNotEmpty();

    // Wait for worker to process the task
    boolean processed = waitForResults(1, 5, TimeUnit.SECONDS);

    assertThat(processed).isTrue();
    assertThat(capturedResults).isNotEmpty();
    assertThat(capturedResults.get(0).taskId()).isEqualTo(publishedTasks.get(0).taskId());
  }

  private boolean waitForResults(int expectedSize, long timeout, TimeUnit unit) throws InterruptedException {
    long deadline = System.nanoTime() + unit.toNanos(timeout);
    while (System.nanoTime() < deadline) {
      if (capturedResults.size() >= expectedSize) {
        return true;
      }
      Thread.sleep(50);
    }
    return false;
  }

  @TestConfiguration
  static class WorkerTestConfig {

    @Bean
    public List<Task> publishedTasks() {
      return new java.util.concurrent.CopyOnWriteArrayList<>();
    }

    @Bean
    public List<WorkerResult> capturedResults() {
      return new java.util.concurrent.CopyOnWriteArrayList<>();
    }

    @Bean
    public RedisTemplate<String, Task> taskRedisTemplate(List<Task> publishedTasks) {
      var listOps = org.mockito.Mockito.mock(ListOperations.class);
      var task = Task.pending("Test task for worker");
      publishedTasks.add(task);

      when(listOps.rightPop("task:queue:test-agent-001", 1, TimeUnit.SECONDS))
          .thenReturn(task)
          .thenReturn(null);

      var template = org.mockito.Mockito.mock(RedisTemplate.class);
      when(template.opsForList()).thenReturn(listOps);
      return template;
    }

    @Bean
    public RedisTemplate<String, WorkerResult> resultRedisTemplate(List<WorkerResult> capturedResults) {
      var listOps = org.mockito.Mockito.mock(ListOperations.class);
      when(listOps.leftPush(eq("review:queue:test-agent-001"), any(WorkerResult.class)))
          .thenAnswer(invocation -> {
            capturedResults.add(invocation.getArgument(1));
            return (long) capturedResults.size();
          });

      var template = org.mockito.Mockito.mock(RedisTemplate.class);
      when(template.opsForList()).thenReturn(listOps);
      return template;
    }
  }
}
