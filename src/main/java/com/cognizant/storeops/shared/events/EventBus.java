package com.cognizant.storeops.shared.events;

import java.util.function.Consumer;

public interface EventBus {
    void emit(DomainEvent event);

    void subscribe(String type, Consumer<DomainEvent> listener);
}
