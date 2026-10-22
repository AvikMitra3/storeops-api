# API examples
All activity/programme requests use `X-User-Id`, `X-Store-Id`, and `X-User-Role` headers.

Implemented endpoints: `GET/POST /api/activities`, `GET/PATCH/DELETE /api/activities/{id}`, `GET/POST /api/programmes`, `POST /api/programmes/{id}/members`, `GET /api/alerts`, and `PATCH /api/activities/bulk-status` (shift handover: body `{"updates":[{"activityId","status":"DONE|BLOCKED"}]}`, returns `{updated, failed}`).
