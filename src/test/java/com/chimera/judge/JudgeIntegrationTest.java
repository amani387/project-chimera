package com.chimera.judge;

import static org.assertj.core.api.Assertions.assertThat;

import com.chimera.model.WorkerResult;
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
    "chimera.redis.enabled=false",
    "chimera.planner.enabled=false",
    "chimera.worker.enabled=false",
    "chimera.judge.enabled=true"
})
class JudgeIntegrationTest {

  @Autowired
  private JudgeService judgeService;

  @Autowired
  private List<com.chimera.model.JudgeVerdict> queuedVerdicts;

  @Test
  void judgeShouldApproveHighConfidenceResult() throws Exception {
    // Wait for the judge loop to process the mocked result
    boolean approved = waitForApprovedResults(1, 2, TimeUnit.SECONDS);
    boolean queued = waitForQueuedVerdicts(1, 2, TimeUnit.SECONDS);

    assertThat(approved).isTrue();
    assertThat(queued).isTrue();
    assertThat(judgeService.getApprovedResults()).hasSize(1);
    assertThat(judgeService.getApprovedResults().get(0).taskId()).isEqualTo("task-1");

    assertThat(queuedVerdicts).hasSize(1);
    assertThat(queuedVerdicts.get(0).verdict()).isEqualTo(com.chimera.model.JudgeVerdictType.APPROVE);
  }

  private boolean waitForQueuedVerdicts(int expectedSize, long timeout, TimeUnit unit) throws InterruptedException {
    long deadline = System.nanoTime() + unit.toNanos(timeout);
    while (System.nanoTime() < deadline) {
      if (queuedVerdicts.size() >= expectedSize) {
        return true;
      }
      Thread.sleep(50);
    }
    return false;
  }

  private boolean waitForApprovedResults(int expectedSize, long timeout, TimeUnit unit) throws InterruptedException {
    long deadline = System.nanoTime() + unit.toNanos(timeout);
    while (System.nanoTime() < deadline) {
      if (judgeService.getApprovedResults().size() >= expectedSize) {
        return true;
      }
      Thread.sleep(50);
    }
    return false;
  }

  @TestConfiguration
  static class JudgeTestConfig {

    @Bean
    public List<com.chimera.model.JudgeVerdict> queuedVerdicts() {
      return new java.util.concurrent.CopyOnWriteArrayList<>();
    }

    @Bean
    public RedisTemplate<String, com.chimera.model.JudgeVerdict> verdictRedisTemplate(
        List<com.chimera.model.JudgeVerdict> queuedVerdicts) {
      var listOps = Mockito.mock(ListOperations.class);
      Mockito.when(listOps.leftPush(Mockito.eq("planner:queue:test-agent-001"), Mockito.any()))
          .thenAnswer(invocation -> {
            queuedVerdicts.add(invocation.getArgument(1));
            return (long) queuedVerdicts.size();
          });

      var template = Mockito.mock(RedisTemplate.class);
      Mockito.when(template.opsForList()).thenReturn(listOps);
      return template;
    }

    @Bean
    public RedisTemplate<String, WorkerResult> resultRedisTemplate() {
      var listOps = Mockito.mock(ListOperations.class);

      var result = new WorkerResult(
          "task-1",
          "worker-1",
          new WorkerResult.WorkerOutput("image", "url", List.of()),
          0.95,
          "ok",
          List.of(),
          100,
          Map.of());

      Mockito.when(listOps.rightPop("review:queue:test-agent-001", 1, TimeUnit.SECONDS))
          .thenReturn(result)
          .thenReturn(null);

      var template = Mockito.mock(RedisTemplate.class);
      Mockito.when(template.opsForList()).thenReturn(listOps);
      return template;
    }
  }
}
