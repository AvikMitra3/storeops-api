package com.cognizant.storeops.shared.events;

import java.time.Instant;
import java.util.Map;

public record DomainEvent(String type, Instant occurredAt, Map<String, Object> payload) {
}
