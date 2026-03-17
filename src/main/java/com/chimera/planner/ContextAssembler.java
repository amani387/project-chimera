package com.chimera.planner;

import com.chimera.model.AgentPersona;
import com.chimera.model.MemoryEntry;
import com.chimera.mcp.WeaviateMcpClient;
import com.chimera.service.PersonaService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Builds a planning context by combining an agent's persona with relevant memories.
 */
@Slf4j
public class ContextAssembler {

  public Context assemble(
      String agentId,
      List<String> goals,
      PersonaService personaService,
      WeaviateMcpClient weaviateMcpClient,
      int memoryLimit) {

    if (personaService == null) {
      log.debug("No PersonaService available; skipping context assembly");
      return null;
    }

    var persona = personaService.getPersona(agentId);
    if (persona == null) {
      log.warn("No persona found for agent {}", agentId);
      return null;
    }

    if (weaviateMcpClient == null || goals == null || goals.isEmpty()) {
      return new Context(persona, List.of());
    }

    var goalQuery = goals.get(0);
    var memories = List.<MemoryEntry>of();
    try {
      var result = weaviateMcpClient.searchMemories(agentId, goalQuery, memoryLimit).join();
      if (result != null) {
        memories = result;
      }
    } catch (Exception e) {
      log.warn("Failed to search memories for agent {}: {}", agentId, e.getMessage());
    }

    return new Context(persona, memories);
  }
}
