package com.cognizant.storeops.activities.repository;

import com.cognizant.storeops.activities.model.*;
import java.util.*;

public interface ActivityRepository {
    List<Task> find(String storeId, String programmeId, TaskStatus status);

    Optional<Task> findById(String id);

    Task save(Task task);

    void delete(String id);
}
