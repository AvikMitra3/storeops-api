# API examples
All activity/programme requests use `X-User-Id`, `X-Store-Id`, and `X-User-Role` headers.

Implemented endpoints: `GET/POST /api/activities`, `GET/PATCH/DELETE /api/activities/{id}`, `GET/POST /api/programmes`, `POST /api/programmes/{id}/members`, `GET /api/alerts`, and `PATCH /api/activities/bulk-status` (shift handover: body `{"updates":[{"activityId","status":"DONE|BLOCKED"}]}`, returns `{updated, failed}`).

## SLA breach alerting
Create a task with an optional ISO-8601 `dueDate`:
```
POST /api/activities
X-User-Id: manager-1  X-Store-Id: store-1  X-User-Role: STORE_MANAGER
{"title":"Restock aisle 4","priority":"HIGH","category":"RESTOCKING","assigneeId":"lead-1",
 "dueDate":"2030-01-01T10:00:00Z"}
```
The response includes `"dueDate":"2030-01-01T10:00:00Z","slaBreachNotifiedAt":null,"slaEscalatedAt":null`.

If the task is not DONE after `dueDate`, the department lead sees an `SLA_BREACH` alert:
```
GET /api/alerts    (X-User-Id: lead-1)
[{"type":"SLA_BREACH","channel":"IN_APP","status":"UNREAD",
  "message":"SLA breach: task 'Restock aisle 4' (<id>) was due 2030-01-01T10:00:00Z"}]
```
If it is still not DONE after `storeops.sla.grace-period` (default PT30M), the store manager sees an `ESCALATION` alert:
```
GET /api/alerts    (X-User-Id: manager-1)
[{"type":"ESCALATION","channel":"IN_APP","status":"UNREAD",
  "message":"SLA escalation: task 'Restock aisle 4' (<id>) was due 2030-01-01T10:00:00Z and is still not done"}]
```
