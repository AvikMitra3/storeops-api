# Sprint 4: Escalation to STORE_MANAGER and hardening
Depends on: S1, S2, S3
Files expected: the SLA listener in `alerts/service`, `docs/api-examples.md`, `README.md` (config keys), `ServiceCoverageTest.java`, `ApiMockMvcTest.java`

## Acceptance criteria
### S4-AC1 Store manager notified after grace
GIVEN a HIGH task in store-1 assigned to `lead-1`, dueDate T, grace PT30M, not DONE
WHEN the scan runs at T+1m and again at T+30m
THEN `GET /api/alerts` as `manager-1` returns exactly one `ESCALATION` notification (IN_APP, UNREAD, message with title and id) and as `lead-1` exactly one `SLA_BREACH`
Proof: `ApiMockMvcTest#slaEscalationNotifiesStoreManager`

### S4-AC2 No escalation before grace or when resolved
GIVEN a breached task
WHEN the scan runs at T+29m, or the task is set DONE and the scan then runs at T+30m
THEN `manager-1` has no `ESCALATION` for that task
Proof: `ApiMockMvcTest#noEscalationBeforeGraceOrWhenDone`

### S4-AC3 Store isolation
GIVEN an overdue task in store-2 while `manager-1` belongs to store-1
WHEN escalation runs
THEN `manager-1` receives nothing for that task
Proof: `ServiceCoverageTest#escalationTargetsOnlySameStoreManager`

### S4-AC4 No manager is tolerated
GIVEN a store with no STORE_MANAGER
WHEN `SLA_ESCALATION_DUE` is handled
THEN no notification, no exception
Proof: `ServiceCoverageTest#escalationWithoutManagerIsSkipped`

### S4-AC5 Unassigned task still escalates
GIVEN an overdue unassigned CRITICAL task in store-1
WHEN the scan runs past dueDate + grace
THEN no lead notification, and one `ESCALATION` to `manager-1`
Proof: `ServiceCoverageTest#unassignedTaskStillEscalates`

### S4-AC6 Configurable grace
GIVEN `storeops.sla.grace-period=PT5M` via test property override
WHEN the scan runs at T+5m
THEN escalation fires
Proof: `ApiMockMvcTest#escalationHonoursConfiguredGrace`

### S4-AC7 Documented
GIVEN the repository docs
WHEN reviewed
THEN `docs/api-examples.md` shows dueDate on create and the resulting alerts; README lists the three `storeops.sla.*` keys
Proof: Evaluator file review

## Out of scope
Email, repeat reminders, per-priority grace, persistence.
