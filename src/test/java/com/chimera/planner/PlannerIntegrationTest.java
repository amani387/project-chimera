package com.chimera.planner;

import static org.assertj.core.api.Assertions.assertThat;

import com.chimera.model.JudgeVerdict;
import com.chimera.model.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
    "chimera.planner.enabled=true",
    "chimera.redis.enabled=false",
    "chimera.worker.enabled=false"
})
class PlannerIntegrationTest {

  @Autowired
  private List<Task> capturedTasks;

  @Test
  void plannerShouldPushTasksIntoRedisQueueAndReplanOnJudgeFeedback() throws Exception {
    assertThat(capturedTasks).isNotNull().isNotEmpty();
    assertThat(capturedTasks.get(0).status()).isEqualTo("pending");
    assertThat(capturedTasks.get(0).context().goalDescription()).isNotBlank();

    boolean replanned = waitForReplannedTask(4, 2, TimeUnit.SECONDS);
    assertThat(replanned).isTrue();

    assertThat(capturedTasks).anySatisfy(task ->
        assertThat(task.context().goalDescription()).contains("Please add more context"));

    // Ensure we don’t exceed max retries (MAX_RETRY_ATTEMPTS=2) per task
    assertThat(capturedTasks.size()).isLessThanOrEqualTo(3 + 2);
  }

  private boolean waitForReplannedTask(int expectedSize, long timeout, TimeUnit unit) throws InterruptedException {
    long deadline = System.nanoTime() + unit.toNanos(timeout);
    while (System.nanoTime() < deadline) {
      if (capturedTasks.size() >= expectedSize) {
        return true;
      }
      Thread.sleep(50);
    }
    return false;
  }

  @TestConfiguration
  static class RedisTestConfig {

    @Bean
    public List<Task> capturedTasks() {
      return new ArrayList<>();
    }

    @Bean
    public List<JudgeVerdict> incomingVerdicts() {
      return new ArrayList<>();
    }

    @Bean
    public RedisTemplate<String, Task> taskRedisTemplate(List<Task> capturedTasks) {
      var listOps = Mockito.mock(ListOperations.class);
      Mockito.when(listOps.leftPush(Mockito.anyString(), Mockito.any(Task.class)))
          .thenAnswer(invocation -> {
            capturedTasks.add(invocation.getArgument(1));
            return (long) capturedTasks.size();
          });
      Mockito.when(listOps.range(Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong()))
          .thenAnswer(invocation -> capturedTasks);

      var template = Mockito.mock(RedisTemplate.class);
      Mockito.when(template.opsForList()).thenReturn(listOps);
      return template;
    }

    @Bean
    public RedisTemplate<String, JudgeVerdict> verdictRedisTemplate(List<JudgeVerdict> incomingVerdicts) {
      var listOps = Mockito.mock(ListOperations.class);
      var verdict = new JudgeVerdict(
          "task-x",
          "judge-001",
          com.chimera.model.JudgeVerdictType.REJECT,
          0.6,
          "Confidence too low",
          "Please add more context",
          null);

      Mockito.when(listOps.rightPop(Mockito.eq("planner:queue:test-agent-001"), Mockito.anyLong(), Mockito.any(TimeUnit.class)))
          .thenReturn(verdict)
          .thenReturn(null);

      var template = Mockito.mock(RedisTemplate.class);
      Mockito.when(template.opsForList()).thenReturn(listOps);
      return template;
    }
  }
}
