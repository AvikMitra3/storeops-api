package com.cognizant.storeops.reports.service;

import com.cognizant.storeops.reports.model.Report;
import com.cognizant.storeops.reports.repository.ReportRepository;
import com.cognizant.storeops.shared.error.NotFoundError;
import org.springframework.stereotype.Service;

@Service
public class ReportService {
    private final ReportRepository repository;

    public ReportService(ReportRepository r) {
        repository = r;
    }

    public Report get(String id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundError("Report not found: " + id));
    }
}
