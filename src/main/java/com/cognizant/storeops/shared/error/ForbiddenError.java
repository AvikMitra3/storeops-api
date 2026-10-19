package com.cognizant.storeops.shared.error;

public final class ForbiddenError extends AppError {
    public ForbiddenError(String message) {
        super("FORBIDDEN", message, 403);
    }
}
