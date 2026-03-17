package com.chimera.model;

import java.util.List;
import java.util.Map;

/**
 * A persona used by an agent to guide its voice, directives, and long-term memory.
 */
public record AgentPersona(
    String id,
    String name,
    List<String> voiceTraits,
    List<String> directives,
    String backstory,
    Map<String, Object> metadata
) {
}
