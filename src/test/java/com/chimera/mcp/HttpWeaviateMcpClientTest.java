package com.chimera.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chimera.config.WeaviateProperties;
import com.chimera.model.MemoryEntry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

class HttpWeaviateMcpClientTest {

  @Test
  void storeMemory_shouldPostObjectToWeaviate() {
    var props = new WeaviateProperties();
    props.setMock(false);
    props.setClassName("Memory");

    var restTemplate = mock(RestTemplate.class);
    when(restTemplate.getForEntity(eq("/v1/schema/Memory"), eq(Map.class)))
        .thenThrow(new HttpServerErrorException(HttpStatus.NOT_FOUND));
    when(restTemplate.postForEntity(eq("/v1/schema"), any(HttpEntity.class), eq(Map.class)))
        .thenReturn(ResponseEntity.ok(Map.of()));
    when(restTemplate.postForEntity(eq("/v1/objects"), any(HttpEntity.class), eq(Map.class)))
        .thenReturn(ResponseEntity.ok(Map.of()));

    var client = new HttpWeaviateMcpClient(restTemplate, props);
    client.storeMemory("agent-1", "hello", Map.of("k", "v"));

    verify(restTemplate).postForEntity(eq("/v1/objects"), any(HttpEntity.class), eq(Map.class));
  }

  @Test
  void storeMemory_shouldWrapHttpErrorsInMcpToolException() {
    var props = new WeaviateProperties();
    props.setMock(false);
    props.setClassName("Memory");

    var restTemplate = mock(RestTemplate.class);
    when(restTemplate.getForEntity(eq("/v1/schema/Memory"), eq(Map.class)))
        .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

    var client = new HttpWeaviateMcpClient(restTemplate, props);

    assertThatThrownBy(() -> client.storeMemory("agent-1", "hi", Map.of()))
        .isInstanceOf(McpToolException.class);
  }

  @Test
  void searchMemories_shouldParseGraphqlResponse() {
    var props = new WeaviateProperties();
    props.setMock(false);
    props.setClassName("Memory");

    var restTemplate = mock(RestTemplate.class);
    when(restTemplate.getForEntity(eq("/v1/schema/Memory"), eq(Map.class)))
        .thenThrow(new HttpServerErrorException(HttpStatus.NOT_FOUND));
    when(restTemplate.postForEntity(eq("/v1/schema"), any(HttpEntity.class), eq(Map.class)))
        .thenReturn(ResponseEntity.ok(Map.of()));

    Map<String, Object> memoryObject = Map.of(
        "id", "id-1",
        "agentId", "agent-1",
        "content", "hello",
        "timestamp", "2025-01-01T00:00:00Z",
        "metadata", "{}"
    );

    Map<String, Object> getObject = Map.of("Memory", List.of(memoryObject));
    Map<String, Object> dataObject = Map.of("Get", getObject);

    Map<String, Object> response = Map.of("data", dataObject);

    when(restTemplate.postForEntity(eq("/v1/graphql"), any(HttpEntity.class), eq(Map.class)))
        .thenReturn(ResponseEntity.ok(response));

    var client = new HttpWeaviateMcpClient(restTemplate, props);
    var results = client.searchMemories("agent-1", "hello", 5).join();

    assertThat(results).hasSize(1);
    MemoryEntry entry = results.get(0);
    assertThat(entry.agentId()).isEqualTo("agent-1");
    assertThat(entry.content()).isEqualTo("hello");
  }
}
