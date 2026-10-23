# Run log: SLA sprint 2 (Staff lookup by store and role)

## Iteration 1
- Sprint ID: sla-sprint-2
- Verdict: PASS
- Iterations used: 1/3
- Escalation flag: no
- Score: D1 40/40, D2 30/30, D3 30/30 = 100

### Tests and coverage
- Tests: 35 run, 0 failures, 0 errors, 0 skipped (ApiMockMvcTest 9, ServiceCoverageTest 15, StoreOpsApplicationTests 1, ArchitectureTest 10). Up from 32 in sprint 1 (+3 sprint 2 proof tests).
- Checkstyle: 0 violations. JaCoCo: "All coverage checks have been met."
- Line coverage (Generator-reported, from jacoco.csv, covered/total): bundle 226/242 (93.4%), staff.service 5/5, staff.repository 11/11, activities.service 70/71, alerts.service 4/4.
- Hard gates triggered: none.

### Estimated token cost
Estimate only: not measured. Method: no token telemetry is available to the Monitor, so cost is not quantified. One Generator, one Evaluator and one Monitor invocation, no retries (minimum cost profile for a sprint).

### Quality trend notes
- No failing check ids; no repeated failures, so no drift signal this iteration. Two consecutive first-iteration PASSes (sla-sprint-1, sla-sprint-2).
- Tooling note: a OneDrive file lock caused a non-fatal "Error while cleaning project local repository" during `mvn clean`, and one Evaluator run hit a clean file-lock before a clean re-run. Not a skill-file issue; environmental (repo under OneDrive). Possible mitigation: add a how-to-test note to re-run on this lock, or move the repo out of OneDrive.
- Non-blocking observations from Evaluator: cosmetic import ordering in StaffRepository.java (Checkstyle reports 0 violations); ServiceCoverageTest accumulates tests across sprints in one file, so per-sprint attribution relies on method names.
- Carry-over from sprint 1 (still open, not addressed here): per-task failure isolation in the SLA scan conflicts with the coding-conventions ban on catching Exception/RuntimeException. Skill file possibly needing refinement: coding-conventions (sanctioned isolation pattern) or the spec template.
