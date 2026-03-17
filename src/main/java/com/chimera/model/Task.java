package com.chimera.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Task object created by the Planner and consumed by Workers.
 *
 * <p>The structure is defined in specs/technical.md §1.2.
 */
public record Task(
    String taskId,
    String taskType,
    String priority,
    Context context,
    BudgetAllocation budgetAllocation,
    String assignedWorkerId,
    Instant createdAt,
    String status,
    Map<String, Object> parameters
) {

  public record Context(
      String goalDescription,
      List<String> personaConstraints,
      List<String> requiredResources,
      List<String> referenceMaterials
  ) {

  }

  public record BudgetAllocation(
      Double maxCostUsdc,
      String currency
  ) {
  }

  public static Task pending(String goalDescription) {
    return new Task(
        java.util.UUID.randomUUID().toString(),
        "generate_content",
        "medium",
        new Context(goalDescription, List.of(), List.of(), List.of()),
        null,
        null,
        Instant.now(),
        "pending",
        Map.of()
    );
  }

  public static Task storeMemory(String content, Map<String, Object> metadata) {
    return new Task(
        java.util.UUID.randomUUID().toString(),
        "store_memory",
        "low",
        new Context("Store memory", List.of(), List.of(), List.of()),
        null,
        null,
        Instant.now(),
        "pending",
        Map.of(
            "content", content,
            "metadata", metadata == null ? Map.of() : Map.copyOf(metadata)
        )
    );
  }

  public static Task searchMemory(String query, int limit) {
    return new Task(
        java.util.UUID.randomUUID().toString(),
        "search_memory",
        "low",
        new Context("Search memory", List.of(), List.of(), List.of()),
        null,
        null,
        Instant.now(),
        "pending",
        Map.of(
            "query", query,
            "limit", limit
        )
    );
  }
}

