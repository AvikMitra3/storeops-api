package com.cognizant.storeops.programmes.model;

import java.time.Instant;

public record Project(String id, String storeId, String name, String description, String ownerId, Instant createdAt) {
}
