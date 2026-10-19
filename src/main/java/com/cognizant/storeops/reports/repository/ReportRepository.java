package com.cognizant.storeops.reports.repository;

import com.cognizant.storeops.reports.model.Report;
import java.util.Optional;

public interface ReportRepository {
    Optional<Report> findById(String id);
}
