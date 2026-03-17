package com.chimera.planner;

import com.chimera.model.AgentPersona;
import com.chimera.model.MemoryEntry;
import java.util.List;

/**
 * Context assembled for an agent, combining persona and relevant memories.
 */
public record Context(AgentPersona persona, List<MemoryEntry> memories) {
}
