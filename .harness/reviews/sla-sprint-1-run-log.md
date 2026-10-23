# Run log: SLA sprint 1 (Due date, clock and SLA scan)

## Iteration 1
- Sprint ID: sla-sprint-1
- Verdict: PASS
- Iterations used: 1/3
- Escalation flag: no
- Score: D1 40/40, D2 30/30, D3 30/30 = 100

### Tests and coverage
- Tests: 32 run, 0 failures, 0 errors, 0 skipped (ArchitectureTest 10, ApiMockMvcTest 9, ServiceCoverageTest 12, StoreOpsApplicationTests 1).
- Checkstyle: 0 violations. JaCoCo: "All coverage checks have been met."
- Line coverage (Generator-reported, from jacoco.csv): bundle 93.2%, activities.service 98.6%, activities.routes 100%, shared.config 100%, shared.events 100%, shared.error 100%.
- Hard gates triggered: none (no raw Error/RuntimeException throws, no routes->repository imports, pom.xml unchanged).

### Estimated token cost
Estimate only: not measured. Method: no token telemetry is available to the Monitor, so cost is not quantified. One Generator and one Evaluator run, no retries, is the minimum cost profile for a sprint (roughly 1 Generator + 1 Evaluator + 1 Monitor invocation).

### Quality trend notes
- No failing check ids; no repeated failures, so no drift signal this iteration.
- Open follow-ups (non-blocking), disclosed by Generator and confirmed by Evaluator:
  - Spec error contract ("one task's failure must not abort the others in the scan") is not implemented, because coding-conventions forbid catching Exception/RuntimeException. Candidate for Sprint 3 (EventBus listener isolation) or a revisit of the scan loop. Skill file possibly needing refinement: coding-conventions (clarify a sanctioned pattern for per-item failure isolation) or the spec template (avoid contracts that conflict with the conventions).
  - Side effect: any unparseable JSON body now returns 400 VALIDATION_ERROR "Malformed request body".
  - InMemoryActivityRepository.findSlaCandidates lacks an explicit @Override (consistent with existing methods; minor Checkstyle/convention consideration).
