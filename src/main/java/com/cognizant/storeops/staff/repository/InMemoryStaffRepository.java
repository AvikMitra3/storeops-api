package com.cognizant.storeops.staff.repository;

import com.cognizant.storeops.staff.model.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryStaffRepository implements StaffRepository {
    private final Map<String, User> data = new ConcurrentHashMap<>();

    public InMemoryStaffRepository() {
        data.put("manager-1", new User("manager-1", "store-1", "Store Manager", StaffRole.STORE_MANAGER));
        data.put("associate-1", new User("associate-1", "store-1", "Associate", StaffRole.ASSOCIATE));
        data.put("lead-1", new User("lead-1", "store-1", "Department Lead", StaffRole.DEPARTMENT_LEAD));
    }

    public Optional<User> findById(String id) {
        return Optional.ofNullable(data.get(id));
    }

    public List<User> findByStoreAndRole(String storeId, StaffRole role) {
        return data.values().stream()
                .filter(u -> u.storeId().equals(storeId) && u.role() == role)
                .sorted(Comparator.comparing(User::id))
                .toList();
    }
}
