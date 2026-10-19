package com.cognizant.storeops.shared.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

@Component
public class InMemoryEventBus implements EventBus {
    private final List<DomainEvent> events = new CopyOnWriteArrayList<>();

    public void emit(DomainEvent event) {
        events.add(event);
    }

    public List<DomainEvent> events() {
        return List.copyOf(events);
    }
}
