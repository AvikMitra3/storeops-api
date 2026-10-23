# Sprint 3: EventBus subscription and SLA_BREACH notification
Depends on: S1, S2
Files expected: `shared/events/EventBus.java`, `shared/events/InMemoryEventBus.java`, an SLA listener in `alerts/service`, `alerts/service/AlertService.java`, `ServiceCoverageTest.java`, `ApiMockMvcTest.java`

## Acceptance criteria
### S3-AC1 Bus dispatches to subscribers
GIVEN a subscriber registered for type "X" on `InMemoryEventBus`
WHEN events of type "X" and "Y" are emitted
THEN the subscriber receives only "X"; both events remain in `events()`
Proof: `ServiceCoverageTest#eventBusDispatchesByType`

### S3-AC2 Failing subscriber does not break emit
GIVEN a subscriber that throws and a second healthy subscriber
WHEN an event is emitted
THEN `emit` returns normally and the healthy subscriber still receives it
Proof: `ServiceCoverageTest#eventBusIsolatesSubscriberFailures`

### S3-AC3 Lead is notified of a breach
GIVEN a HIGH task assigned to `lead-1` (DEPARTMENT_LEAD, store-1) that is overdue
WHEN the SLA scan runs with a fixed Clock
THEN `GET /api/alerts` as `lead-1` returns 200 with a notification of type `SLA_BREACH`, channel IN_APP, status UNREAD, message containing the task title and id
Proof: `ApiMockMvcTest#slaBreachNotifiesDepartmentLead`

### S3-AC4 Non-lead or unassigned yields no lead notification
GIVEN overdue HIGH tasks, one assigned to `associate-1` and one unassigned
WHEN the scan runs
THEN no notification is created for either, no exception, the scan completes
Proof: `ServiceCoverageTest#breachListenerSkipsNonLeadAssignee`

### S3-AC5 Notification is private to the recipient
GIVEN the lead notification exists
WHEN `GET /api/alerts` as `manager-1`
THEN the lead's notification is absent
Proof: `ApiMockMvcTest#slaBreachNotificationVisibleOnlyToRecipient`

### S3-AC6 Boundaries
GIVEN the code base
WHEN `ArchitectureTest` runs
THEN no class outside `alerts` depends on `alerts.service` or `alerts.repository`; alerts uses `StaffService`, not `staff.repository`
Proof: `ArchitectureTest`

## Out of scope
Escalation, email channel, mark-as-read.
