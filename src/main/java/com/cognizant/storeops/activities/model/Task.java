package com.cognizant.storeops.activities.model;

import java.time.Instant;

public record Task(String id, String storeId, String programmeId, String title, TaskStatus status,
        TaskPriority priority, TaskCategory category, String assigneeId, String ownerId, Instant createdAt) {
}
