package com.cognizant.storeops.activities.model;

import java.util.List;

public record BulkStatusResult(List<Task> updated, List<Failure> failed) {
    public record Failure(String activityId, String code, String message) {
    }
}
