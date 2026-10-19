package com.cognizant.storeops.reports.model;

import java.time.Instant;
import java.util.Map;

public record Report(String id, ReportType type, ReportStatus status, Map<String, Object> metrics,
        Instant generatedAt) {
}
