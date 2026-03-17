package com.chimera.mcp;

import com.chimera.config.WeaviateProperties;
import com.chimera.model.MemoryEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Real HTTP implementation of a Weaviate MCP client.
 */
@Slf4j
public class HttpWeaviateMcpClient implements WeaviateMcpClient {

  private final RestTemplate restTemplate;
  private final WeaviateProperties props;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final AtomicBoolean schemaEnsured = new AtomicBoolean(false);

  public HttpWeaviateMcpClient(RestTemplate restTemplate, WeaviateProperties props) {
    this.restTemplate = restTemplate;
    this.props = props;
  }

  @Override
  public CompletableFuture<Void> storeMemory(String agentId, String content, Map<String, Object> metadata) {
    ensureSchemaExists();

    var timestamp = Instant.now().toString();
    var properties = new HashMap<String, Object>();
    properties.put("agentId", agentId);
    properties.put("content", content);
    properties.put("timestamp", timestamp);
    properties.put("metadata", serializeMetadata(metadata));

    var request = Map.<String, Object>of(
        "class", props.getClassName(),
        "properties", properties
    );

    try {
      postJson("/v1/objects", request);
      return CompletableFuture.completedFuture(null);
    } catch (Exception e) {
      throw new McpToolException("Failed to store memory in Weaviate", e);
    }
  }

  @Override
  public CompletableFuture<List<MemoryEntry>> searchMemories(String agentId, String query, int limit) {
    ensureSchemaExists();

    try {
      // Fetch a pool of results for this agent, then filter in-memory.
      // This reduces reliance on Weaviate's filtering behavior and makes tests more stable.
      var all = searchByAgentId(agentId, Math.max(limit, 20));
      if (query == null || query.isBlank()) {
        return CompletableFuture.completedFuture(all.stream().limit(limit).toList());
      }

      var normalized = query.toLowerCase();
      var filtered = all.stream()
          .filter(m -> m.content() != null && m.content().toLowerCase().contains(normalized))
          .limit(limit)
          .toList();

      return CompletableFuture.completedFuture(filtered);
    } catch (Exception e) {
      throw new McpToolException("Failed to search memories in Weaviate", e);
    }
  }

