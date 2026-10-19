package com.cognizant.storeops.alerts.routes;

import com.cognizant.storeops.alerts.model.Notification;
import com.cognizant.storeops.alerts.service.AlertService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alerts")
public class AlertRoutes {
    private final AlertService service;

    public AlertRoutes(AlertService s) {
        service = s;
    }

    @GetMapping
    public List<Notification> list(@RequestHeader("X-User-Id") String userId) {
        return service.forUser(userId);
    }
}
