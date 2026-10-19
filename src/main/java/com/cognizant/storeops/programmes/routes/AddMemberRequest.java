package com.cognizant.storeops.programmes.routes;

import com.cognizant.storeops.programmes.model.ProjectRole;
import jakarta.validation.constraints.*;

public record AddMemberRequest(@NotBlank String userId, @NotNull ProjectRole role) {
}
