package com.chimera.judge;

import static org.assertj.core.api.Assertions.assertThat;

import com.chimera.model.WorkerResult;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
    var listOps = Mockito.mock(ListOperations.class);
    var redisTemplate = Mockito.mock(RedisTemplate.class);
    Mockito.when(redisTemplate.opsForList()).thenReturn(listOps);

    var judgeService = new JudgeService(redisTemplate, executor);

    var result = new WorkerResult(
        "task-1",
        "worker-1",
        new WorkerResult.WorkerOutput("image", "url", List.of()),
        0.75,
        "ok",
        List.of(),
        100,
        Map.of());

    judgeService.processResult(result);

    assertThat(judgeService.getApprovedResults()).hasSize(1);
    assertThat(judgeService.getApprovedResults().get(0).taskId()).isEqualTo("task-1");
  }

  @Test
  void processResult_shouldRejectWhenConfidenceBelowThreshold() {
    var listOps = Mockito.mock(ListOperations.class);
    var redisTemplate = Mockito.mock(RedisTemplate.class);
    Mockito.when(redisTemplate.opsForList()).thenReturn(listOps);

    var judgeService = new JudgeService(redisTemplate, executor);

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
  }
}
