package com.cognizant.storeops.activities.routes;

import com.cognizant.storeops.activities.model.TaskStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BulkStatusRequest(@NotEmpty @Size(max = 100) List<@Valid Item> updates) {
    public record Item(@NotBlank String activityId, @NotNull TaskStatus status) {
    }
}
