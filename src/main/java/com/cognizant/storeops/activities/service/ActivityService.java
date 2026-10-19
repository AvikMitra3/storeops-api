package com.cognizant.storeops.activities.service;

import com.cognizant.storeops.activities.model.*;
import com.cognizant.storeops.activities.repository.ActivityRepository;
import com.cognizant.storeops.activities.routes.*;
import com.cognizant.storeops.shared.auth.RequestContext;
import com.cognizant.storeops.shared.error.*;
import com.cognizant.storeops.shared.events.*;
import com.cognizant.storeops.staff.model.StaffRole;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class ActivityService {
    private final ActivityRepository repository;
    private final EventBus eventBus;

    public ActivityService(ActivityRepository repository, EventBus eventBus) {
        this.repository = repository;
        this.eventBus = eventBus;
    }

    public List<Task> list(RequestContext c, String programmeId, TaskStatus status) {
        return repository.find(c.storeId(), programmeId, status);
    }

    public Task create(RequestContext c, CreateTaskRequest r) {
        Task t = new Task(UUID.randomUUID().toString(), c.storeId(), r.programmeId(), r.title(), TaskStatus.TODO,
                r.priority(), r.category(), r.assigneeId(), c.userId(), Instant.now());
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
                r.assigneeId() == null ? old.assigneeId() : r.assigneeId(), old.ownerId(), old.createdAt());
        return repository.save(t);
    }

    public void delete(RequestContext c, String id) {
        Task t = get(c, id);
        if (!t.ownerId().equals(c.userId()) && c.role() != StaffRole.STORE_MANAGER)
            throw new ForbiddenError("Only the activity owner or store manager may delete it");
        repository.delete(id);
        eventBus.emit(new DomainEvent("ACTIVITY_DELETED", Instant.now(), Map.of("activityId", id)));
    }
}
