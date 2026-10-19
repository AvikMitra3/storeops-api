package com.cognizant.storeops.alerts.repository;

import com.cognizant.storeops.alerts.model.Notification;
import java.util.List;

public interface AlertRepository {
    List<Notification> findByUserId(String userId);

    Notification save(Notification n);
}
