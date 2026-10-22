# Harness invocation prompt

```
RUN HARNESS: Add shift handover bulk update - PATCH /api/activities/bulk-status allowing outgoing shift staff to mark multiple operational activities as DONE or BLOCKED in a single request, with partial failure handling and an audit entry per updated task; constraints: keep routes -> service -> repository layering, typed AppError only, no new dependencies; acceptance notes: a bad item must not abort the batch, response lists updated and failed items
```
