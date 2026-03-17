package com.chimera.planner;

import com.chimera.model.JudgeVerdict;
import com.chimera.model.Task;
import com.chimera.model.MemoryEntry;
import com.chimera.mcp.WeaviateMcpClient;
import com.chimera.service.PersonaService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "chimera.planner.enabled", havingValue = "true", matchIfMissing = true)
public class PlannerService {

  private static final String GOALS_RESOURCE = "goals.json";
  private static final String TASK_QUEUE = "task:queue:test-agent-001";
  private static final String PLANNER_QUEUE = "planner:queue:test-agent-001";
  private static final int MAX_RETRY_ATTEMPTS = 2;

  private final RedisTemplate<String, Task> taskRedisTemplate;
  private final RedisTemplate<String, JudgeVerdict> verdictRedisTemplate;
  private final ExecutorService executor;
  private final ObjectMapper objectMapper;

  private final java.util.concurrent.ConcurrentMap<String, Integer> retryCounts =
      new java.util.concurrent.ConcurrentHashMap<>();

  private volatile boolean running = true;

  private PersonaService personaService;
  private WeaviateMcpClient weaviateMcpClient;
  private String agentId = "test-agent-001";
  private int contextMemoryLimit = 5;
  private Context context;

  public PlannerService(
      RedisTemplate<String, Task> taskRedisTemplate,
      RedisTemplate<String, JudgeVerdict> verdictRedisTemplate,
      ExecutorService plannerExecutor) {
    this.taskRedisTemplate = taskRedisTemplate;
    this.verdictRedisTemplate = verdictRedisTemplate;
    this.executor = plannerExecutor;
    this.objectMapper = new ObjectMapper();
  }

  @Autowired(required = false)
  public void setPersonaService(PersonaService personaService) {
    this.personaService = personaService;
  }

  @Autowired(required = false)
  public void setWeaviateMcpClient(WeaviateMcpClient weaviateMcpClient) {
    this.weaviateMcpClient = weaviateMcpClient;
  }

  @Autowired(required = false)
  public void setAgentId(@Value("${chimera.agent.id:test-agent-001}") String agentId) {
    this.agentId = agentId;
  }

  @Autowired(required = false)
  public void setContextMemoryLimit(@Value("${chimera.planner.context-memory-limit:5}") int contextMemoryLimit) {
    this.contextMemoryLimit = contextMemoryLimit;
  }

  public Context getContext() {
    return context;
  }

  @PostConstruct
  void init() {
    try {
      var goals = loadGoals();
      var assembler = new ContextAssembler();
      this.context = assembler.assemble(agentId, goals, personaService, weaviateMcpClient, contextMemoryLimit);
      planGoals(goals);
      log.info("Loaded {} goals and pushed tasks to Redis.", goals.size());
    } catch (IOException e) {
      log.error("Failed to load goals from {}", GOALS_RESOURCE, e);
    }

    executor.submit(this::runVerdictLoop);
  }

  @PreDestroy
  void shutdown() {
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

  private void runVerdictLoop() {
    log.info("Planner verdict loop started");
    while (running && !Thread.currentThread().isInterrupted()) {
      try {
        var verdict = verdictRedisTemplate.opsForList().rightPop(PLANNER_QUEUE, 1, TimeUnit.SECONDS);
        if (verdict != null) {
          handleVerdict(verdict);
        }
      } catch (IllegalStateException e) {
        if (e.getMessage() != null && e.getMessage().toUpperCase().contains("STOPP")) {
          log.debug("Redis connection factory stopping/stopped; exiting planner loop");
          break;
        }
        log.error("Error in planner verdict loop", e);
      } catch (Exception e) {
        log.error("Error in planner verdict loop", e);
      }
    }
    log.info("Planner verdict loop stopped");
  }

  private void handleVerdict(JudgeVerdict verdict) {
    switch (verdict.verdict()) {
      case APPROVE -> log.info("Task {} approved by judge; no replanning needed.", verdict.taskId());
      case ESCALATE, REJECT -> {
        var attempts = retryCounts.compute(verdict.taskId(), (k, v) -> (v == null ? 1 : v + 1));
        if (attempts > MAX_RETRY_ATTEMPTS) {
          log.warn(
              "Max retry attempts reached for task {} ({}); dropping verdict {}",
              verdict.taskId(),
              attempts - 1,
              verdict.verdict());
          return;
        }

        var goal = String.format(
            "Replan for task %s (attempt %d/%d): %s",
            verdict.taskId(),
            attempts,
            MAX_RETRY_ATTEMPTS,
            verdict.feedbackForPlanner() != null ? verdict.feedbackForPlanner() : "No feedback provided");
        var task = Task.pending(goal);
        taskRedisTemplate.opsForList().leftPush(TASK_QUEUE, task);
        log.info("Replanned task {} (attempt {}/{}) based on judge verdict {}", verdict.taskId(), attempts, MAX_RETRY_ATTEMPTS, verdict.verdict());
      }
      default -> log.warn("Received verdict with unknown type: {}", verdict.verdict());
    }
  }

  List<String> loadGoals() throws IOException {
    var resource = new ClassPathResource(GOALS_RESOURCE);
    try (InputStream in = resource.getInputStream()) {
      return objectMapper.readValue(in, new TypeReference<>() {});
    }
  }

  void planGoals(List<String> goals) {
    for (String goal : goals) {
      var task = Task.pending(goal);
      taskRedisTemplate.opsForList().leftPush(TASK_QUEUE, task);
      log.debug("Enqueued task {} for goal '{}'.", task.taskId(), goal);
    }
  }
}
