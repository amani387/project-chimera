package com.chimera.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.chimera.model.Task;
import com.chimera.model.WorkerResult;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
    "chimera.redis.enabled=false",
    "chimera.planner.enabled=false",
    "chimera.worker.enabled=true",
    "chimera.worker.mcp.enabled=true",
    "chimera.weaviate.mock=true"
})
class McpMemoryIntegrationTest {

  @Autowired
  private List<WorkerResult> capturedResults;

  @Autowired
  private List<Task> publishedTasks;

  @Test
  void workerShouldStoreAndSearchMemory() throws Exception {
    assertThat(publishedTasks).hasSize(2);

    boolean processed = waitForResults(2, 5, TimeUnit.SECONDS);

    assertThat(processed).isTrue();
    assertThat(capturedResults).hasSize(2);

    assertThat(capturedResults).anySatisfy(result ->
        assertThat(result.output().contentType()).isEqualTo("memory_store"));

    assertThat(capturedResults).anySatisfy(result -> {
      assertThat(result.output().contentType()).isEqualTo("memory_search");
      assertThat(result.metadata()).containsKey("memories");
    });
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

      var storeTask = Task.storeMemory("hello world", Map.of("tag", "greeting"));
      var searchTask = Task.searchMemory("hello", 10);
      publishedTasks.add(storeTask);
      publishedTasks.add(searchTask);

      when(listOps.rightPop("task:queue:test-agent-001", 1, TimeUnit.SECONDS))
          .thenReturn(storeTask)
          .thenReturn(searchTask)
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
