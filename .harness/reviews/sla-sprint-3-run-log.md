# Run log: SLA sprint 3 (EventBus subscription and SLA_BREACH notification)

## Iteration 1
- Sprint ID: sla-sprint-3
- Verdict: PASS
- Iterations used: 1/3
- Escalation flag: no
- Score: D1 40/40, D2 30/30, D3 30/30 = 100

### Tests and coverage
- Tests: 41 run, 0 failures, 0 errors, 0 skipped (ApiMockMvcTest 11, ServiceCoverageTest 19, StoreOpsApplicationTests 1, ArchitectureTest 10). Up from 35 in sprint 2 (+6: 3 ServiceCoverageTest S3 proofs plus the breachListenerNotifiesLead supporting test, 2 ApiMockMvcTest proofs; counts per surefire).
- Checkstyle: 0 violations. JaCoCo: "All coverage checks have been met."
- Line coverage (Generator-reported, from jacoco.csv): total 95.3%, service 99.2%, routes 94.4%, shared.events 100%, shared.config 100%.
- Hard gates triggered: none. Ambiguity fallbacks used: none.

### Estimated token cost
Estimate only: not measured. Method: no token telemetry is available to the Monitor, so cost is not quantified. One Generator, one Evaluator and one Monitor invocation, no retries (minimum cost profile for a sprint).

### Quality trend notes
- No failing check ids; no repeated failures, so no drift signal. Third consecutive first-iteration PASS (sla-sprint-1, -2, -3).
- Resolved carry-over: the per-listener failure isolation in InMemoryEventBus catches RuntimeException (required by S3-AC2). The Evaluator accepted it as a `catch` that logs, not a `throw new RuntimeException`. The sprint 1/2 open question about catching Exception/RuntimeException versus coding-conventions is still not codified. Skill file likely needing refinement: coding-conventions (document the sanctioned log-and-isolate pattern for listeners and scans).
- Spec drift: the SLA event payload gained a `title` key (disclosed by Generator, accepted by Evaluator). The spec's payload list should be updated. Skill/template needing refinement: spec template (sprint-decomposition) to list payload fields needed by downstream messages.
- Non-blocking Evaluator note: AlertService.notifySlaBreach uses `String.valueOf(p.get("assigneeId"))`, which yields "null" if the key is absent; harmless today since the payload always carries the key.
- Known open item for sprint 4: `SLA_ESCALATION_DUE` has no listener yet (ESCALATION handling not implemented).
- Environmental: no OneDrive file-lock issue this run (clean worked).
