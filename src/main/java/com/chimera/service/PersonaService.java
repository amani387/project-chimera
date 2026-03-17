package com.chimera.service;

import com.chimera.model.AgentPersona;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

/**
 * Loads agent persona definitions from SOUL.md files.
 */
@Slf4j
@Service
public class PersonaService {

  private final Path personaDirectory;
  private final ConcurrentMap<String, AgentPersona> personas = new ConcurrentHashMap<>();
  private final Yaml yaml = new Yaml();

  public PersonaService(@Value("${chimera.agents.directory:agents}") String personaDirectory) {
    this.personaDirectory = Paths.get(personaDirectory);
  }

  @PostConstruct
  void init() {
    if (!Files.exists(personaDirectory) || !Files.isDirectory(personaDirectory)) {
      log.warn("Persona directory does not exist or is not a directory: {}", personaDirectory);
      return;
    }

    try {
      var soulFiles = Files.walk(personaDirectory)
          .filter(p -> p.getFileName().toString().equalsIgnoreCase("SOUL.md"))
          .collect(Collectors.toList());

      for (var file : soulFiles) {
        try {
          loadPersonaFromFile(file);
        } catch (Exception e) {
          log.warn("Failed to load persona file {}: {}", file, e.getMessage());
        }
      }

      log.info("Loaded {} persona(s) from {}", personas.size(), personaDirectory);
    } catch (IOException e) {
      log.warn("Failed to scan persona directory {}", personaDirectory, e);
    }
  }

  private void loadPersonaFromFile(Path file) throws IOException {
    var content = Files.readString(file);
    var fm = splitFrontMatter(content);

    if (fm == null) {
      throw new IllegalArgumentException("Missing YAML frontmatter");
    }

    var frontMatter = fm.front();
    var backstory = fm.body();

    Map<String, Object> data = yaml.load(frontMatter);
    if (data == null) {
      throw new IllegalArgumentException("Empty YAML frontmatter");
    }

    var id = (String) data.get("id");
    var name = (String) data.get("name");
    var voiceTraits = toStringList(data.get("voice_traits"));
    var directives = toStringList(data.get("directives"));
    var metadata = toMap(data.get("metadata"));

    if (id == null || name == null) {
      throw new IllegalArgumentException("Persona must include id and name");
    }

    var persona = new AgentPersona(id, name, voiceTraits, directives, backstory, metadata);
    personas.put(id, persona);
  }

  private static Map<String, Object> toMap(Object obj) {
    if (obj instanceof Map m) {
      return Map.copyOf(m);
    }
    return Map.of();
  }

  @SuppressWarnings("unchecked")
  private static List<String> toStringList(Object obj) {
    if (obj instanceof List<?> list) {
      return list.stream().map(String::valueOf).collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  private static record FrontMatter(String front, String body) {
  }

  private static FrontMatter splitFrontMatter(String content) {
    if (content == null) {
      return null;
    }
    var lines = content.split("\r?\n");
    if (lines.length < 3 || !lines[0].strip().equals("---")) {
      return null;
    }

    int end = -1;
    for (int i = 1; i < lines.length; i++) {
      if (lines[i].strip().equals("---")) {
        end = i;
        break;
      }
    }
    if (end <= 0) {
      return null;
    }

    var front = String.join("\n", java.util.Arrays.copyOfRange(lines, 1, end));
    var body = String.join("\n", java.util.Arrays.copyOfRange(lines, end + 1, lines.length)).strip();
    return new FrontMatter(front, body);
  }

  public AgentPersona getPersona(String agentId) {
    return personas.get(agentId);
  }
}
