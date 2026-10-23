VERDICT: PASS

# Evaluator feedback: sla-sprint-3 (iteration 1/3)

## Automated checks
| Id | Result | Evidence |
|---|---|---|
| A1 `mvn -B clean verify` | PASS | exit 0, BUILD SUCCESS (clean worked, no lock) |
| A2 surefire | PASS | ArchitectureTest 10, ApiMockMvcTest 11, ServiceCoverageTest 19, StoreOpsApplicationTests 1 = 41; 0 failures/errors/skipped |
| A3 ArchitectureTest | PASS | exit 0 |
| A4 JaCoCo | PASS | "All coverage checks have been met."; Checkstyle 0 violations |
| A5 proof methods | PASS | eventBusDispatchesByType, eventBusIsolatesSubscriberFailures, breachListenerSkipsNonLeadAssignee (ServiceCoverageTest, 3 testcases); slaBreachNotifiesDepartmentLead, slaBreachNotificationVisibleOnlyToRecipient (ApiMockMvcTest, 2 testcases); none failing |

## Scores
D1 8/8 x 40 = 40. D2 9/9 x 30 = 30. D3 12/12 x 30 = 30. Total 100.

## Checks
| Check id | Result | Evidence |
|---|---|---|
| D1-1/2/3 | PASS | A1, A2, A4 above |
| D1-4 | PASS | all 6 AC proof methods exist (S3-AC6 proof is ArchitectureTest, passes) |
| D1-5 | PASS | no new endpoint this sprint (N/A) |
| D1-6 | PASS | breach tests assert event types/payload (ServiceCoverageTest S1 test, breachEvent); no new state mutation of tasks |
| D1-7 | PASS | no Thread.sleep in src/test; fixed Instant `2020-01-01T00:01:00Z` used |
| D1-8 | PASS | summary says 41 (11/19/1/10), matches surefire |
| D2-1 | PASS | A3 |
| D2-2 | PASS | no routes change; ArchUnit green |
| D2-3 | PASS | role/assignee logic in AlertService.notifySlaBreach (AlertService.java) |
| D2-4 | PASS | no RuntimeException/Error thrown in main; AlertService catches typed NotFoundError |
| D2-5 | PASS | activities -> EventBus -> SlaAlertListener -> AlertService; no cross-module repository use |
| D2-6 | PASS | constructor injection; alerts.service uses StaffService (not staff.repository) |
| D2-7 | PASS | no report/HTTP write changes |
| D2-8 | PASS | not affected by this sprint (task store scoping untouched) |
| D2-9 | PASS | pom.xml not in diff |
| D3-S3-AC1 | PASS | eventBusDispatchesByType asserts received == [x] and events() == [x,y] |
| D3-S3-AC2 | PASS | eventBusIsolatesSubscriberFailures: assertDoesNotThrow, healthy listener receives |
| D3-S3-AC3 | PASS | slaBreachNotifiesDepartmentLead asserts SLA_BREACH/IN_APP/UNREAD/title; task id proven via alertsFor filter `message.contains(id)` (ApiMockMvcTest:153) |
| D3-S3-AC4 | PASS | breachListenerSkipsNonLeadAssignee: associate, blank, unknown; verify never save |
| D3-S3-AC5 | PASS | slaBreachNotificationVisibleOnlyToRecipient: manager-1 empty |
| D3-S3-AC6 | PASS | ArchitectureTest green; AlertService imports StaffService only |
| D3-R1 | PASS | no error contract change |
| D3-R2 | PASS | no bulk endpoint touched |
| D3-R3 | PASS | files changed match "Files expected" plus SlaAlertListener (the contract's "SLA listener in alerts/service") and ActivityService payload (disclosed); other working-tree changes belong to sprints 1-2 |
| D3-R4 | PASS | summary has AC table, files, known gaps; claims verified |

## Hard gates triggered
None. Ambiguity fallbacks used: none.

## Judgement of disclosed deviations
- `title` key added to SLA event payload (ActivityService): payload values stay Strings (principle 9), event type unchanged, S1 test expectation updated consistently, and the contract AC3 requires the message to contain the title, so it is necessary. Not a violation of coding-conventions/architecture-principles; accepted.
- InMemoryEventBus catches `RuntimeException` per listener and logs: required by S3-AC2. It is a `catch`, not `throw new RuntimeException`, so Checkstyle/D2-G2 do not apply; the error is logged with the exception, not swallowed silently. Accepted.

## Non-scoring notes
- AlertService.notifySlaBreach uses `String.valueOf(p.get("assigneeId"))`, which would yield "null" if the key were absent; harmless (resolves to unknown assignee, logged) given the payload always carries the key.
- Spec's payload list should be updated to mention `title`.
