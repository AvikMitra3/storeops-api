package com.cognizant.storeops.alerts.repository;

import com.cognizant.storeops.alerts.model.Notification;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryAlertRepository implements AlertRepository {
    private final List<Notification> data = new CopyOnWriteArrayList<>();

    public List<Notification> findByUserId(String id) {
        return data.stream().filter(x -> x.userId().equals(id)).toList();
    }

    public Notification save(Notification n) {
        data.add(n);
        return n;
    }
}
