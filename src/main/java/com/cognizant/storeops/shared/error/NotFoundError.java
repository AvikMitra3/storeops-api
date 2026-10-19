package com.cognizant.storeops.shared.error;

public final class NotFoundError extends AppError {
    public NotFoundError(String message) {
        super("NOT_FOUND", message, 404);
    }
}
