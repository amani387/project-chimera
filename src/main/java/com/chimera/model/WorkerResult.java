package com.chimera.model;

import java.util.List;
import java.util.Map;

/**
 * Result returned by a Worker after executing a Task.
 *
 * <p>The structure is defined in specs/technical.md §1.3.
 */
public record WorkerResult(
    String taskId,
    String workerId,
    WorkerOutput output,
    double confidenceScore,
    String reasoningTrace,
    List<Map<String, Object>> toolCalls,
    long executionTimeMs,
    Map<String, Object> metadata
) {

  public record WorkerOutput(
      String contentType,
      String content,
      List<String> artifacts
  ) {
  }
}
