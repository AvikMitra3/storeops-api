# Generator summary

Sprint: 4 (Escalation to STORE_MANAGER and hardening). Iteration: 1 of 3.

## AC self-check
| AC id | Met (Y/N) | Evidence |
|---|---|---|
| S4-AC1 | Y | `ApiMockMvcTest#slaEscalationNotifiesStoreManager`; `AlertService.notifyEscalation` |
| S4-AC2 | Y | `ApiMockMvcTest#noEscalationBeforeGraceOrWhenDone` |
| S4-AC3 | Y | `ServiceCoverageTest#escalationTargetsOnlySameStoreManager` (lookup keyed by event `storeId`) |
| S4-AC4 | Y | `ServiceCoverageTest#escalationWithoutManagerIsSkipped` |
| S4-AC5 | Y | `ServiceCoverageTest#unassignedTaskStillEscalates` |
| S4-AC6 | Y | `ApiMockMvcTest.ConfiguredGrace#escalationHonoursConfiguredGrace` (nested class with `@SpringBootTest(properties = "storeops.sla.grace-period=PT5M")`, as a property override needs its own Spring context) |
| S4-AC7 | Y | `docs/api-examples.md` section "SLA breach alerting"; `README.md` table of the three `storeops.sla.*` keys |

## Files changed (sprint 4)
- `src/main/java/com/cognizant/storeops/alerts/service/AlertService.java`: added `notifyEscalation` (ESCALATION, IN_APP, UNREAD to all STORE_MANAGERs of the event's store; skipped with a log line if none).
- `src/main/java/com/cognizant/storeops/alerts/service/SlaAlertListener.java`: subscribes `SLA_ESCALATION_DUE`.
- `src/test/java/com/cognizant/storeops/routes/ApiMockMvcTest.java`: AC1, AC2, AC6 tests.
- `src/test/java/com/cognizant/storeops/services/ServiceCoverageTest.java`: AC3, AC4, AC5 tests.
- `docs/api-examples.md`: dueDate on create and resulting alerts.
- `README.md`: SLA config keys.

## mvn clean verify
BUILD SUCCESS. Tests run: 47, Failures: 0, Errors: 0, Skipped: 0 (ApiMockMvcTest incl. nested, ServiceCoverageTest 22, StoreOpsApplicationTests 1, ArchitectureTest 10). "All coverage checks have been met."
JaCoCo line coverage from `target/site/jacoco/jacoco.csv`: bundle 275 covered / 13 missed = 95.5%; `alerts.service` 40/0 = 100%; `activities.service` 70/1 = 98.6%; `staff.service` 5/0 = 100%; `shared.events` 16/0 = 100%.

## Known gaps
None
