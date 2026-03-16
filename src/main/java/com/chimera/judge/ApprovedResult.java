package com.chimera.judge;

import com.chimera.model.WorkerResult;
import java.time.Instant;

/**
 * Represents an approved WorkerResult stored by the Judge.
 */
public record ApprovedResult(
    String id,
    String taskId,
    WorkerResult result,
    Instant approvedAt
) {
}
