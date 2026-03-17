package com.chimera.mcp;

import com.chimera.model.MemoryEntry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Client that interacts with a Weaviate-based memory store via MCP tools.
 */
public interface WeaviateMcpClient {

  /**
   * Stores a memory entry for the given agent.
   */
  CompletableFuture<Void> storeMemory(String agentId, String content, Map<String, Object> metadata);

  /**
   * Searches memories for an agent using a natural language query.
   */
  CompletableFuture<List<MemoryEntry>> searchMemories(String agentId, String query, int limit);
}
