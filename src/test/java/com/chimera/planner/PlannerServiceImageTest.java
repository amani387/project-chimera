package com.chimera.planner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.chimera.model.Task;
import com.chimera.planner.Context;
import com.chimera.worker.PromptBuilder;
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
class PlannerServiceImageTest {

  @Mock private RedisTemplate<String, Task> redisTemplate;
  @Mock private RedisTemplate<String, com.chimera.model.JudgeVerdict> verdictRedisTemplate;
  @Mock private ListOperations<String, Task> listOperations;
  @Captor private ArgumentCaptor<Task> taskCaptor;

  private PlannerService plannerService;
  private ExecutorService executor;
  private PromptBuilder mockPromptBuilder;
  private Context mockContext;

  @BeforeEach
  void setUp() {
    executor = Executors.newVirtualThreadPerTaskExecutor();
    when(redisTemplate.opsForList()).thenReturn(listOperations);
    plannerService = new PlannerService(redisTemplate, verdictRedisTemplate, executor);
    mockPromptBuilder = mock(PromptBuilder.class);
    mockContext = mock(Context.class);
    plannerService.setPromptBuilder(mockPromptBuilder);
  }

  @AfterEach
  void tearDown() {
    plannerService.shutdown();
  }

  @Test
  void planGoals_FashionGoal_CreatesGenerateImageTask() {
    // Arrange
    String fashionGoal = "Promote new summer fashion line";
    String expectedImagePrompt = "mock image prompt";
    when(mockPromptBuilder.buildImagePrompt(mockContext, fashionGoal, "instagram")).thenReturn(expectedImagePrompt);
    try {
      java.lang.reflect.Field contextField = PlannerService.class.getDeclaredField("context");
      contextField.setAccessible(true);
      contextField.set(plannerService, mockContext);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      org.junit.jupiter.api.Assertions.fail("Reflection setup failed: " + e.getMessage());
    }

    // Act
    plannerService.planGoals(List.of(fashionGoal));

    // Assert
    verify(listOperations).leftPush(anyString(), taskCaptor.capture());
    Task task = taskCaptor.getValue();
    assertThat(task.taskType()).isEqualTo("generate_image");
    assertThat(task.parameters().get("prompt")).isEqualTo(expectedImagePrompt);
    assertThat(task.parameters().get("platform")).isEqualTo("instagram");
    assertThat(task.parameters().get("width")).isEqualTo(1024);
    assertThat(task.parameters().get("height")).isEqualTo(1024);
    assertThat(task.status()).isEqualTo("pending");
  }

  @Test
  void planGoals_NonFashionGoal_CreatesGenerateTextTask() {
    // Arrange
    String techGoal = "Tech product launch";
    when(mockPromptBuilder.buildSystemPrompt(mockContext)).thenReturn("mock system");
    when(mockPromptBuilder.buildUserPrompt(techGoal, "twitter")).thenReturn("mock user");
    try {
      java.lang.reflect.Field contextField = PlannerService.class.getDeclaredField("context");
      contextField.setAccessible(true);
      contextField.set(plannerService, mockContext);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      org.junit.jupiter.api.Assertions.fail("Reflection setup failed: " + e.getMessage());
    }

    // Act
    plannerService.planGoals(List.of(techGoal));

    // Assert
    verify(listOperations).leftPush(anyString(), taskCaptor.capture());
    Task task = taskCaptor.getValue();
    assertThat(task.taskType()).isEqualTo("generate_text");
  }
}

