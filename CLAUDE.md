# StoreOps Autonomous Delivery Harness

## Entry prompt
`RUN HARNESS: <feature request>; constraints: <optional>; acceptance notes: <optional>`

## Agents
- `.claude/agents/planner.md`: writes `harness/output/spec.md` and sprint GIVEN/WHEN/THEN contracts, then writes `AWAITING APPROVAL` and stops.
- `.claude/agents/generator.md`: after the developer types `APPROVED`, implements one sprint and writes `harness/output/generator-summary.md`.
- `.claude/agents/evaluator.md`: runs `mvn clean verify`, architecture checks and requirement review, then writes `harness/output/evaluator-feedback.md` with exactly one verdict: `PASS`, `CONDITIONAL PASS`, or `FAIL`.
- `.claude/agents/monitor.md`: records each iteration in `.harness/reviews/<sprint>-run-log.md`.

## Orchestration
1. Invoke Planner using only the feature prompt, this file and current repository tree.
2. Planner produces the specification and `AWAITING APPROVAL`. Do not generate code before the standalone input `APPROVED`.
3. For each sprint, invoke Generator with only the approved spec, current sprint contract, latest evaluator feedback and repository diff.
4. Invoke Evaluator with only the approved spec, sprint contract, generator summary and repository diff.
5. `PASS`: invoke Monitor, then continue to next sprint. `CONDITIONAL PASS`: retry Generator against listed conditions. `FAIL`: retry Generator against blocking findings.
6. Maximum three Generator/Evaluator iterations per sprint. After the third non-PASS verdict, write `harness/output/escalation.md` naming the sprint, iteration count, blocking issue, failed checks and recommended developer decision, then stop.

## Context scoping
Start each agent invocation with a fresh context. Supply only the bounded inputs listed above. Never carry free-form chat history between agents. Persist decisions solely in output artefacts and git diffs.

## CI/CD
The harness precedes and feeds the existing CI pipeline; it does not replace pipeline gates. Evaluator uses the same Maven `verify` command locally, while CI remains authoritative for merge approval, environment-specific integration tests, security scanning and deployment policy.
