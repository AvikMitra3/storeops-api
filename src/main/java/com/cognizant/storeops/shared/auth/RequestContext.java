package com.cognizant.storeops.shared.auth;

import com.cognizant.storeops.staff.model.StaffRole;

public record RequestContext(String userId, String storeId, StaffRole role) {
}
