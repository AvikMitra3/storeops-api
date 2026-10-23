package com.cognizant.storeops.activities.service;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "storeops.sla.scan-enabled", havingValue = "true", matchIfMissing = true)
public class SlaScanScheduler {
    private final ActivityService service;
    private final Clock clock;

    public SlaScanScheduler(ActivityService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${storeops.sla.scan-interval:PT1M}")
    public void scan() {
        service.scanSlaBreaches(clock.instant());
    }
}
