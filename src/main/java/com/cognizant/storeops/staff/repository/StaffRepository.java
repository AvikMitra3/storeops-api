package com.cognizant.storeops.staff.repository;

import com.cognizant.storeops.staff.model.User;
import com.cognizant.storeops.staff.model.StaffRole;
import java.util.List;
import java.util.Optional;

public interface StaffRepository {
    Optional<User> findById(String id);

    List<User> findByStoreAndRole(String storeId, StaffRole role);
}
