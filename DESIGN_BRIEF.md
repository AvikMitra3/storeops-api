# StoreOps Autonomous Delivery Harness: Design Brief

Feature used for the demonstration: **shift handover bulk update** (`PATCH /api/activities/bulk-status`).
Stack: Spring Boot 3.3.5, Java 17, Maven; verification gate `mvn clean verify` (Checkstyle, JUnit/MockMvc, ArchUnit, JaCoCo).

**Honesty note.** The bulk-status code existed in the working tree before the harness files were written. The demonstration artefacts in `.harness/` were produced afterwards by following the harness procedure against that code, in a single session. The automated checks quoted below were really executed; the separation of Planner, Generator and Evaluator into fresh-context agents, the approval gate and the retry loop are designed and documented but were not exercised live. Section D and `REFLECTION.md` say what that costs.

```
 developer prompt ──> PLANNER ──> spec.md + sprint-N-contract.md   "STATUS: AWAITING APPROVAL"
                                   │  developer types APPROVED
                                   ▼
            ┌──────────────> GENERATOR ──> src/ + generator-summary.md
            │                      │
   retry (<=3 per sprint)          ▼
            │                  EVALUATOR ──> evaluator-feedback.md  (one VERDICT: line)
            │                      │
            └── CONDITIONAL/FAIL ◄─┤
                                   ├── PASS ──> MONITOR ──> .harness/reviews/<sprint>-run-log.md ──> next sprint
                                   └── 3rd non-PASS ──> .harness/output/escalation.md ──> developer
```

---

## Section A: Intent Decomposition

**From intent to responsibilities.** The one-line request is split by *who may decide what*. The Planner decides scope and acceptance criteria and may not touch `src/`. The Generator implements exactly one contract and may not widen it. The Evaluator judges against the contract and may not edit code. The Monitor only records. Each role reads a different input set, so no agent can grade its own work or quietly rewrite the requirement.

**Sprint boundaries.** `sprint-decomposition/SKILL.md` limits a sprint to one module's routes, service and repository, at most one `shared` change, and at most three endpoints or one event flow. A sprint must also leave `mvn clean verify` green and the app runnable. The bulk-update feature touches only the `activities` module and adds one endpoint plus one event flow, so it is **one sprint**. Splitting it would have produced a first sprint (endpoint without audit) that is not shippable, because the brief makes the audit entry part of the feature. For the other suggested features the skill predicts a split at the module boundary: SLA alerting becomes S1 (clock plus detection in `activities`) and S2 (alert consumer in `alerts` via the event bus), because S2 crosses a module boundary and needs its own architecture checks.

**Making criteria testable.** Every acceptance criterion (AC) has an ID, a GIVEN state that names headers, role and store, a WHEN that is one HTTP call, and a THEN made only of observable facts: status code, JSON fields, error `code`, emitted event payload. Each AC ends with a `Proof:` line naming the test that must exist. "Handles errors gracefully" is not allowed; "`failed` has 2 items in order `NOT_FOUND`, `VALIDATION_ERROR`" is. The Evaluator can then check an AC mechanically: the named test exists in the Surefire report, passes, and asserts the THEN clause.

**Example contract entry (from `.harness/output/sprint-1-contract.md`):**

```
### S1-AC3 One audit event per updated task
GIVEN activity A in TODO
WHEN PATCH /api/activities/bulk-status with [A -> DONE]
THEN 200, `failed` empty, exactly one ACTIVITY_STATUS_CHANGED event with
     activityId=A, from=TODO, to=DONE, changedBy=caller
Proof: ApiMockMvcTest#bulkStatusEmitsOneAuditEventPerUpdatedTask
```

The sprint has four ACs: partial failure (S1-AC1), invalid envelope (S1-AC2), audit event (S1-AC3) and architecture compliance (S1-AC4). At least one is negative and one covers the event or audit side, as the skill requires.

---

## Section B: Governance Framework

**Skill file strategy.** Seven plain-markdown files in `.harness/skills/<name>/SKILL.md`, grouped by who needs them:

