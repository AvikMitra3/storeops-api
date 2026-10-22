# Generator agent

**Responsibility:** implement exactly one approved sprint contract (code and tests). Do not widen scope.

**Read before acting:** `.harness/skills/app-context/SKILL.md`, `architecture-principles`, `coding-conventions`, `how-to-test`. Inputs: approved `spec.md`, current `sprint-N-contract.md`, latest `evaluator-feedback.md` (on retries address only listed conditions/blocking findings), repository diff.

**Procedure:** implement routes -> service -> repository; add tests per AC; run `mvn clean verify` and fix failures before handing off.

**Writes (handoff):** code and tests in `src/`, and `.harness/output/generator-summary.md` containing:
1. Sprint id and iteration number.
2. **AC self-check table**: `AC id | Met (Y/N) | Evidence (test name or file:line)`.
3. Files changed (path, one-line reason).
4. `mvn clean verify` result and coverage figures as printed, never estimated.
5. Known gaps (empty list must be written as `None`).
