# StoreOps Autonomous Delivery Harness

This file is the orchestration brain. Claude Code reads it automatically. Harness files live under `.harness/` (not `.github/`) to keep them separate from CI/CD configuration.

## 1. Entry prompt
`RUN HARNESS: <feature request>; constraints: <optional>; acceptance notes: <optional>`
Short form: `@planner <feature request>`. The prompt is saved verbatim to `PROMPT.md` in the repo root.

## 2. Agents and how each is invoked
Each agent is a definition file in `.harness/agents/`, invoked as a fresh subagent (Agent tool) whose prompt is "Follow `.harness/agents/<name>.agent.md`" plus only the bounded inputs in section 5. `.claude/agents/*.md` are thin pointers so `@planner` etc. resolve.

| Agent | Definition | Reads (skills in `.harness/skills/<name>/SKILL.md`) | Writes (handoff) |
|---|---|---|---|
| Planner | `.harness/agents/planner.agent.md` | app-context, architecture-principles, sprint-decomposition | `.harness/output/spec.md` (ends `STATUS: AWAITING APPROVAL`), `.harness/output/sprint-N-contract.md` |
| Generator | `.harness/agents/generator.agent.md` | app-context, architecture-principles, coding-conventions, how-to-test | code + tests in `src/`, `.harness/output/generator-summary.md` |
| Evaluator | `.harness/agents/evaluator.agent.md` | architecture-principles, how-to-review, evaluation-criteria | `.harness/output/evaluator-feedback.md` (exactly one verdict) |
| Monitor | `.harness/agents/monitor.agent.md` | app-context | `.harness/reviews/<sprint>-run-log.md` |

## 3. Sequence
1. Developer sends the entry prompt. Save to `PROMPT.md`.
2. Invoke Planner. It writes `spec.md` with `STATUS: AWAITING APPROVAL` and the sprint contracts, then stops.
3. Do not generate any code until the developer types the standalone input `APPROVED`. On approval, change the marker in `spec.md` to `STATUS: APPROVED`.
4. For each sprint in order, run the Generator/Evaluator loop (section 4) autonomously. The developer is only re-engaged at an escalation.

## 4. Routing logic
After every Evaluator run, read the single `VERDICT:` line of `.harness/output/evaluator-feedback.md`:
- `PASS` -> copy `generator-summary.md` and `evaluator-feedback.md` to `.harness/reviews/<sprint>-*.md`, invoke Monitor, advance to the next sprint (or finish when none remain).
- `CONDITIONAL PASS` -> iteration += 1, re-invoke Generator against the listed conditions only.
- `FAIL` -> iteration += 1, re-invoke Generator against the blocking findings.
- Missing, malformed or multiple verdict lines -> treat as `FAIL` and re-run the Evaluator once; a second malformed result counts as an iteration.
- Maximum **3** Generator/Evaluator iterations per sprint. After the third non-PASS verdict, invoke Monitor (escalation flag = yes), write `.harness/output/escalation.md`, and stop.

`escalation.md` format:
```
# ESCALATION
Sprint: <id>
Iterations used: 3/3
Blocking issue: <one sentence>
Failed checks: <check ids from evaluator-feedback.md with file:line>
Recommended developer decision: <split sprint | relax criterion | fix manually | change skill file>
```

## 5. Context scoping
Every agent invocation starts with a fresh context; never carry chat history between agents. Persist decisions only in artefacts and git diffs.
- Planner gets: feature prompt, this file, repository tree.
- Generator gets: approved `spec.md`, the current `sprint-N-contract.md`, latest `evaluator-feedback.md` (if any), repository diff.
- Evaluator gets: approved `spec.md`, sprint contract, `generator-summary.md`, repository diff. It must not see Generator reasoning.
- Monitor gets: `evaluator-feedback.md`, `generator-summary.md`, iteration count.
Handoff files are overwritten each iteration; history is preserved by copying to `.harness/reviews/` on every verdict (`<sprint>-iter<N>-...`).

## 6. CI/CD relationship
The harness **precedes and feeds** CI; it does not replace it. The Evaluator runs the same `mvn clean verify` (Checkstyle, tests, ArchUnit, JaCoCo gates) locally, so CI should rarely fail on the same checks. CI stays authoritative for merge approval, environment-specific integration tests, security scanning and deployment (`.github/workflows/docker-publish.yml`). A harness PASS is evidence attached to the PR, not a merge approval.
