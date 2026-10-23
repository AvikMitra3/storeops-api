package com.cognizant.storeops.activities.service;

import com.cognizant.storeops.activities.model.*;
import com.cognizant.storeops.activities.repository.ActivityRepository;
import com.cognizant.storeops.activities.routes.*;
import com.cognizant.storeops.shared.auth.RequestContext;
import com.cognizant.storeops.shared.error.*;
import com.cognizant.storeops.shared.events.*;
import com.cognizant.storeops.staff.model.StaffRole;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ActivityService {
    private final ActivityRepository repository;
    private final EventBus eventBus;

    private final Duration gracePeriod;

    public ActivityService(ActivityRepository repository, EventBus eventBus,
            @Value("${storeops.sla.grace-period:PT30M}") Duration gracePeriod) {
        this.repository = repository;
        this.eventBus = eventBus;
        this.gracePeriod = gracePeriod;
    }

    public List<Task> list(RequestContext c, String programmeId, TaskStatus status) {
        return repository.find(c.storeId(), programmeId, status);
    }

    public Task create(RequestContext c, CreateTaskRequest r) {
        Task t = new Task(UUID.randomUUID().toString(), c.storeId(), r.programmeId(), r.title(), TaskStatus.TODO,
                r.priority(), r.category(), r.assigneeId(), c.userId(), Instant.now(),
                r.dueDate(), null, null);
        repository.save(t);
        eventBus.emit(new DomainEvent("ACTIVITY_CREATED", Instant.now(), Map.of("activityId", t.id())));
        return t;
    }

    public Task get(RequestContext c, String id) {
        Task t = repository.findById(id).orElseThrow(() -> new NotFoundError("Activity not found: " + id));
        if (!t.storeId().equals(c.storeId()))
            throw new NotFoundError("Activity not found: " + id);
        return t;
    }

    public Task update(RequestContext c, String id, UpdateTaskRequest r) {
        Task old = get(c, id);
        Task t = new Task(old.id(), old.storeId(), old.programmeId(), old.title(),
                r.status() == null ? old.status() : r.status(), r.priority() == null ? old.priority() : r.priority(),
                r.category() == null ? old.category() : r.category(),
                r.assigneeId() == null ? old.assigneeId() : r.assigneeId(), old.ownerId(), old.createdAt(),
                r.dueDate() == null ? old.dueDate() : r.dueDate(), old.slaBreachNotifiedAt(),
                old.slaEscalatedAt());
        return repository.save(t);
    }

    public void delete(RequestContext c, String id) {
        Task t = get(c, id);
        if (!t.ownerId().equals(c.userId()) && c.role() != StaffRole.STORE_MANAGER)
            throw new ForbiddenError("Only the activity owner or store manager may delete it");
        repository.delete(id);
        eventBus.emit(new DomainEvent("ACTIVITY_DELETED", Instant.now(), Map.of("activityId", id)));
    }

    public BulkStatusResult bulkUpdateStatus(RequestContext c, BulkStatusRequest r) {
        List<Task> updated = new ArrayList<>();
        List<BulkStatusResult.Failure> failed = new ArrayList<>();
        for (BulkStatusRequest.Item item : r.updates()) {
            try {
                updated.add(applyHandoverStatus(c, item));
            } catch (AppError e) {
                failed.add(new BulkStatusResult.Failure(item.activityId(), e.getCode(), e.getMessage()));
            }
        }
        return new BulkStatusResult(updated, failed);
    }

    private Task applyHandoverStatus(RequestContext c, BulkStatusRequest.Item item) {
        if (item.status() != TaskStatus.DONE && item.status() != TaskStatus.BLOCKED)
            throw new ValidationError("Bulk status must be DONE or BLOCKED");
        Task old = get(c, item.activityId());
        Task t = repository.save(new Task(old.id(), old.storeId(), old.programmeId(), old.title(), item.status(),
                old.priority(), old.category(), old.assigneeId(), old.ownerId(), old.createdAt(), old.dueDate(),
                old.slaBreachNotifiedAt(), old.slaEscalatedAt()));
        eventBus.emit(new DomainEvent("ACTIVITY_STATUS_CHANGED", Instant.now(),
                Map.of("activityId", t.id(), "from", old.status().name(), "to", t.status().name(), "changedBy",
                        c.userId())));
        return t;
    }

    public void scanSlaBreaches(Instant now) {
        for (Task candidate : repository.findSlaCandidates()) {
            Task t = candidate;
            if (t.slaBreachNotifiedAt() == null && now.isAfter(t.dueDate())) {
                t = repository.save(withSlaFlags(t, now, t.slaEscalatedAt()));
                eventBus.emit(slaEvent("SLA_BREACH_DETECTED", t, now));
            }
            if (t.slaBreachNotifiedAt() != null && t.slaEscalatedAt() == null
                    && !now.isBefore(t.dueDate().plus(gracePeriod))) {
                t = repository.save(withSlaFlags(t, t.slaBreachNotifiedAt(), now));
                eventBus.emit(slaEvent("SLA_ESCALATION_DUE", t, now));
            }
        }
    }

    private Task withSlaFlags(Task t, Instant notifiedAt, Instant escalatedAt) {
        return new Task(t.id(), t.storeId(), t.programmeId(), t.title(), t.status(), t.priority(), t.category(),
                t.assigneeId(), t.ownerId(), t.createdAt(), t.dueDate(), notifiedAt, escalatedAt);
    }

    private DomainEvent slaEvent(String type, Task t, Instant now) {
        return new DomainEvent(type, now, Map.of("activityId", t.id(), "storeId", t.storeId(), "assigneeId",
                t.assigneeId() == null ? "" : t.assigneeId(), "title", t.title(), "priority", t.priority().name(), "dueDate",
                t.dueDate().toString()));
    }
}
