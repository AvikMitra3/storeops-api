# Generator summary

Sprint: sla-sprint-3 (EventBus subscription and SLA_BREACH notification). Iteration: 1/3.

## AC self-check
| AC id | Met (Y/N) | Evidence |
|---|---|---|
| S3-AC1 | Y | `ServiceCoverageTest#eventBusDispatchesByType` |
| S3-AC2 | Y | `ServiceCoverageTest#eventBusIsolatesSubscriberFailures`; `InMemoryEventBus.emit` catches `RuntimeException` per listener and logs |
| S3-AC3 | Y | `ApiMockMvcTest#slaBreachNotifiesDepartmentLead` (task for lead-1, due 2020-01-01; `scanSlaBreaches(2020-01-01T00:01:00Z)`; GET /api/alerts as lead-1: SLA_BREACH, IN_APP, UNREAD, message has title and id). Plus `ServiceCoverageTest#breachListenerNotifiesLead` |
| S3-AC4 | Y | `ServiceCoverageTest#breachListenerSkipsNonLeadAssignee` (ASSOCIATE assignee, empty assignee, unknown assignee; no save, no exception) |
| S3-AC5 | Y | `ApiMockMvcTest#slaBreachNotificationVisibleOnlyToRecipient` |
| S3-AC6 | Y | `ArchitectureTest` passes (10 tests). `alerts.service` uses `StaffService` only |

## Files changed
- `src/main/java/com/cognizant/storeops/shared/events/EventBus.java`: added `subscribe(String, Consumer<DomainEvent>)`.
- `src/main/java/com/cognizant/storeops/shared/events/InMemoryEventBus.java`: per-type listener registry, synchronous dispatch after storing, per-listener failure isolation (logged).
- `src/main/java/com/cognizant/storeops/alerts/service/AlertService.java`: now takes `StaffService`; new `notifySlaBreach(DomainEvent)` creates SLA_BREACH/IN_APP/UNREAD notification for a DEPARTMENT_LEAD assignee, skips blank, unknown or non-lead assignees.
- `src/main/java/com/cognizant/storeops/alerts/service/SlaAlertListener.java` (new): subscribes to `SLA_BREACH_DETECTED` at construction.
- `src/main/java/com/cognizant/storeops/activities/service/ActivityService.java`: SLA event payload now also carries `title` (needed for the message; the spec payload list lacked it).
- `src/test/java/com/cognizant/storeops/services/ServiceCoverageTest.java`: new S3 tests; expected payload of the S1 breach test updated with `title`; `AlertService` constructor call updated.
- `src/test/java/com/cognizant/storeops/routes/ApiMockMvcTest.java`: two S3 MockMvc tests and helpers.

## mvn clean verify
BUILD SUCCESS. Tests run: 41, Failures: 0, Errors: 0, Skipped: 0 (ApiMockMvcTest 11, ServiceCoverageTest 19, StoreOpsApplicationTests 1, ArchitectureTest 10). Checkstyle: 0 violations. JaCoCo check passed. Line coverage computed from `target/site/jacoco/jacoco.csv`: total 95.3%, service 99.2%, routes 94.4%, shared.events 100%, shared.config 100%.

## Known gaps
- Payload gained a `title` key beyond the spec's listed fields (deliberate; a contract-S1 test assertion was updated accordingly).
- `ESCALATION` handling is not implemented (sprint 4). A scan past due plus grace emits `SLA_ESCALATION_DUE` with no listener yet.
