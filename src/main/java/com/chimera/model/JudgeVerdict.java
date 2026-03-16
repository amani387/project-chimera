package com.chimera.model;

/**
 * Represents the verdict produced by the Judge for a WorkerResult.
 *
 * This is intentionally a record to ensure immutability and easy serialization.
 */
public record JudgeVerdict(
    String taskId,
    String judgeId,
    String verdict,
    double confidenceScore,
    String reasoning,
    String feedbackForPlanner,
    String stateVersionChecked
) {
}
