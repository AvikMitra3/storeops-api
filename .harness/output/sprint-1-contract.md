# Sprint 1: Due date, clock and SLA scan
Depends on: none
Files expected: `activities/model/Task.java`, `activities/routes/CreateTaskRequest.java`, `activities/routes/UpdateTaskRequest.java`, `activities/repository/ActivityRepository.java`, `activities/repository/InMemoryActivityRepository.java`, `activities/service/ActivityService.java`, a scheduler component in `activities`, shared Clock bean and `@EnableScheduling` config, `src/main/resources/application.yml`, `ServiceCoverageTest.java` (Task constructor), `ApiMockMvcTest.java`

## Acceptance criteria
### S1-AC1 Due date accepted and returned
GIVEN a STORE_MANAGER of store-1
WHEN POST `/api/activities` with priority HIGH and `dueDate` "2030-01-01T10:00:00Z"
THEN same success status as today; body `dueDate` equals the sent value; `slaBreachNotifiedAt` and `slaEscalatedAt` are null
Proof: `ApiMockMvcTest#createTaskWithDueDate`

### S1-AC2 Malformed due date rejected
GIVEN valid identity headers
WHEN POST `/api/activities` with `dueDate` "not-a-date"
THEN 400 `VALIDATION_ERROR`
Proof: `ApiMockMvcTest#createTaskRejectsMalformedDueDate`

### S1-AC3 Overdue HIGH/CRITICAL task emits breach once
GIVEN an IN_PROGRESS HIGH task and a TODO CRITICAL task with dueDate T, neither notified
WHEN `scanSlaBreaches(T + 1 minute)` runs twice
THEN exactly one `SLA_BREACH_DETECTED` event per task (payload activityId, storeId, assigneeId, priority, dueDate), `slaBreachNotifiedAt` equals the scan time, and the second run emits nothing
Proof: `ServiceCoverageTest#slaScanEmitsBreachOncePerTask`

### S1-AC4 Non-qualifying tasks ignored
GIVEN overdue tasks that are MEDIUM, LOW, DONE, or have no dueDate, plus a HIGH task not yet due
WHEN `scanSlaBreaches(now)` runs
THEN no event is emitted and no task is modified
Proof: `ServiceCoverageTest#slaScanIgnoresNonQualifyingTasks`

### S1-AC5 Escalation after grace period
GIVEN a breached HIGH task (dueDate T, grace PT30M) not DONE
WHEN the scan runs at T+29m, then T+30m, then T+31m
THEN no `SLA_ESCALATION_DUE` at T+29m; exactly one at T+30m with the same payload fields; none at T+31m; `slaEscalatedAt` is set
Proof: `ServiceCoverageTest#slaScanEscalatesAfterGraceOnce`

### S1-AC6 Resolved task does not escalate
GIVEN a breached task that is then updated to DONE
WHEN the scan runs after dueDate + grace
THEN no `SLA_ESCALATION_DUE` event
Proof: `ServiceCoverageTest#slaScanSkipsEscalationWhenDone`

### S1-AC7 Layering and config
GIVEN the code base
WHEN `ArchitectureTest` and Checkstyle run and the app context starts
THEN all pass; grace period is read from `storeops.sla.grace-period`; the scheduler is off when `storeops.sla.scan-enabled=false`
Proof: `ArchitectureTest`, `ApiMockMvcTest` context load

## Out of scope
Notifications, staff lookup, EventBus subscribe, new endpoints.
