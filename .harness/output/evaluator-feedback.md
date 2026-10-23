# Evaluator feedback: Sprint 4 (Escalation to STORE_MANAGER and hardening), iteration 1 of 3

VERDICT: PASS

## Automated results
- A1 `mvn clean verify`: exit 0 (first attempt, no OneDrive lock problem).
- A2 surefire txt: Failures 0, Errors 0, Skipped 0 in every file. Counts: ArchitectureTest 10, ApiMockMvcTest (incl. nested ConfiguredGrace, reported in `ApiMockMvcTest$ConfiguredGrace.txt`) 14, ServiceCoverageTest 22, StoreOpsApplicationTests 1 = 47 (matches generator-summary).
- A3 ArchitectureTest 10 tests, 0 failures. A4 JaCoCo `check` is part of verify and the build exited 0.
- A5 all six Proof methods present in TEST-*.xml with no `<failure>`/`<error>` (any file: 0).
- `grep "throw new (Error|RuntimeException)(" src/main`: no matches. `git diff pom.xml`: empty.

## Dimension scores
| Dimension | Passed/Total | Weighted |
|---|---|---|
| D1 | 8/8 | 40 |
| D2 | 9/9 | 30 |
| D3 | 12/12 (AC1-7 plus R1-R4, with R1/R2 N/A-pass) | 30 |
Total = 40 + 30 + 30 = 100. No hard gate triggered.

## Per-check table
| Check id | Result | Evidence |
|---|---|---|
| D1-1 | PASS | A1 exit 0 |
| D1-2 | PASS | A2 clean |
| D1-3 | PASS | JaCoCo check inside verify passed |
| D1-4 | PASS | all 6 Proof methods found in surefire XML |
| D1-5 | PASS | no new endpoint this sprint (N/A) |
| D1-6 | PASS | event-driven path asserted via resulting notifications (ApiMockMvcTest:183-196); unit test emits SLA_ESCALATION_DUE (ServiceCoverageTest:322) |
| D1-7 | PASS | scans use fixed Instants (`DUE.plusSeconds(..)`, ApiMockMvcTest:180-233); no sleep |
| D1-8 | PASS | 47 total, 22 ServiceCoverageTest match summary |
| D2-1 | PASS | A3 |
| D2-2 | PASS | ArchUnit green; no routes change for escalation |
| D2-3 | PASS | logic in `AlertService.notifyEscalation` |
| D2-4 | PASS | no new throws; NotFoundError only caught |
| D2-5 | PASS | `SlaAlertListener` subscribes `SLA_ESCALATION_DUE` via EventBus; AlertService uses `StaffService`, not staff repository |
| D2-6 | PASS | constructor injection; no new models |
| D2-7 | PASS | no reports change |
| D2-8 | PASS | manager lookup keyed on event storeId: `findByStoreAndRole` filters `u.storeId().equals(storeId)` (InMemoryStaffRepository:22-27) |
| D2-9 | PASS | pom.xml unchanged |
| D3-1 S4-AC1 | PASS | ApiMockMvcTest:183 asserts exactly one ESCALATION IN_APP UNREAD with title for manager-1 and exactly one SLA_BREACH for lead-1 after scans at T+1m and T+30m |
| D3-2 S4-AC2 | PASS | ApiMockMvcTest:199 covers T+29m and DONE-then-T+30m |
| D3-3 S4-AC3 | PASS | ServiceCoverageTest:292 store-2 event saves nothing and store-1 manager lookup never made |
| D3-4 S4-AC4 | PASS | ServiceCoverageTest:304 assertDoesNotThrow and no save |
| D3-5 S4-AC5 | PASS | ServiceCoverageTest:313 no `getById`, one ESCALATION to manager-1 |
| D3-6 S4-AC6 | PASS | see ruling below |
| D3-7 S4-AC7 | PASS | docs/api-examples.md "SLA breach alerting" shows dueDate on create plus SLA_BREACH and ESCALATION alerts; README.md table lists `storeops.sla.grace-period`, `scan-interval`, `scan-enabled` |
| D3-R1 | PASS | no error contract change |
| D3-R2 | PASS | N/A, bulk untouched |
| D3-R3 | PASS | files changed match contract's expected files plus SlaAlertListener and test resources already part of the feature |
| D3-R4 | PASS | summary has AC table, files, known gaps; claims verified |

## S4-AC6 ruling (nested class)
The contract names `ApiMockMvcTest#escalationHonoursConfiguredGrace` and requires a test property override `storeops.sla.grace-period=PT5M`. A property override requires its own Spring context, so a plain method on the outer class cannot satisfy the GIVEN. The method `escalationHonoursConfiguredGrace` exists inside `ApiMockMvcTest.ConfiguredGrace` (ApiMockMvcTest:212-235), carries `@SpringBootTest(properties = "storeops.sla.grace-period=PT5M")`, runs and passes, and its testcase name matches the A5 grep. It asserts no escalation at T+1m and escalation at T+5m. Accepted as satisfying the contract; the disclosure was honest.

## Hard gates triggered
None. Ambiguity fallbacks: none used.

## Non-scoring notes
- Surefire lists the outer `ApiMockMvcTest` report with 0 tests and puts its tests in the `$ConfiguredGrace` file; cosmetic.
- AC3 is proven at unit level with mocks; real StaffRepository store filter confirmed by reading code.
