# Sprint 1: Bulk status update
Depends on: none
Files expected: `activities/routes/ActivityRoutes.java`, `activities/routes/BulkStatusRequest.java`, `activities/model/BulkStatusResult.java`, `activities/service/ActivityService.java`, `src/test/.../routes/ApiMockMvcTest.java`

## Acceptance criteria
### S1-AC1 Mixed batch is partially applied
GIVEN store-1 has an activity A in TODO and the caller is STORE_MANAGER of store-1
WHEN PATCH `/api/activities/bulk-status` with [A->BLOCKED, "missing"->DONE, A->TODO]
THEN 200; `updated` has 1 item (A, BLOCKED); `failed` has 2 items in order: `NOT_FOUND`, `VALIDATION_ERROR`
Proof: `ApiMockMvcTest#bulkStatusHandlesPartialFailure`

### S1-AC2 Invalid envelope rejected
GIVEN any valid identity headers
WHEN PATCH with `{"updates":[]}`
THEN 400 `VALIDATION_ERROR`
Proof: `ApiMockMvcTest#bulkStatusHandlesPartialFailure`

### S1-AC3 One audit event per updated task
GIVEN activity A in TODO
WHEN PATCH with [A->DONE]
THEN 200, `failed` empty, exactly one `ACTIVITY_STATUS_CHANGED` event with activityId=A, from=TODO, to=DONE, changedBy=caller
Proof: `ApiMockMvcTest#bulkStatusEmitsOneAuditEventPerUpdatedTask`

### S1-AC4 Layering and boundaries
GIVEN the code base
WHEN `ArchitectureTest` runs and Checkstyle runs
THEN routes do not depend on repositories and no untyped exception is thrown
Proof: `ArchitectureTest`

## Out of scope
Notifications, new statuses, persistence.
