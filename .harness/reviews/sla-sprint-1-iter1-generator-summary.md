# Generator summary
Sprint: 1 (Due date, clock and SLA scan) | Iteration: 1/3

## AC self-check
| AC id | Met (Y/N) | Evidence |
|---|---|---|
| S1-AC1 | Y | `ApiMockMvcTest#createTaskWithDueDate` (201, dueDate echoed, both SLA timestamps null) |
| S1-AC2 | Y | `ApiMockMvcTest#createTaskRejectsMalformedDueDate` (400 VALIDATION_ERROR via new handler in GlobalExceptionHandler.java) |
| S1-AC3 | Y | `ServiceCoverageTest#slaScanEmitsBreachOncePerTask` |
| S1-AC4 | Y | `ServiceCoverageTest#slaScanIgnoresNonQualifyingTasks` (real InMemoryActivityRepository; tasks unchanged) |
| S1-AC5 | Y | `ServiceCoverageTest#slaScanEscalatesAfterGraceOnce` (T+29m/30m/31m) |
| S1-AC6 | Y | `ServiceCoverageTest#slaScanSkipsEscalationWhenDone` |
| S1-AC7 | Y | `ArchitectureTest` (10 pass), Checkstyle 0 violations, `StoreOpsApplicationTests`/`ApiMockMvcTest` context loads, `ApiMockMvcTest#schedulerIsOffWhenScanDisabled`; grace read via `@Value("${storeops.sla.grace-period:PT30M}")` in ActivityService.java; scheduler `@ConditionalOnProperty storeops.sla.scan-enabled` |

## Files changed
- `activities/model/Task.java`: added dueDate, slaBreachNotifiedAt, slaEscalatedAt
- `activities/routes/CreateTaskRequest.java`, `UpdateTaskRequest.java`: optional `Instant dueDate`
- `activities/repository/ActivityRepository.java`, `InMemoryActivityRepository.java`: `findSlaCandidates()`
- `activities/service/ActivityService.java`: new constructor arg (grace period), dueDate on create/update, SLA flags preserved, `scanSlaBreaches(Instant)`
- `activities/service/SlaScanScheduler.java` (new): `@Scheduled` scan using Clock, off when scan-enabled=false
- `shared/config/SchedulingConfig.java` (new): `@EnableScheduling` and `Clock` bean (UTC)
- `shared/error/GlobalExceptionHandler.java`: HttpMessageNotReadableException -> 400 VALIDATION_ERROR
- `src/main/resources/application.yml`: `storeops.sla.*` (grace-period, scan-interval, scan-enabled)
- `src/test/resources/application.properties` (new): `storeops.sla.scan-enabled=false` for tests
- `ServiceCoverageTest.java`, `ApiMockMvcTest.java`: updated Task/ActivityService/UpdateTaskRequest call sites, new tests (also far-overdue breach+escalation in one scan, scheduler uses clock, update dueDate)

## mvn clean verify
BUILD SUCCESS. Tests run: 32, Failures: 0, Errors: 0, Skipped: 0 (ApiMockMvcTest 9, ServiceCoverageTest 12, StoreOpsApplicationTests 1, ArchitectureTest 10). Checkstyle: 0 violations. "All coverage checks have been met."
Coverage computed from target/site/jacoco/jacoco.csv (line ratio): bundle 93.2%, activities.service 98.6%, activities.routes 100.0%, shared.config 100.0%, shared.events 100.0%, shared.error 100.0%.

## Known gaps
- Spec's "failure for one task must not abort others" is not implemented as try/catch in the scan, because coding-conventions forbid catching Exception/RuntimeException; the in-memory repository does not throw. Listener isolation belongs to Sprint 3 (EventBus).
- Malformed (unparseable) JSON bodies of any kind now return 400 VALIDATION_ERROR (previously Spring default 400 body); a side effect of the handler needed for AC2.
