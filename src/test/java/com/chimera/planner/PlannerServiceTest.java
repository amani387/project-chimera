package com.chimera.planner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chimera.model.Task;
import java.util.List;
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
  private ListOperations<String, Task> listOperations;

  @Captor
  private ArgumentCaptor<Task> taskCaptor;

  private PlannerService plannerService;

  @BeforeEach
  void setUp() {
    when(redisTemplate.opsForList()).thenReturn(listOperations);
    plannerService = new PlannerService(redisTemplate);
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
}