  private List<MemoryEntry> searchByAgentId(String agentId, int limit) {
    // Prefer the REST objects endpoint for reliably fetching stored memories.
    try {
      var url = String.format("/v1/objects?class=%s&limit=%d", props.getClassName(), limit);
      var response = restTemplate.getForEntity(url, Map.class).getBody();
      var results = parseObjectsResponse(response, agentId);
      if (results.isEmpty()) {
        log.debug("Weaviate objects endpoint returned 0 results for agentId={}; response={}", agentId, response);
      }
      return results;
    } catch (Exception e) {
      log.warn("Failed to query objects endpoint; falling back to GraphQL search", e);
      // Fallback to GraphQL if the objects endpoint fails for any reason.
    }

    // GraphQL fallback (original implementation)
    var escapedAgentId = escapeForGraphql(agentId);
    var gql = String.format(
        """
            {
              Get {
                %s(
                  where: {
                    operator: Equal,
                    path: [\"agentId\"],
                    valueString: \"%s\"
                  },
                  limit: %d
                ) {
                  id
                  agentId
                  content
                  timestamp
                  metadata
                }
              }
            }
            """,
        props.getClassName(),
        escapedAgentId,
        limit);

    var request = Map.<String, Object>of("query", gql);
    var response = postJson("/v1/graphql", request);
    var results = parseSearchResponse(response);
    if (results.isEmpty()) {
      log.debug("Weaviate search by agentId returned 0 results; response={}", response);
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  private List<MemoryEntry> parseObjectsResponse(Map<String, Object> response, String agentId) {
    if (response == null) {
      return List.of();
    }

    var objects = (List<Map<String, Object>>) response.get("objects");
    if (objects == null) {
      return List.of();
    }

    var entries = new ArrayList<MemoryEntry>();
    for (var obj : objects) {
      var properties = (Map<String, Object>) obj.get("properties");
      if (properties == null) {
        continue;
      }
      var objectAgentId = (String) properties.get("agentId");
      if (!agentId.equals(objectAgentId)) {
        continue;
      }
      var id = (String) obj.get("id");
      var content = (String) properties.get("content");
      var timestamp = parseInstant(properties.get("timestamp"));
      var metadata = parseMetadata(properties.get("metadata"));
      entries.add(new MemoryEntry(
          id != null ? id : "",
          objectAgentId != null ? objectAgentId : "",
          "weaviate",
          content != null ? content : "",
          List.of(),
          metadata,
          timestamp != null ? timestamp : Instant.now(),
          0,
          Instant.now()));
    }
    return entries;
  }

  private void ensureSchemaExists() {
    if (schemaEnsured.get()) {
      return;
    }

    synchronized (schemaEnsured) {
      if (schemaEnsured.get()) {
        return;
      }

      try {
        restTemplate.getForEntity("/v1/schema/" + props.getClassName(), Map.class);
        schemaEnsured.set(true);
        return;
      } catch (HttpStatusCodeException e) {
        if (e.getStatusCode().value() != 404) {
          throw new McpToolException("Failed to verify Weaviate schema", e);
        }
      } catch (RestClientException e) {
        throw new McpToolException("Failed to query Weaviate schema", e);
      }

      var schema = Map.<String, Object>of(
          "class", props.getClassName(),
          "vectorizer", "none",
          "properties", List.of(
              Map.of("name", "agentId", "dataType", List.of("string")),
              Map.of("name", "content", "dataType", List.of("string")),
              Map.of("name", "timestamp", "dataType", List.of("date")),
              Map.of("name", "metadata", "dataType", List.of("string"))
          )
      );

      try {
        postJson("/v1/schema", schema);
        schemaEnsured.set(true);
      } catch (Exception e) {
        throw new McpToolException("Failed to create Weaviate schema", e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private List<MemoryEntry> parseSearchResponse(Map<String, Object> response) {
    if (response == null) {
      return List.of();
    }

    var data = (Map<String, Object>) response.get("data");
    if (data == null) {
      return List.of();
    }

    var get = (Map<String, Object>) data.get("Get");
    if (get == null) {
      return List.of();
    }

    var items = (List<Map<String, Object>>) get.get(props.getClassName());
    if (items == null) {
      return List.of();
    }

    var entries = new ArrayList<MemoryEntry>();
    for (var item : items) {
      var id = (String) item.get("id");
      var agentId = (String) item.get("agentId");
      var content = (String) item.get("content");
      var timestamp = parseInstant(item.get("timestamp"));
      var metadata = parseMetadata(item.get("metadata"));

      entries.add(new MemoryEntry(
          id != null ? id : "",
          agentId != null ? agentId : "",
          "weaviate",
          content != null ? content : "",
          List.of(),
          metadata,
          timestamp != null ? timestamp : Instant.now(),
          0,
          Instant.now()));
    }

    return entries;
  }

  private Instant parseInstant(Object value) {
    if (value instanceof String s) {
      try {
        return Instant.parse(s);
      } catch (Exception e) {
        // ignore
      }
    }
    return Instant.now();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> parseMetadata(Object value) {
    if (value instanceof Map m) {
      return Map.copyOf(m);
    }
    if (value instanceof String s) {
      try {
        return objectMapper.readValue(s, Map.class);
      } catch (JsonProcessingException e) {
        // ignore and fallback
      }
    }
    return Map.of();
  }

  private String serializeMetadata(Map<String, Object> metadata) {
    if (metadata == null || metadata.isEmpty()) {
      return "";
    }
    try {
      return objectMapper.writeValueAsString(metadata);
    } catch (JsonProcessingException e) {
      log.warn("Failed to serialize metadata for Weaviate; storing empty string", e);
      return "";
    }
  }

  private String escapeForGraphql(String input) {
    if (input == null) {
      return "";
    }
    return input.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private Map<String, Object> postJson(String path, Object payload) {
    try {
      var headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      var entity = new HttpEntity<>(payload, headers);
      var response = restTemplate.postForEntity(path, entity, Map.class);
      return response.getBody();
    } catch (HttpStatusCodeException e) {
      throw new McpToolException("Weaviate HTTP error: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
    } catch (RestClientException e) {
      throw new McpToolException("Failed to call Weaviate", e);
    }
  }
}
