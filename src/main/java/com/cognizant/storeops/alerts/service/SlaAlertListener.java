package com.cognizant.storeops.alerts.service;

import com.cognizant.storeops.shared.events.EventBus;
import org.springframework.stereotype.Component;

@Component
public class SlaAlertListener {
    public SlaAlertListener(EventBus eventBus, AlertService alertService) {
        eventBus.subscribe("SLA_BREACH_DETECTED", alertService::notifySlaBreach);
        eventBus.subscribe("SLA_ESCALATION_DUE", alertService::notifyEscalation);
    }
}
