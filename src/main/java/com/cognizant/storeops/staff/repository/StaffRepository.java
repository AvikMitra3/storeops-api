package com.cognizant.storeops.staff.repository;

import com.cognizant.storeops.staff.model.User;
import java.util.Optional;

public interface StaffRepository {
    Optional<User> findById(String id);
}
