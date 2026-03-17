package com.chimera.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents a memory entry stored by an agent.
 *
 * <p>Defined in specs/technical.md §1.5.
 */
public record MemoryEntry(
    String id,
    String agentId,
    String type,
    String content,
    List<Float> embedding,
    Map<String, Object> metadata,
    Instant timestamp,
    int accessCount,
    Instant lastAccessed
) {
}
