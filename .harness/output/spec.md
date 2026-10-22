# Spec: Shift handover bulk status update

## Goal
Let outgoing shift staff mark many activities (tasks) `DONE` or `BLOCKED` in one call. One bad item must not abort the batch. Every updated task leaves an audit entry.

## Scope
- In: `PATCH /api/activities/bulk-status`, request/response records, service method, audit events, tests.
- Out: new statuses, authorization beyond store scoping, persistence, notifications.

## Endpoint
| Method/Path | Headers | Body | Success |
|---|---|---|---|
| PATCH `/api/activities/bulk-status` | `X-User-Id`, `X-Store-Id`, `X-User-Role` | `{"updates":[{"activityId":"...","status":"DONE\|BLOCKED"}]}` (1..100 items) | 200 `{"updated":[Task...],"failed":[{"activityId","code","message"}]}` |

## Error contract
- Envelope invalid (empty, >100 items, blank id, null status): 400 `VALIDATION_ERROR` via `GlobalExceptionHandler`.
- Per item: status other than DONE/BLOCKED -> failed entry `VALIDATION_ERROR`; unknown id or other store's id -> failed entry `NOT_FOUND`.
- Order of `failed` follows request order.

## Modules touched
`activities` only (routes, service, model). Audit = one `ACTIVITY_STATUS_CHANGED` `DomainEvent` per updated task with payload `activityId, from, to, changedBy`.

## Sprints
1. Sprint 1: bulk-status endpoint, partial-failure handling, audit events (single sprint; fits one pass per the sprint-decomposition sizing rules).

## Risks
- `/bulk-status` must be declared so it is not shadowed by `/{id}`.
- The event payload is the only audit store (in-memory bus).

> Provenance note: the production code for this feature pre-dated these harness artefacts in the working tree. This spec and the run records were produced afterwards by following the harness procedure against that code; the Evaluator's automated checks were executed for real.

STATUS: APPROVED
