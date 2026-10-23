# Build Journal

Decisions, trade-offs and insights from building the StoreOps delivery harness and the work around it. Entries are in the order they happened. Companion documents: `DESIGN_BRIEF.md` (the intended design), `REFLECTION.md` (one-page retrospective), `.harness/reviews/` (run evidence).

## 1. Starting point (2026-10-22)

**Found:** the repo already had the Spring Boot scaffold, the nine base endpoints, a `CLAUDE.md` with a short orchestration outline, four one-line agent stubs (since removed), and an uncommitted bulk-status implementation. It had no `.harness/agents`, no skill files, no evaluation framework and none of the demo artefacts.

**Decision:** treat the brief (`AI_Harness_Prompt.txt`) as the checklist and close the gaps instead of restarting. Keep the existing feature code; build the harness around it.

**Trade-off:** this makes the demonstration run *retroactive*. A real run would have the Planner and Generator produce the code. I chose to verify and document what existed rather than delete working code just to regenerate it. The cost is that the evidence chain (spec, contract, summary, verdict) was authored after the fact. Every artefact says so. This is the biggest weakness of the submission and is repeated in `REFLECTION.md`.

## 2. Path convention: `.harness/` over `harness/`

**Found:** the existing `CLAUDE.md` wrote handoffs to `harness/output/`, and `.gitignore` excluded `harness/output/*.md`. The brief says `.harness/` is used deliberately to stay apart from `.github/`.

**Decision:** standardise everything on `.harness/` (agents, skills, output, reviews) and make `.harness/agents/*.agent.md` the only agent definitions. The original one-line stubs were first kept as pointers, then deleted at the developer's request so there is one source of truth.

**Trade-off:** two places now mention an agent. A pointer file can go stale, but it contains one line ("follow `.harness/agents/<name>.agent.md`"), so there is nothing to drift. The alternative, duplicating content, was worse.

**Side effect:** the old `.gitignore` rule meant the handoff files would never have been committed, which defeats an audit trail. I removed it. Insight: *an audit trail that is gitignored is not an audit trail.*

## 3. Skill files grounded in the real code

**Decision:** write the seven skills only after reading the actual code (error classes, `RequestContexts`, `InMemoryEventBus`, the ArchUnit test, `pom.xml` thresholds, `checkstyle.xml`), so rules cite real class names and real numbers.

**Insight:** several of my first-draft statements were wrong when checked against the code, and I fixed them on review:
- I wrote the CI file as `docker-publish.yaml`; it is `docker-publish.yml`.
- I listed `TaskStatus` as "TODO, DONE, BLOCKED, ..." (it also has `IN_PROGRESS`).
- I described service tests as using in-memory repositories; `ServiceCoverageTest` uses Mockito mocks.

Lesson: skill files are read as authority by every agent, so a wrong fact in one is replicated into all generated code. Verifying skill content against the repo is the cheapest quality control in the system.

## 4. Evaluation framework design

**Decision:** three dimensions (Build and Test Integrity 40, Architecture Compliance 30, Requirement Fulfilment 30), binary checks, eight hard gates, and a fixed verdict formula (gate means FAIL; >=90 with all D3 passing means PASS; >=75 means CONDITIONAL PASS; else FAIL).

**Alternatives rejected:** holistic LLM grade (drifts); checklist with no weights (cannot separate cosmetic from structural).

**Trade-off:** the weights are a judgement call, not tuned from data. With one run there is nothing to tune against. I put the largest weight on what tools decide so the weak spot (LLM judgement) carries the least.

