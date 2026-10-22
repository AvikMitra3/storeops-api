# Deployment

**Target:** local run of the built Spring Boot jar on port 18080. Docker is not installed on this machine, so the container path was not exercised.

## Steps taken
1. `mvn -B clean verify` -> BUILD SUCCESS, `target/storeops-api-1.0.0.jar`.
2. `java -jar target/storeops-api-1.0.0.jar --server.port=18080`
3. Created an activity, then called the new endpoint with a mixed batch.

## Evidence (real output, 2026-10-22)
Request:
```
curl -X PATCH localhost:18080/api/activities/bulk-status \
  -H "X-User-Id: u1" -H "X-Store-Id: s1" -H "X-User-Role: STORE_MANAGER" -H "Content-Type: application/json" \
  -d '{"updates":[{"activityId":"<id>","status":"BLOCKED"},{"activityId":"missing","status":"DONE"},{"activityId":"<id>","status":"TODO"}]}'
```
Response `HTTP/1.1 200`:
```
{"updated":[{"id":"75781517-2b41-430b-a9ae-dae174a0f8c1","storeId":"s1","programmeId":null,"title":"Handover","status":"BLOCKED","priority":"HIGH","category":"RESTOCKING","assigneeId":null,"ownerId":"u1","createdAt":"2026-10-22T11:44:45.843980400Z"}],"failed":[{"activityId":"missing","code":"NOT_FOUND","message":"Activity not found: missing"},{"activityId":"75781517-2b41-430b-a9ae-dae174a0f8c1","code":"VALIDATION_ERROR","message":"Bulk status must be DONE or BLOCKED"}]}
```
No screenshot was taken; the captured response above is the evidence.

## Docker (to run where Docker is available)
```
mvn -B clean verify
docker build -t storeops-api .
docker run -p 8080:8080 storeops-api
```
CI image publishing is handled by the existing `docker-publish.yml` workflow.
