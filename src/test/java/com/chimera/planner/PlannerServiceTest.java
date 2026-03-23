package com.chimera.planner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

import com.chimera.model.JudgeVerdict;
import com.chimera.model.Task;
import com.chimera.planner.Context;
import com.chimera.worker.PromptBuilder;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
class PlannerServiceTest {

  @Mock
  private RedisTemplate<String, Task> redisTemplate;

  @Mock
  private RedisTemplate<String, JudgeVerdict> verdictRedisTemplate;

  @Mock
  private ListOperations<String, Task> listOperations;

  @Captor
  private ArgumentCaptor<Task> taskCaptor;

  private PlannerService plannerService;
  private ExecutorService executor;

  @BeforeEach
  void setUp() {
    executor = Executors.newVirtualThreadPerTaskExecutor();
    when(redisTemplate.opsForList()).thenReturn(listOperations);
    plannerService = new PlannerService(redisTemplate, verdictRedisTemplate, executor);
  }

  @AfterEach
  void tearDown() {
    plannerService.shutdown();
  }

@Test
  void planGoals_shouldPushOneTaskPerGoalToRedis() {
    var goals = List.of(
        "Promote the new summer fashion line to Gen-Z audience",
        "Create engagement around tech product launch"
    );

    plannerService.planGoals(goals);

    verify(listOperations, org.mockito.Mockito.times(2))
        .leftPush(eq("task:queue:test-agent-001"), taskCaptor.capture());

    var captured = taskCaptor.getAllValues();
    assertThat(captured).hasSize(2);

    assertThat(captured.get(0).status()).isEqualTo("pending");
    assertThat(captured.get(0).context().goalDescription()).isEqualTo(goals.get(0));

    assertThat(captured.get(1).status()).isEqualTo("pending");
    assertThat(captured.get(1).context().goalDescription()).isEqualTo(goals.get(1));
  }

  @Test
  void planGoals_withPromptBuilder_createsGenerateTextTasks() {
    var mockPromptBuilder = org.mockito.Mockito.mock(com.chimera.worker.PromptBuilder.class);
    var mockContext = org.mockito.Mockito.mock(com.chimera.planner.Context.class);
    plannerService.setPromptBuilder(mockPromptBuilder);
    // Set field via reflection or constructor, but for test use setter and assume context set
    // Mock set context properly without reflection issues
    Field contextField = null;
    try {
      contextField = PlannerService.class.getDeclaredField("context");
    } catch (NoSuchFieldException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SecurityException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    contextField.setAccessible(true);
    try {
      contextField.set(plannerService, mockContext);
    } catch (IllegalArgumentException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    when(mockPromptBuilder.buildSystemPrompt(any())).thenReturn("mock system prompt");
    when(mockPromptBuilder.buildUserPrompt(anyString(), anyString())).thenReturn("mock user prompt");

    var goals = List.of("Test fashion goal");
    plannerService.planGoals(goals);

    verify(listOperations).leftPush(eq("task:queue:test-agent-001"), taskCaptor.capture());
    var task = taskCaptor.getValue();
    assertThat(task.taskType()).isEqualTo("generate_text");
    assertThat(task.parameters()).containsKeys("systemPrompt", "userPrompt", "platform", "goalDescription");
    assertThat(task.parameters().get("platform")).isEqualTo("twitter");
    assertThat(task.status()).isEqualTo("pending");
  }
}