**Insight, found by applying my own rules:** the first draft of D3 had only judgement-based hard gates, while the brief strongly recommends an automated gate per dimension. I added check A5 (each acceptance criterion's named test must be present and passing in the Surefire XML) and made it feed D3-G1. I ran it against the real reports as part of the recorded evaluation.

**Insight:** writing the evaluator rules *before* grading exposed a real defect. Rule D1-6 requires an assertion on the emitted event, and the pre-existing bulk-status tests asserted nothing about the audit entry the feature requires. I added `bulkStatusEmitsOneAuditEventPerUpdatedTask` before recording a PASS. The harness's value showed up before it ran.

**Ambiguity policy:** an undecidable check is FAIL (`AMBIGUOUS`), never PASS. This will cause some avoidable retries; I accepted that because a lenient fallback silently turns the Evaluator back into a rubber stamp.

## 5. Verification and environment notes

- `mvn clean verify` at first pass: 14 tests, all green, coverage gates met. Later totals: 15 (audit test), 16 (docs test), 22 (extra ArchUnit rules).
- The Maven in use is 4.0.0-rc; it prints `Error while cleaning project local repository` for `.mvn/target/...` on every `clean`. It is harmless (build still succeeds) but it is noise, and I did not touch it.
- **Docker is not installed on the dev machine**, so the deployment evidence is a local jar run on port 18080, not a container. The user later deployed to a container on a server; that deployment is theirs, outside this repo's evidence.
- I once wrote a long shell heredoc that failed to parse, so nothing was written. Fix: use the file-writing tool for markdown artefacts rather than shell heredocs. Verify before assuming files exist.
- A stray `python` call (a Windows Store stub) hung a Maven command for the full timeout. Lesson: don't leave interpreters in shell one-liners on this machine; use awk/sed.

## 6. Production feedback from the deployed app

**`/docs` returned 404.** Cause: the project never had an API-docs library. I added springdoc (`springdoc-openapi-starter-webmvc-ui` 2.6.0, compatible with Spring Boot 3.3), mapped Swagger UI to `/docs` and the JSON to `/v3/api-docs`, and added a test.
- **Trade-off:** a new runtime dependency and a publicly browsable API surface on an HTTP-only public IP. I flagged it and gave the property switches to turn it off in production.
- **Surprise:** the change vanished from the working tree after the user's "Harness Changes" commit (the commit predated it and the files were reverted on disk). I re-applied it and said so rather than assuming it was intentional.

**`/api/activities/bulk-status` returned 404 in the browser.** Cause: a browser sends GET; the endpoint is PATCH-only; Spring matched the path to `GET /api/activities/{id}` with id `bulk-status`, giving "Activity not found". I reproduced it locally rather than guessing.
- **Insight:** the misleading message is a consequence of the `/{id}` route, not a bug in the new code. I offered a 405 instead and did not change behaviour unasked.
- **Insight:** a 400 after switching to PATCH is almost always a missing identity header (`X-User-Id`, `X-Store-Id`, `X-User-Role`) or an empty/invalid body. I documented the exact request body and rules for the user.

## 7. Checking the code against the API specification (`AI_API_Prompt.txt`)

**Found compliant:** all module entities and enum values, all nine endpoints, three-layer structure, the typed `AppError` hierarchy and the single cross-module call (programme service to staff service, a read-only lookup the spec allows).

**Found non-compliant, and fixed:**
- JaCoCo thresholds were below the specification (overall 70% vs 80% required; shared 60% vs 70%). Raised to 80% and 70%. I kept the service (80%) and routes (70%) thresholds above the spec's minimums because actual coverage (98.6% and 94.4%) leaves room and a looser value would only weaken the gate.
- `ArchitectureTest` covered three repository boundaries only. Added `alerts` and `reports` boundaries, "no direct dependency on alerts/reports services" (event-bus-only), "repositories do not call upward" and "reports never touches another module's repository". 4 tests became 10.

**Decision on a layered ArchUnit rule:** I did not add `layeredArchitecture()` (routes can only be called by nobody). Request DTOs live in each module's `routes` package and services import them, so the rule would fail on existing, intentional structure. Moving DTOs would be a refactor outside the brief. Trade-off: layer discipline is enforced by targeted rules rather than one strict layering statement.

**Left unbuilt on purpose:** the event bus records events but nothing subscribes to them, so the spec's examples (SLA_BREACH alert on an overdue CRITICAL task; STORE_SUMMARY report when a programme closes) do not exist. The spec lists them as examples of the mechanism, not required endpoints, and they are exactly the demo features (SLA alerting, regional rollup). I did not build them unasked.

## 8. Design Brief

I wrote `DESIGN_BRIEF.md` as the account of intent. The worked verdict example in Section C is built from the real audit-test gap, but the failing run itself was never recorded; the brief says so. It runs close to or over the 3-5 page limit when rendered, and may need trimming.

## 9. Open items and next steps

1. Run the harness live on a second feature (SLA breach alerting is the best candidate: it forces a two-sprint split and an event consumer) to produce a real multi-iteration audit trail and exercise the approval gate and retry loop.
2. Build `.harness/run-checks.sh` to run A1-A5, parse Surefire/JaCoCo and emit the score and `VERDICT:` line, removing the remaining assessor variability.
3. Make the audit trail durable: today the only audit record is an in-memory event, lost on restart.
4. Decide whether bulk update should be limited by role or ownership (currently any caller with a valid store scope can mark tasks DONE or BLOCKED).
5. Deployment: rebuild and redeploy the container so `/docs` and the new tests reach the server; consider disabling Swagger in production.
6. Capture a real deployment screenshot or container log; the current evidence is a pasted response from a local jar run.

## 10. What I would tell the next person

- The harness is only as good as its skill files. Check their facts against the code.
- Put every rule you can into the build, then mirror it in prose for the Generator. A rule that only exists in prose gets violated quietly.
- Be explicit about what was and wasn't actually exercised. The honest label on the retroactive run is worth more than a polished but untrue "end-to-end" claim.
