# Evaluator feedback
Sprint: 1 (Due date, clock and SLA scan) | Iteration: 1/3

VERDICT: PASS

## Automated checks
| Id | Result | Evidence |
|---|---|---|
| A1 `mvn -B clean verify` | PASS | exit 0, BUILD SUCCESS |
| A2 surefire reports | PASS | ArchitectureTest 10, ApiMockMvcTest 9, ServiceCoverageTest 12, StoreOpsApplicationTests 1 = 32; Failures 0, Errors 0, Skipped 0; no `<failure>`/`<error>` in TEST-*.xml |
| A3 ArchitectureTest | PASS | 10 tests pass inside A1 run |
| A4 JaCoCo | PASS | "All coverage checks have been met."; Checkstyle "0 violations" |
| A5 proof methods | PASS | createTaskWithDueDate, createTaskRejectsMalformedDueDate, slaScanEmitsBreachOncePerTask, slaScanIgnoresNonQualifyingTasks, slaScanEscalatesAfterGraceOnce, slaScanSkipsEscalationWhenDone, schedulerIsOffWhenScanDisabled each found once, passing |

## Hard gates
None triggered. `grep "throw new (Error|RuntimeException)(" src/main` has no matches; no routes class imports a repository; pom.xml unchanged.

## Checks
| Check id | Result | Evidence |
|---|---|---|
| D1-1 | PASS | A1 exit 0 |
| D1-2 | PASS | A2 clean |
| D1-3 | PASS | A4 line present |
| D1-4 | PASS | all contract Proof methods exist (A5) |
| D1-5 | PASS | no new endpoints (n/a); dueDate 201 and 400 covered |
| D1-6 | PASS | ServiceCoverageTest.java:113,152,171-172 assert event types |
| D1-7 | PASS | no sleep/Instant.now in SLA tests; fixed T and Clock.fixed (ServiceCoverageTest.java:87,195) |
| D1-8 | PASS | summary counts (32; 9/12/1/10) match surefire |
| D2-1 | PASS | A3 |
| D2-2 | PASS | no routes->repository imports in diff |
| D2-3 | PASS | SLA logic in ActivityService.scanSlaBreaches |
| D2-4 | PASS | no new throws; only handler added |
| D2-5 | PASS | emission via eventBus.emit in ActivityService.java (scan) |
| D2-6 | PASS | Task/requests are records; constructor injection (ActivityService, SlaScanScheduler) |
| D2-7 | PASS | no report changes |
| D2-8 | PASS | existing HTTP paths unchanged; scan is intentionally cross-store per spec |
| D2-9 | PASS | pom.xml not in diff |
| D3-1 (S1-AC1) | PASS | createTaskWithDueDate asserts dueDate echo and null SLA fields |
| D3-2 (S1-AC2) | PASS | createTaskRejectsMalformedDueDate; handler GlobalExceptionHandler.java:25 |
| D3-3 (S1-AC3) | PASS | ServiceCoverageTest.java:99-120 asserts 2 events, payload, notifiedAt==now, second run no emit |
| D3-4 (S1-AC4) | PASS | ServiceCoverageTest.java:123-137 real repo, never emit, tasks equal |
| D3-5 (S1-AC5) | PASS | ServiceCoverageTest.java:140-160 T+29/30/31m |
| D3-6 (S1-AC6) | PASS | ServiceCoverageTest.java:176-187 |
| D3-7 (S1-AC7) | PASS | @Value grace-period (ActivityService.java), @ConditionalOnProperty scan-enabled (SlaScanScheduler.java:8), test properties disable it, schedulerIsOffWhenScanDisabled passes |
| D3-R1 | PASS | malformed dueDate -> 400 VALIDATION_ERROR |
| D3-R2 | PASS | n/a, no partial-failure change |
| D3-R3 | PASS | extra files (GlobalExceptionHandler, test application.properties) justified in summary |
| D3-R4 | PASS | summary has AC table, files, known gaps; no claim contradicted |

## Score
D1 = 8/8 x 40 = 40; D2 = 9/9 x 30 = 30; D3 = 11/11 x 30 = 30. Total = 100. No gate, total >= 90, all D3 pass -> PASS.

## Non-scoring notes
- Spec error contract says a failure for one task must not abort others in the scan; not implemented (disclosed in Known gaps). Sprint 3 should cover listener isolation in the bus; consider revisiting for the scan loop.
- Any unparseable JSON body now returns 400 VALIDATION_ERROR with message "Malformed request body" (disclosed side effect).
- InMemoryActivityRepository.findSlaCandidates lacks an explicit @Override (consistent with existing methods).
