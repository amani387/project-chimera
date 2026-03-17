package com.chimera.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.chimera.model.MemoryEntry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WeaviateMcpClientTest {

  @Test
  void mockClient_shouldStoreAndSearchMemories() {
    var client = new MockWeaviateMcpClient();

    client.storeMemory("agent-1", "Hello world", Map.of("tag", "greeting")).join();
    client.storeMemory("agent-1", "Goodbye world", Map.of("tag", "farewell")).join();

    List<MemoryEntry> results = client.searchMemories("agent-1", "hello", 10).join();
    assertThat(results).hasSize(1);
    assertThat(results.get(0).content()).contains("Hello world");

    List<MemoryEntry> all = client.searchMemories("agent-1", "", 10).join();
    assertThat(all).hasSize(2);
  }
}
