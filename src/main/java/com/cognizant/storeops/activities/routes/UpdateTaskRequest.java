package com.cognizant.storeops.activities.routes;

import com.cognizant.storeops.activities.model.*;
import java.time.Instant;

public record UpdateTaskRequest(TaskStatus status, TaskPriority priority, TaskCategory category, String assigneeId,
        Instant dueDate) {
}
