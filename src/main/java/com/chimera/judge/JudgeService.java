package com.chimera.judge;

import com.chimera.model.JudgeVerdict;
import com.chimera.model.JudgeVerdictType;
import com.chimera.model.WorkerResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Judge service that consumes WorkerResults from Redis, validates them, and stores approved results.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "chimera.judge.enabled", havingValue = "true", matchIfMissing = true)
public class JudgeService {

  private static final String REVIEW_QUEUE = "review:queue:test-agent-001";
  private static final String PLANNER_QUEUE = "planner:queue:test-agent-001";
  private static final String JUDGE_ID = "judge-001";
  private static final double APPROVE_THRESHOLD = 0.9;
  private static final double ESCALATE_THRESHOLD = 0.7;

  private final RedisTemplate<String, WorkerResult> resultRedisTemplate;
  private final RedisTemplate<String, JudgeVerdict> verdictRedisTemplate;
  private final ExecutorService executor;

  private final List<ApprovedResult> approvedResults = new CopyOnWriteArrayList<>();
  private volatile boolean running = true;

  public JudgeService(
      @Qualifier("resultRedisTemplate") RedisTemplate<String, WorkerResult> resultRedisTemplate,
      @Qualifier("verdictRedisTemplate") RedisTemplate<String, JudgeVerdict> verdictRedisTemplate,
      @Qualifier("judgeExecutor") ExecutorService executor) {
    this.resultRedisTemplate = resultRedisTemplate;
    this.verdictRedisTemplate = verdictRedisTemplate;
    this.executor = executor;
  }

  @PostConstruct
  public void start() {
    executor.submit(() -> {
      log.info("Judge loop started");
      while (running && !Thread.currentThread().isInterrupted()) {
        try {
          WorkerResult result = resultRedisTemplate.opsForList().rightPop(REVIEW_QUEUE, 1, TimeUnit.SECONDS);
          if (result != null) {
            processResult(result);
          }
        } catch (IllegalStateException e) {
          // Connection factory may be stopping/stopped during shutdown.
          if (e.getMessage() != null && e.getMessage().toUpperCase().contains("STOPP")) {
            log.debug("Redis connection factory stopping/stopped; exiting judge loop");
            break;
          }
          log.error("Error in judge loop", e);
        } catch (Exception e) {
          log.error("Error in judge loop", e);
        }
      }
      log.info("Judge loop stopped");
    });
  }

  @PreDestroy
  public void stop() {
    running = false;
    executor.shutdown();
    try {
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  void processResult(WorkerResult result) {
    log.info("Judge received result for task {} (confidence={})", result.taskId(), result.confidenceScore());

    if (result.confidenceScore() >= APPROVE_THRESHOLD) {
      approve(result);
    } else if (result.confidenceScore() >= ESCALATE_THRESHOLD) {
      escalate(result);
    } else {
      reject(result);
    }
  }

  private void approve(WorkerResult result) {
    var approved = new ApprovedResult(
        UUID.randomUUID().toString(),
        result.taskId(),
        result,
        Instant.now());

    approvedResults.add(approved);

    var verdict = new JudgeVerdict(
        result.taskId(),
        JUDGE_ID,
        JudgeVerdictType.APPROVE,
        result.confidenceScore(),
        "Confidence meets threshold",
        null,
        null);

    log.info("Approved task {} (confidence={}); verdict={}", result.taskId(), result.confidenceScore(), verdict);
    pushVerdictToPlanner(verdict);
  }

  private void escalate(WorkerResult result) {
    var verdict = new JudgeVerdict(
        result.taskId(),
        JUDGE_ID,
        JudgeVerdictType.ESCALATE,
        result.confidenceScore(),
        "Confidence in gray zone; escalated for human review",
        "Escalation requested; schedule human review",
        null);

    log.warn("Escalated task {} (confidence={}); verdict={}", result.taskId(), result.confidenceScore(), verdict);
    pushVerdictToPlanner(verdict);
  }

  private void reject(WorkerResult result) {
    var verdict = new JudgeVerdict(
        result.taskId(),
        JUDGE_ID,
        JudgeVerdictType.REJECT,
        result.confidenceScore(),
        "Confidence too low",
        "Consider using a stronger prompt or more context",
        null);

    log.warn("Rejected task {} (confidence={}); verdict={}", result.taskId(), result.confidenceScore(), verdict);
    pushVerdictToPlanner(verdict);
  }

  private void pushVerdictToPlanner(JudgeVerdict verdict) {
    try {
      verdictRedisTemplate.opsForList().leftPush(PLANNER_QUEUE, verdict);
    } catch (Exception e) {
      log.error("Failed to publish verdict to planner queue", e);
    }
  }

  /**
   * Exposed for testing.
   */
  public List<ApprovedResult> getApprovedResults() {
    return approvedResults;
  }
}
