package com.cognizant.storeops.staff.routes;

import com.cognizant.storeops.staff.service.StaffService;
import org.springframework.stereotype.Component;

@Component
public class StaffRoutes {
    private final StaffService service;

    public StaffRoutes(StaffService service) {
        this.service = service;
    }

    public StaffService service() {
        return service;
    }
}
