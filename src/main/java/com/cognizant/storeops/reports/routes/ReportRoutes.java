package com.cognizant.storeops.reports.routes;

import com.cognizant.storeops.reports.service.ReportService;
import org.springframework.stereotype.Component;

@Component
public class ReportRoutes {
    private final ReportService service;

    public ReportRoutes(ReportService s) {
        service = s;
    }

    public ReportService service() {
        return service;
    }
}
