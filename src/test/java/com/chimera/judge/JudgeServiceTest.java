package com.chimera.judge;

import static org.assertj.core.api.Assertions.assertThat;

import com.chimera.model.JudgeVerdict;
import com.chimera.model.WorkerResult;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

class JudgeServiceTest {

  private static ExecutorService executor;

  @BeforeAll
  static void setUpExecutor() {
    executor = Executors.newVirtualThreadPerTaskExecutor();
  }

  @AfterAll
  static void tearDownExecutor() {
    executor.shutdownNow();
  }

  @AfterEach
  void tearDown() {
    // ensure no threads are left waiting
  }

  @Test
  void processResult_shouldApproveWhenConfidenceAtOrAboveThreshold() {
    var resultListOps = Mockito.mock(ListOperations.class);
    var verdictListOps = Mockito.mock(ListOperations.class);

    var resultRedisTemplate = Mockito.mock(RedisTemplate.class);
    Mockito.when(resultRedisTemplate.opsForList()).thenReturn(resultListOps);

    var verdictRedisTemplate = Mockito.mock(RedisTemplate.class);
    Mockito.when(verdictRedisTemplate.opsForList()).thenReturn(verdictListOps);

    var judgeService = new JudgeService(resultRedisTemplate, verdictRedisTemplate, executor);

    var result = new WorkerResult(
        "task-1",
        "worker-1",
        new WorkerResult.WorkerOutput("image", "url", List.of()),
        0.95,
        "ok",
        List.of(),
        100,
        Map.of());

    judgeService.processResult(result);

    assertThat(judgeService.getApprovedResults()).hasSize(1);
    assertThat(judgeService.getApprovedResults().get(0).taskId()).isEqualTo("task-1");

    Mockito.verify(verdictListOps).leftPush(Mockito.eq("planner:queue:test-agent-001"), Mockito.any());
  }

  @Test
  void processResult_shouldRejectWhenConfidenceBelowThreshold() {
    var resultListOps = Mockito.mock(ListOperations.class);
    var verdictListOps = Mockito.mock(ListOperations.class);

    var resultRedisTemplate = Mockito.mock(RedisTemplate.class);
    Mockito.when(resultRedisTemplate.opsForList()).thenReturn(resultListOps);

    var verdictRedisTemplate = Mockito.mock(RedisTemplate.class);
    Mockito.when(verdictRedisTemplate.opsForList()).thenReturn(verdictListOps);

    var judgeService = new JudgeService(resultRedisTemplate, verdictRedisTemplate, executor);

    var result = new WorkerResult(
        "task-2",
        "worker-1",
        new WorkerResult.WorkerOutput("image", "url", List.of()),
        0.65,
        "low",
        List.of(),
        100,
        Map.of());

    judgeService.processResult(result);

    assertThat(judgeService.getApprovedResults()).isEmpty();
    Mockito.verify(verdictListOps).leftPush(Mockito.eq("planner:queue:test-agent-001"), Mockito.any());
  }

  @Test
  void processResult_shouldEscalateWhenConfidenceIsInGreyZone() {
    var resultListOps = Mockito.mock(ListOperations.class);
    var verdictListOps = Mockito.mock(ListOperations.class);

    var resultRedisTemplate = Mockito.mock(RedisTemplate.class);
    Mockito.when(resultRedisTemplate.opsForList()).thenReturn(resultListOps);

    var verdictRedisTemplate = Mockito.mock(RedisTemplate.class);
    Mockito.when(verdictRedisTemplate.opsForList()).thenReturn(verdictListOps);

    var captor = ArgumentCaptor.forClass(JudgeVerdict.class);

    var judgeService = new JudgeService(resultRedisTemplate, verdictRedisTemplate, executor);

    var result = new WorkerResult(
        "task-3",
        "worker-1",
        new WorkerResult.WorkerOutput("image", "url", List.of()),
        0.85,
        "ok",
        List.of(),
        100,
        Map.of());

    judgeService.processResult(result);

    assertThat(judgeService.getApprovedResults()).isEmpty();
    Mockito.verify(verdictListOps).leftPush(Mockito.eq("planner:queue:test-agent-001"), captor.capture());
    assertThat(captor.getValue().verdict()).isEqualTo(com.chimera.model.JudgeVerdictType.ESCALATE);
  }
}
