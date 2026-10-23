package com.cognizant.storeops.staff.service;

import com.cognizant.storeops.staff.model.StaffRole;
import com.cognizant.storeops.staff.model.User;
import com.cognizant.storeops.staff.repository.StaffRepository;
import com.cognizant.storeops.shared.error.NotFoundError;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StaffService {
    private final StaffRepository repository;

    public StaffService(StaffRepository repository) {
        this.repository = repository;
    }

    public User getById(String id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundError("Staff member not found: " + id));
    }

    public List<User> findByStoreAndRole(String storeId, StaffRole role) {
        return repository.findByStoreAndRole(storeId, role);
    }
}
