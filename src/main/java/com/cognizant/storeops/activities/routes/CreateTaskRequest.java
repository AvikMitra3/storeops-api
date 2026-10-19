package com.cognizant.storeops.activities.routes;

import com.cognizant.storeops.activities.model.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTaskRequest(@NotBlank String title, String programmeId, @NotNull TaskPriority priority,
        @NotNull TaskCategory category, String assigneeId) {
}
