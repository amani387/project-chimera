package com.chimera.worker;

import com.chimera.model.AgentPersona;
import com.chimera.model.MemoryEntry;
import com.chimera.planner.Context;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptBuilder {

    public String buildSystemPrompt(Context context) {
        if (context == null || context.persona() == null) {
            return "You are a helpful AI assistant.";
        }

        AgentPersona persona = context.persona();
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("You are %s. ", persona.name()));

        if (persona.voiceTraits() != null && !persona.voiceTraits().isEmpty()) {
            sb.append("Your voice traits: ")
              .append(String.join(", ", persona.voiceTraits()))
              .append(". ");
        }

        if (persona.directives() != null && !persona.directives().isEmpty()) {
            sb.append("\n\nImportant guidelines: ")
              .append(String.join(". ", persona.directives()))
              .append(".");
        }

        if (persona.backstory() != null && !persona.backstory().isBlank()) {
            sb.append("\n\nYour background: ").append(persona.backstory());
        }

        if (context.memories() != null && !context.memories().isEmpty()) {
            sb.append("\n\nRelevant past memories:");
            for (MemoryEntry memory : context.memories()) {
                sb.append("\n- ").append(memory.content());
            }
        }

        return sb.toString();
    }

    public String buildUserPrompt(String goal, String platform) {
        if (platform != null && !platform.isBlank()) {
            return String.format("Write a %s post about: %s", platform, goal);
        }
        return goal;
    }

    public String buildImagePrompt(Context context, String goal, String platform) {
        if (context == null || context.persona() == null) {
            return goal;
        }

        AgentPersona persona = context.persona();
        String style = persona.voiceTraits() != null && !persona.voiceTraits().isEmpty() 
            ? String.join(", ", persona.voiceTraits()) 
            : "professional fashion";
        return String.format("Create a visually stunning %s image for: %s. In the style of %s (%s aesthetic). High quality, photorealistic.", 
            platform, goal, persona.name(), style);
    }
}