| Skill | Read by | What it encodes that is specific to StoreOps |
|---|---|---|
| `app-context` | Planner, Generator, Monitor | Five domain modules plus `shared`; the `X-User-Id/X-Store-Id/X-User-Role` identity headers; storeId tenancy (another store's task is `NOT_FOUND`, never 403); the existing event types |
| `architecture-principles` | Planner, Generator, Evaluator | Routes to service to repository; per-module repository boundaries; event bus only for cross-module effects; `AppError` contract; read-only `reports`; partial-failure response shape. Rules are tagged **[AUTO]** where ArchUnit or Checkstyle already enforces them |
| `sprint-decomposition` | Planner | Sizing rules and the contract format; hints for the four suggested features |
| `coding-conventions` | Generator | Records for models/DTOs, constructor injection, header-to-`RequestContext` pattern, literal paths declared so `/bulk-status` is not shadowed by `/{id}` |
| `how-to-test` | Generator | JaCoCo thresholds per package, where MockMvc and Mockito tests live, determinism rules |
| `how-to-review` | Evaluator | How to turn a diff into binary results (cite evidence or fail) and the ambiguity fallback |
| `grading-criteria` | Evaluator | Dimensions, weights, hard gates, checklists, verdict formula, and the exact commands |

Two files are shared by several agents (`app-context`, `architecture-principles`) because the Planner must not plan something the architecture forbids, the Generator must build to it, and the Evaluator must test the same text. Keeping one copy prevents the three agents drifting apart. The Evaluator deliberately does *not* read `coding-conventions`: style preferences must not become verdict inputs.

**`.harness/reviews/` as audit trail.** On every verdict the orchestrator copies the generator summary and evaluator feedback there, and the Monitor writes `<sprint>-run-log.md`. A run log records sprint ID, verdict, iterations used (n/3), escalation flag, test and coverage figures, hard gates triggered, an estimated token cost, and quality-trend notes. It is committed to git, so access equals repository access (developers, reviewers, CI), history is tamper-evident through commits, and a PR reviewer can read the chain contract, generator summary, evaluator verdict in one folder. A recurring problem shows up as the same check ID failing across run logs, for example D2-8 (store scoping) failing in three sprints. The Monitor's note then points at the skill file that needs a clearer rule. In this repository the archive holds only one run, so no recurring-issue signal exists yet; the mechanism is designed, not demonstrated.

**One rule that encodes a StoreOps decision (`architecture-principles`, rule 5):**
> No class outside `alerts` or `reports` may depend on `alerts.service` or `reports.service`. Cross-module effects are raised with `EventBus.emit(new DomainEvent(...))`.

It is enforced by the ArchUnit tests `ALERT_SERVICE_NOT_CALLED_DIRECTLY` and `REPORT_SERVICE_NOT_CALLED_DIRECTLY`. *Without it*, a Generator implementing SLA alerting would take the shortest path: inject `AlertService` into `ActivityService`. That creates a compile-time link from `activities` to `alerts`, makes activity tests need alert wiring, risks circular dependencies once alerts read tasks, and makes the later regional-rollup flow (a report created from an event) impossible to add without touching `activities`. The error would compile, pass unit tests and look reasonable to a reviewer, which is why it is a build-failing rule rather than a guideline.

---

## Section C: Non-Determinism Strategy

**Dimensions and weights.**

| Dimension | Weight | Why for StoreOps |
|---|---|---|
| D1 Build and Test Integrity | 40% | StoreOps has strict, tool-checkable gates (Checkstyle, JaCoCo per package, ArchUnit). The largest weight goes to what a tool can decide, which least depends on assessor mood |
| D2 Architecture Compliance | 30% | Module boundaries and the event bus are the project's defining constraints and the most common place for LLM code to "work but violate" the design |
| D3 Requirement Fulfilment | 30% | Correct code that does not meet the contract is still wrong. Weighted below D1 because it needs more judgement |

Weights sum to 100. Within a dimension every check weighs the same and is strictly PASS or FAIL.

**Hard gates (any one forces FAIL regardless of score).**

| Gate | Failure mode it prevents | Why not a soft check |
|---|---|---|
| D1-G1 `mvn clean verify` exits non-zero | Shipping code that does not compile, breaks Checkstyle or misses a coverage rule | A broken build cannot be offset by points elsewhere |
| D1-G2 any failed, errored or skipped test, or a test class without assertions | "Green" built from skipped or empty tests | A skipped test is an unverified AC; partial credit would reward it |
| D1-G3 JaCoCo threshold missed | Untested business logic in the service layer (80% required) | A tool number, with no interpretation to average |
| D2-G1 ArchUnit fails | Cross-module repository access, direct service coupling, upward calls from repositories | Boundary violations compound; they are cheap now and expensive later |
| D2-G2 raw `throw new Error/RuntimeException` | Bypassing the `AppError` contract so the API returns inconsistent error bodies | The error contract is a public API promise |
| D2-G3 route imports a repository, or a service imports another module's repository | Layer skipping | Same as D2-G1, caught by grep when ArchUnit is not applicable |
| D3-G1 an AC's `Proof:` test is absent from the Surefire XML or failing | An AC claimed in the summary but never proven | The Generator's self-check cannot be trusted as evidence |
| D3-G2 endpoint path, method or status differs from the contract | A client-visible contract change | Consumers break at runtime |

Each dimension has at least one gate backed by a tool command (D1: Maven; D2: ArchUnit; D3: Surefire XML lookup, command A5), so the verdict does not rely on the LLM for those.

**Same results, same verdict.** The formula is fixed: (1) any hard gate, FAIL; (2) else total >= 90 and every D3 check PASS, PASS; (3) else total >= 75, CONDITIONAL PASS; (4) else FAIL. The Evaluator computes the score from the check table and writes the arithmetic into the feedback; it may not override the formula.

*Worked example.* Suppose a Generator run implements the endpoint and the audit event correctly but writes no test asserting the event. The Evaluator reads the output as binary facts:
- A1 `mvn clean verify`: exit 0, PASS. A4 coverage: met, PASS.
- A5: `bulkStatusEmitsOneAuditEventPerUpdatedTask` is not in `TEST-*.xml`, so **D3-G1 triggers**.
- D1-6 (state change asserts event) fails, so D1 scores 7/8 = 35.0. D2 is 9/9 = 30.0. D3 has AC3 unproven, 7/8 = 26.25.
- Total 91.25, which would pass the 90 line, but a hard gate fired, so rule 1 applies: **FAIL**, with the feedback line `D1-6 | ApiMockMvcTest.java | no assertion on ACTIVITY_STATUS_CHANGED | add test asserting type and payload`.

A different phrasing of the same code, or a different assessor run, hits the same grep and the same XML lookup and produces the same verdict. This is not hypothetical: this exact gap existed in the repository and I found it by applying D1-6; the added test is what made the recorded run PASS (100.0 points, no gates, `.harness/reviews/sprint-1-evaluator-feedback.md`).

*Variability and ambiguity.* The Evaluator must cite a `file:line`, test name or command output for every PASS, uses grep for negative checks, ignores naming and style taste, and re-verifies each row of the Generator's self-check table. If a check cannot be decided after one targeted retry, the result is FAIL with reason `AMBIGUOUS`; if a tool cannot run, FAIL with reason `TOOLING` and the orchestrator re-runs once before counting an iteration. Ambiguity therefore never rounds up.

**Escalation path.** The orchestrator allows at most **three** Generator/Evaluator iterations per sprint. After the third non-PASS verdict it invokes the Monitor with the escalation flag set and writes `.harness/output/escalation.md`:

```
# ESCALATION
Sprint: <id>
Iterations used: 3/3
Blocking issue: <one sentence>
Failed checks: <check ids with file:line>
Recommended developer decision: <split sprint | relax criterion | fix manually | change skill file>
```

The recipient is the developer who approved the spec; the run stops and nothing further is generated. Each retry receives only the listed conditions or blocking findings, so three attempts are finite and targeted. A missing or malformed verdict line is treated as FAIL (the Evaluator re-runs once) so a formatting error cannot stall or accidentally pass a sprint.

---

## Section D: Architectural Decisions

**1. Verdicts come from a fixed formula plus tool outputs, not from the Evaluator's overall judgement.**
*Alternatives:* a single LLM "grade this 1-10" review; a pure pass/fail checklist with no weights.
*Rationale:* a holistic grade drifts between runs and between assessor moods; a pure checklist cannot distinguish "one cosmetic miss" (CONDITIONAL PASS) from "architecture violated" (FAIL). Weighted binary checks with deterministic hard gates give repeatable outcomes while still allowing a retry path.
*Assumption:* the important quality properties can be expressed as commands or greppable facts. If a future feature needs behaviour that no test can express (for example UX wording), the formula will need a human-reviewed dimension.

**2. Architecture rules live in the build (ArchUnit and Checkstyle) as well as in skill text.**
*Alternatives:* skill files only and an LLM check; a separate dependency analyser outside Maven.
*Rationale:* a rule written only in prose is followed by the Generator most of the time and noticed by the Evaluator some of the time; a failing test is followed every time. Running inside `mvn clean verify` means the same check is used by the Evaluator locally and by CI, so the harness feeds the pipeline instead of competing with it. Skill files still carry the rule so the Generator avoids the violation instead of discovering it after a failed build.
*Assumption:* the project stays a single Maven module with package-per-module layout, so package-based ArchUnit rules map to module boundaries. A multi-module split would move the rules to Maven module dependencies.

**3. Context is reset between agents; handoffs are files, not chat.**
*Alternatives:* one long conversation with all four roles; a shared scratch memory.
*Rationale:* a Generator that has seen the Evaluator's reasoning, or an Evaluator that has seen the Generator's, starts agreeing with it. Bounded inputs (spec, current contract, latest feedback, diff) keep each prompt small, make runs replayable from git, and give the audit trail for free because the handoff files are the archive. Cost: each agent re-reads the skills on every invocation.
*Assumption:* the artefacts are complete enough to carry all decisions. If the Generator needs context that only existed in discussion, the escalation path (`change skill file` or `split sprint`) is how that gap is closed.

**What I would do next.** Add a `run-checks` script that runs A1 to A5, parses Surefire and JaCoCo, and prints the score and `VERDICT:` line, so only D2 and D3 judgement checks remain with the LLM, and then run the full loop live on a second feature (SLA breach alerting) to produce a real multi-iteration audit trail.
