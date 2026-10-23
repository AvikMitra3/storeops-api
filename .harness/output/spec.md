# Spec: SLA breach alerting with escalation

## Goal
When a HIGH or CRITICAL task passes its due date without reaching DONE, fire an `SLA_BREACH` notification to the assigned Department Lead. If the task is still not DONE after a configurable grace period, fire an escalation notification to the store's STORE_MANAGER.

## Findings in the existing code (drive the design)
1. `Task` has **no due date**. A `dueDate` field is required.
2. `EventBus` only has `emit`; `InMemoryEventBus` just stores events. **Nothing consumes events today** and nothing creates `Notification`s (`AlertRepository.save` has no caller). A subscribe mechanism is required.
3. No scheduler and no `Clock` exist. `@EnableScheduling` and an injectable `Clock` are required (tests must not use the wall clock).
4. `StaffRepository` only has `findById`; the seed has `manager-1` (STORE_MANAGER) and `associate-1`, no DEPARTMENT_LEAD. A store+role lookup and a seeded lead are needed.
5. `AlertType` already contains `SLA_BREACH` and `ESCALATION`; reuse them (no enum change).
6. ArchUnit forbids non-alerts classes from depending on `alerts.service`/`alerts.repository` and non-staff classes on `staff.repository`. The alerts module must therefore subscribe to the bus itself and call `StaffService` (allowed: a service, not a repository).
7. `Task` is constructed positionally in `ActivityService` and `ServiceCoverageTest`; adding fields requires updating those call sites.

## Scope
- In: `dueDate` on tasks (create/update), periodic SLA scan, breach and escalation domain events, event-bus subscription, notification creation (IN_APP), staff lookup by store and role, configuration, tests.
- Out: EMAIL delivery, new notification endpoints (existing `GET /api/alerts` is used to observe results), mark-as-read, persistence, per-priority grace periods, repeat reminders, reports.

## Design
- **Task model**: add `dueDate` (Instant, nullable), `slaBreachNotifiedAt` (Instant, nullable), `slaEscalatedAt` (Instant, nullable). The last two are system-managed and ignored in requests; they make the scan idempotent.
- **Request changes**: `CreateTaskRequest` and `UpdateTaskRequest` gain an optional `dueDate` (ISO-8601 instant). Existing clients stay valid.
- **Config** (`application.yml`): `storeops.sla.grace-period: PT30M`, `storeops.sla.scan-interval: PT1M`, `storeops.sla.scan-enabled: true` (tests set false and call the scan directly with a fixed `Clock`).
- **Detection (activities)**: `ActivityService.scanSlaBreaches(Instant now)` runs over all stores.
  - Breach: priority HIGH or CRITICAL, status != DONE, dueDate != null, now > dueDate, `slaBreachNotifiedAt == null`. Set `slaBreachNotifiedAt = now`, emit `SLA_BREACH_DETECTED`.
  - Escalation: still != DONE, `slaBreachNotifiedAt != null`, `slaEscalatedAt == null`, now >= dueDate + grace. Set `slaEscalatedAt = now`, emit `SLA_ESCALATION_DUE`.
  - One scan may emit both if the task is already past due plus grace on first sight.
  - A scheduled component calls the scan every scan-interval. The repository gains a method to list SLA candidates across stores.
- **Event payloads** (String values only): `activityId, storeId, assigneeId ("" if none), priority, dueDate`.
- **Delivery (alerts)**: `EventBus` gains `subscribe(String type, Consumer<DomainEvent>)`; `InMemoryEventBus` dispatches synchronously after storing and isolates listener failures. An alerts listener subscribes to the two event types and creates Notifications through `AlertRepository`.
  - `SLA_BREACH_DETECTED` -> Notification(type `SLA_BREACH`, IN_APP, UNREAD) to the assignee if the assignee's role is DEPARTMENT_LEAD.
  - `SLA_ESCALATION_DUE` -> Notification(type `ESCALATION`, IN_APP, UNREAD) to the STORE_MANAGER(s) of the task's store.
  - Message includes task title, id and due date.
- **Tenancy**: the escalation target is looked up by the task's `storeId`, so a notification never crosses stores.

## Endpoint changes
| Method/Path | Change |
|---|---|
| POST `/api/activities` | optional `dueDate` in body; response Task includes `dueDate`, `slaBreachNotifiedAt`, `slaEscalatedAt` |
| update of `/api/activities/{id}` | optional `dueDate`; same response shape |
| GET `/api/alerts` | unchanged; now returns `SLA_BREACH` / `ESCALATION` notifications for the header user |

No new endpoints.

## Error contract
No new codes. A malformed `dueDate` gives 400 `VALIDATION_ERROR` via `GlobalExceptionHandler`. A scan or listener failure for one task must not abort processing of others (logged, skipped).

## Modules touched
`activities` plus shared Clock/scheduling config (S1), `staff` (S2), `shared.events` plus `alerts` (S3), `alerts` (S4). Alerts consumes events and calls `StaffService` only.

## Sprints
| Sprint | Title | Depends on |
|---|---|---|
| 1 | Due date, clock, SLA scan and events | none |
| 2 | Staff lookup by store and role, seeded Department Lead | none |
| 3 | EventBus subscribe and SLA_BREACH notification to the Department Lead | S1, S2 |
| 4 | Escalation notification to STORE_MANAGER and hardening | S1, S2, S3 |

S2 is deliberately small: the sizing rules allow one module per sprint, and the staff lookup is needed by both S3 and S4.

## Assumptions (please confirm or correct)
1. "Assigned Department Lead" = the task's `assigneeId`, valid only if that user has role DEPARTMENT_LEAD. If the task is unassigned or the assignee has another role, no SLA_BREACH notification goes to a lead; escalation still fires after the grace period.
2. The grace period is measured from `dueDate` (escalate at `dueDate + grace`), is global, and defaults to 30 minutes.
3. Escalation goes to every STORE_MANAGER of the task's store. If none exists it is skipped with a log line.
4. BLOCKED, TODO and IN_PROGRESS are all "not DONE" and can breach. Tasks without a `dueDate` never breach.
5. Changing priority or due date after a breach does not reset the flags (one breach and one escalation per task). Re-opening a DONE task does not re-fire.
6. Deleting a task removes it from scans; no event.
7. The scan runs in-process via `@Scheduled`; single instance only (no distributed lock).

## Risks
- Adding Task record components breaks positional constructors in main and test code (`ServiceCoverageTest`); S1 must update them.
- `EventBus` is mocked in existing tests; adding `subscribe` is source-compatible with Mockito mocks.
- Synchronous dispatch means a listener exception could break the emitting scan; the bus must catch and log per-listener errors.
- MockMvc tests share one Spring context; use fresh ids, a fixed `Clock` and a disabled scheduler to stay deterministic.
- The JaCoCo `*.service` 80% gate needs branch tests (no assignee, wrong role, no manager).

STATUS: APPROVED
