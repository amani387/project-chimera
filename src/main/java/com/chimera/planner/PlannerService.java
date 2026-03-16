package com.chimera.planner;

import com.chimera.model.Task;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "chimera.planner.enabled", havingValue = "true", matchIfMissing = true)
public class PlannerService {

  private static final String TASK_QUEUE_PREFIX = "task:queue:";
  private static final String DEFAULT_AGENT_ID = "test-agent-001";
  private static final String GOALS_RESOURCE = "goals.json";

  private final RedisTemplate<String, Task> taskRedisTemplate;
  private final ObjectMapper objectMapper;

  public PlannerService(RedisTemplate<String, Task> taskRedisTemplate) {
    this.taskRedisTemplate = taskRedisTemplate;
    this.objectMapper = new ObjectMapper();
  }

  @PostConstruct
  void init() {
    try {
      var goals = loadGoals();
      planGoals(goals);
      log.info("Loaded {} goals and pushed tasks to Redis.", goals.size());
    } catch (IOException e) {
      log.error("Failed to load goals from {}", GOALS_RESOURCE, e);
    }
  }

  List<String> loadGoals() throws IOException {
    var resource = new ClassPathResource(GOALS_RESOURCE);
    try (InputStream in = resource.getInputStream()) {
      return objectMapper.readValue(in, new TypeReference<>() {});
    }
  }

  void planGoals(List<String> goals) {
    var queue = TASK_QUEUE_PREFIX + DEFAULT_AGENT_ID;
    for (String goal : goals) {
      var task = Task.pending(goal);
      taskRedisTemplate.opsForList().leftPush(queue, task);
      log.debug("Enqueued task {} for goal '{}'.", task.taskId(), goal);
    }
  }
}
