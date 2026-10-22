# sprint-decomposition

**Purpose:** how the Planner turns a StoreOps feature request into sprints and contracts that the Generator can implement and the Evaluator can grade deterministically.

## Sizing rules
- A sprint touches at most **one module's** routes/service/repository plus at most one `shared` change, and adds at most 3 endpoints or 1 event flow.
- Order sprints so each one leaves `mvn clean verify` green and the app runnable. Foundations (model/repository) go first only when a later sprint needs them.
- Typical StoreOps split: S1 = vertical slice of the primary endpoint; S2 = cross-module effect via event bus (alert/report); S3 = hardening (limits, authorization, docs).
- If a feature fits one sprint (e.g. shift handover bulk update), plan one sprint; do not pad.

## Contract format (`sprint-N-contract.md`)
```
# Sprint N: <title>
Depends on: <sprint ids | none>
Files expected: <paths>
## Acceptance criteria
### SN-AC1 <short name>
GIVEN <state incl. headers/role/store>
WHEN <HTTP call or service call>
THEN <status code, body fields, events emitted, state afterwards>
Proof: <test class#method that must exist>
## Out of scope
```
Rules: 3-8 ACs; each THEN is observable and has exact status codes/error codes; at least one negative AC (validation 400, not-found, other-store isolation); at least one AC about events/audit when state changes.

## spec.md layout
Goal; scope and out-of-scope; endpoint table; error contract; modules touched; sprint list with dependencies; assumptions; risks; last line `STATUS: AWAITING APPROVAL`.

## Feature-specific hints
- Bulk status: PATCH `/api/activities/bulk-status`, items DONE/BLOCKED only, per-item result, one `ACTIVITY_STATUS_CHANGED` event per updated task.
- SLA breach: needs a clock abstraction (testability), `AlertType.SLA_BREACH`, grace period in `application.yml`; effects via events.
- Regional rollup: `GET /api/reports/region/{id}`; the Report record is created via event bus, endpoint stays read-only in spirit (document the tension in spec).
- Planogram template: `POST /api/programmes/{id}/templates`; creates `PLANOGRAM` tasks through `ActivityService`, not the repository.
