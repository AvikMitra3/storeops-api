package com.cognizant.storeops.alerts.service;

import com.cognizant.storeops.alerts.model.Notification;
import com.cognizant.storeops.alerts.repository.AlertRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AlertService {
    private final AlertRepository repository;

    public AlertService(AlertRepository r) {
        repository = r;
    }

    public List<Notification> forUser(String userId) {
        return repository.findByUserId(userId);
    }
}
