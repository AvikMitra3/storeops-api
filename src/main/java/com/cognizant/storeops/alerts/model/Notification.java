package com.cognizant.storeops.alerts.model;

import java.time.Instant;

public record Notification(String id, String userId, String message, NotificationChannel channel,
        NotificationStatus status, AlertType type, Instant createdAt) {
}
