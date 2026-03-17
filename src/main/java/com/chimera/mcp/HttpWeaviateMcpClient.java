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

    var escapedQuery = escapeForGraphql(query);
    var escapedAgentId = escapeForGraphql(agentId);

    // Build a GraphQL query that filters by agentId and also matches the query text in content.
    // We use a LIKE operator to avoid depending on Weaviate's bm25 module.
    var whereClause = "operator: And, operands: ["
        + "{ operator: Equal, path: [\"agentId\"], valueString: \"" + escapedAgentId + "\" },"
        + "{ operator: Like, path: [\"content\"], valueString: \"%" + escapedQuery + "%\" }"
        + "]";

    String gql = String.format(
        """
            {
              Get {
                %s(
                  where: {%s},
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
        whereClause,
        limit);

    var request = Map.<String, Object>of("query", gql);

    try {
      var response = postJson("/v1/graphql", request);
      var results = parseSearchResponse(response);
      if (results.isEmpty() && query != null && !query.isBlank()) {
        // Fallback to a simple agentId-only filter if the query doesn't return results.
        results = searchByAgentId(agentId, limit);
      }
      return CompletableFuture.completedFuture(results);
    } catch (Exception e) {
      throw new McpToolException("Failed to search memories in Weaviate", e);
    }
  }

  private List<MemoryEntry> searchByAgentId(String agentId, int limit) {
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
    return parseSearchResponse(response);
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
              Map.of("name", "content", "dataType", List.of("text")),
              Map.of("name", "timestamp", "dataType", List.of("date")),
              Map.of("name", "metadata", "dataType", List.of("text"))
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
      var response = restTemplate.exchange(
          path,
          org.springframework.http.HttpMethod.POST,
          entity,
          new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
      return response.getBody();
    } catch (HttpStatusCodeException e) {
      throw new McpToolException("Weaviate HTTP error: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
    } catch (RestClientException e) {
      throw new McpToolException("Failed to call Weaviate", e);
    }
  }
}
