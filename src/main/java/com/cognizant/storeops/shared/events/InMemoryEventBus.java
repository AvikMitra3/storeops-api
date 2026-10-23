package com.cognizant.storeops.shared.events;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InMemoryEventBus implements EventBus {
    private static final Logger LOG = LoggerFactory.getLogger(InMemoryEventBus.class);

    private final List<DomainEvent> events = new CopyOnWriteArrayList<>();
    private final Map<String, List<Consumer<DomainEvent>>> listeners = new ConcurrentHashMap<>();

    public void emit(DomainEvent event) {
        events.add(event);
        for (Consumer<DomainEvent> listener : listeners.getOrDefault(event.type(), List.of())) {
            try {
                listener.accept(event);
            } catch (RuntimeException ex) {
                LOG.error("Listener failed for event type {}", event.type(), ex);
            }
        }
    }

    public void subscribe(String type, Consumer<DomainEvent> listener) {
        listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public List<DomainEvent> events() {
        return List.copyOf(events);
    }
}
