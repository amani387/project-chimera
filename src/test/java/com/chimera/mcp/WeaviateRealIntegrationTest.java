package com.chimera.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.chimera.model.MemoryEntry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
    "chimera.redis.enabled=false",
    "chimera.planner.enabled=false",
    "chimera.worker.enabled=false",
    "chimera.weaviate.mock=false"
})
class WeaviateRealIntegrationTest {

  @Container
  @SuppressWarnings("resource")
  static final GenericContainer<?> weaviate = new GenericContainer<>("semitechnologies/weaviate:1.25.0")
      .withExposedPorts(8080)
      .withEnv("AUTHENTICATION_ANONYMOUS_ACCESS_ENABLED", "true")
      .withEnv("PERSISTENCE_DATA_PATH", "/var/lib/weaviate")
      .waitingFor(Wait.forHttp("/v1/.well-known/ready").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(2)));

  @Autowired
  private WeaviateMcpClient weaviateMcpClient;

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("chimera.weaviate.url", () -> "http://" + weaviate.getHost() + ":" + weaviate.getFirstMappedPort());
  }

  @Test
  @Timeout(value = 120)
  void storeAndSearchMemory_shouldReturnStoredEntry() {
    var agentId = "test-agent-integration";
    var content = "integration test memory";

    weaviateMcpClient.storeMemory(agentId, content, java.util.Map.of("foo", "bar")).join();

    // Weaviate indexing can be eventually consistent; retry a few times before failing.
    List<MemoryEntry> results = java.util.Collections.emptyList();
    long deadline = System.nanoTime() + java.time.Duration.ofSeconds(30).toNanos();
    while (System.nanoTime() < deadline) {
      results = weaviateMcpClient.searchMemories(agentId, "integration", 5).join();
      if (!results.isEmpty()) {
        break;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }

    assertThat(results).isNotEmpty();
    assertThat(results.get(0).agentId()).isEqualTo(agentId);
    assertThat(results.get(0).content()).contains("integration");
  }
}
