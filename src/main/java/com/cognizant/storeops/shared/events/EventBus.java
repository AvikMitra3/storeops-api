package com.cognizant.storeops.shared.events;

public interface EventBus {
    void emit(DomainEvent event);
}
