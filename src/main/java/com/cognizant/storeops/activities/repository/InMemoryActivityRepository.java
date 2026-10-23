package com.cognizant.storeops.activities.repository;

import com.cognizant.storeops.activities.model.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryActivityRepository implements ActivityRepository {
    private final Map<String, Task> data = new ConcurrentHashMap<>();

    public List<Task> find(String storeId, String programmeId, TaskStatus status) {
        return data.values().stream().filter(x -> x.storeId().equals(storeId))
                .filter(x -> programmeId == null || Objects.equals(x.programmeId(), programmeId))
                .filter(x -> status == null || x.status() == status).toList();
    }

    public Optional<Task> findById(String id) {
        return Optional.ofNullable(data.get(id));
    }

    public List<Task> findSlaCandidates() {
        return data.values().stream().filter(x -> x.dueDate() != null && x.status() != TaskStatus.DONE)
                .filter(x -> x.priority() == TaskPriority.HIGH || x.priority() == TaskPriority.CRITICAL).toList();
    }

    public Task save(Task task) {
        data.put(task.id(), task);
        return task;
    }

    public void delete(String id) {
        data.remove(id);
    }
}
