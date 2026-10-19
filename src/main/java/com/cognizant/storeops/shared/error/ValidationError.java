package com.cognizant.storeops.shared.error;

public final class ValidationError extends AppError {
    public ValidationError(String message) {
        super("VALIDATION_ERROR", message, 400);
    }
}
