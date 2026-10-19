package com.cognizant.storeops.shared.error;

import java.time.Instant;

public record ErrorResponse(String code, String message, int statusCode, Instant timestamp) {
}
