package com.cognizant.storeops.reports.repository;

import com.cognizant.storeops.reports.model.Report;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryReportRepository implements ReportRepository {
    private final Map<String, Report> data = Map.of();

    public Optional<Report> findById(String id) {
        return Optional.ofNullable(data.get(id));
    }
}
