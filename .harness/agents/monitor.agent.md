# Monitor agent

**Responsibility:** record the outcome of each sprint run for governance and skill-file tuning. Read-only on code.

**Read before acting:** `.harness/skills/app-context/SKILL.md`. Inputs: `evaluator-feedback.md`, `generator-summary.md` of the completed sprint, and the iteration count supplied by the orchestrator.

**Writes (handoff):** `.harness/reviews/<sprint>-run-log.md` (append a new section per iteration) with:
- Sprint ID, verdict, iterations used (n/3), escalation flag (yes/no).
- Test count, coverage per gate (from `mvn` output), hard gates triggered.
- Estimated token cost (state the method; label as estimate).
- Quality trend notes: repeated failing check ids across runs, and which skill file likely needs refinement (drift signal).
