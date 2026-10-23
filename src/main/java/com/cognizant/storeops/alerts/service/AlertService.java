package com.cognizant.storeops.alerts.service;

import com.cognizant.storeops.alerts.model.AlertType;
import com.cognizant.storeops.alerts.model.Notification;
import com.cognizant.storeops.alerts.model.NotificationChannel;
import com.cognizant.storeops.alerts.model.NotificationStatus;
import com.cognizant.storeops.alerts.repository.AlertRepository;
import com.cognizant.storeops.shared.error.NotFoundError;
import com.cognizant.storeops.shared.events.DomainEvent;
import com.cognizant.storeops.staff.model.StaffRole;
import com.cognizant.storeops.staff.model.User;
import com.cognizant.storeops.staff.service.StaffService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AlertService {
    private static final Logger LOG = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository repository;
    private final StaffService staffService;

    public AlertService(AlertRepository r, StaffService staffService) {
        repository = r;
        this.staffService = staffService;
    }

    public List<Notification> forUser(String userId) {
        return repository.findByUserId(userId);
    }

    public void notifySlaBreach(DomainEvent event) {
        Map<String, Object> p = event.payload();
        String assigneeId = String.valueOf(p.get("assigneeId"));
        if (assigneeId.isBlank()) {
            LOG.info("SLA breach for task {} has no assignee; no lead notified", p.get("activityId"));
            return;
        }
        User assignee;
        try {
            assignee = staffService.getById(assigneeId);
        } catch (NotFoundError ex) {
            LOG.warn("SLA breach assignee {} not found; no lead notified", assigneeId);
            return;
        }
        if (assignee.role() != StaffRole.DEPARTMENT_LEAD) {
            return;
        }
        String message = "SLA breach: task '" + p.get("title") + "' (" + p.get("activityId") + ") was due "
                + p.get("dueDate");
        repository.save(new Notification(UUID.randomUUID().toString(), assignee.id(), message,
                NotificationChannel.IN_APP, NotificationStatus.UNREAD, AlertType.SLA_BREACH, event.occurredAt()));
    }

    public void notifyEscalation(DomainEvent event) {
        Map<String, Object> p = event.payload();
        List<User> managers = staffService.findByStoreAndRole(String.valueOf(p.get("storeId")),
                StaffRole.STORE_MANAGER);
        if (managers.isEmpty()) {
            LOG.warn("SLA escalation for task {} skipped: no store manager in store {}", p.get("activityId"),
                    p.get("storeId"));
            return;
        }
        String message = "SLA escalation: task '" + p.get("title") + "' (" + p.get("activityId")
                + ") was due " + p.get("dueDate") + " and is still not done";
        for (User manager : managers) {
            repository.save(new Notification(UUID.randomUUID().toString(), manager.id(), message,
                    NotificationChannel.IN_APP, NotificationStatus.UNREAD, AlertType.ESCALATION,
                    event.occurredAt()));
        }
    }
}
