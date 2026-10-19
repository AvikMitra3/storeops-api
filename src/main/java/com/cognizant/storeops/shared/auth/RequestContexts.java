package com.cognizant.storeops.shared.auth;

import com.cognizant.storeops.staff.model.StaffRole;
import com.cognizant.storeops.shared.error.ValidationError;

public final class RequestContexts {
    private RequestContexts() {
    }

    public static RequestContext of(String userId, String storeId, String role) {
        try {
            return new RequestContext(userId, storeId, StaffRole.valueOf(role));
        } catch (IllegalArgumentException e) {
            throw new ValidationError("Invalid X-User-Role");
        }
    }
}
