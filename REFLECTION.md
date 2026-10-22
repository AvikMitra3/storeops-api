# Reflection

**Did well.** The skill files encode project-specific rules (ArchUnit boundaries, the `AppError` contract, JaCoCo thresholds per package), so most Evaluator checks are commands with numeric pass criteria rather than opinion. Writing the evaluator rules first exposed a real gap: nothing asserted the per-task audit event. I fixed it with a test.

**Fell short.** This demo run is retroactive. The feature code existed before the harness files, and one Claude session wrote the spec, contract, summary and verdict afterwards. It did not exercise fresh-context subagents, the approval gate or the retry loop (1 of 3 iterations used, no FAIL path observed). Token cost is an estimate, not telemetry. Docker was unavailable, so deployment was a local jar run. The audit trail is only the in-memory event bus.

**One improvement.** Add a script (`.harness/run-checks.sh`) that runs A1-A4, parses surefire and JaCoCo output, and emits the score and `VERDICT:` line, leaving the LLM only the D2/D3 judgement checks. That removes the remaining assessor variability and makes a genuine fresh-context run replayable.
