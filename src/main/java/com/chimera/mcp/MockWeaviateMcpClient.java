package com.chimera.mcp;

import com.chimera.model.MemoryEntry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * In-memory mock implementation of a Weaviate MCP client.
 *
 * <p>Used for tests and local development when a real MCP server is not available.
 */
@Slf4j
public class MockWeaviateMcpClient implements WeaviateMcpClient {

  private final ConcurrentMap<String, List<MemoryEntry>> store = new ConcurrentHashMap<>();

  @Override
  public CompletableFuture<Void> storeMemory(String agentId, String content, Map<String, Object> metadata) {
    var entry = new MemoryEntry(
        UUID.randomUUID().toString(),
        agentId,
        "semantic",
        content,
        Collections.emptyList(),
        metadata != null ? Map.copyOf(metadata) : Map.of(),
        Instant.now(),
        0,
        Instant.now());

    store.compute(agentId, (k, list) -> {
      if (list == null) {
        list = new ArrayList<>();
      }
      list.add(entry);
      return list;
    });
    log.debug("Stored memory for agent {}: {}", agentId, entry.id());
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<List<MemoryEntry>> searchMemories(String agentId, String query, int limit) {
    var entries = store.getOrDefault(agentId, List.of());
    if (query == null || query.isBlank()) {
      return CompletableFuture.completedFuture(entries.stream().limit(limit).collect(Collectors.toList()));
    }

    var lowerQuery = query.toLowerCase();
    var matched = entries.stream()
        .filter(e -> e.content() != null && e.content().toLowerCase().contains(lowerQuery))
        .limit(limit)
        .collect(Collectors.toList());

    return CompletableFuture.completedFuture(matched);
  }
}
