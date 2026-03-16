package com.chimera.planner;

import static org.assertj.core.api.Assertions.assertThat;

import com.chimera.model.Task;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest(properties = {
    "chimera.planner.enabled=true",
    "chimera.redis.enabled=false",
    "chimera.worker.enabled=false"
})
class PlannerIntegrationTest {

  @Autowired
  private List<Task> capturedTasks;

  @Test
  void plannerShouldPushTasksIntoRedisQueue() {
    assertThat(capturedTasks).isNotNull().isNotEmpty();
    assertThat(capturedTasks.get(0).status()).isEqualTo("pending");
    assertThat(capturedTasks.get(0).context().goalDescription()).isNotBlank();
  }

  @TestConfiguration
  static class RedisTestConfig {

    @Bean
    public List<Task> capturedTasks() {
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
  }
}
