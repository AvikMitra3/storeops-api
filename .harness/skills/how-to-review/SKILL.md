# how-to-review

**Purpose:** the Evaluator's procedure for turning an LLM-generated diff into repeatable binary check results.

## Order of work
1. `git diff` + `git status` to list every changed/new file. Anything outside the contract's "Files expected" needs justification in `generator-summary.md` or it fails check R4.
2. Run the automated commands in `evaluation-criteria` and record exit codes and the printed numbers. Automated results are final; never overrule them with judgement.
3. Walk the checklist. Each check yields exactly PASS or FAIL.
4. Compute score and verdict by the rules in `evaluation-criteria`. Write `evaluator-feedback.md`.

## Converting variable output into a binary result
- **Cite evidence or it fails.** A check is PASS only if you can quote a `file:line`, a test name or a command output line. "Looks fine" is not evidence.
- **Search, don't skim.** Use grep for the concrete pattern named in the check (e.g. `grep -rn "repository" src/main/java/**/routes`, `grep -rn "throw new RuntimeException"`, `grep -rn "emit(" <service>`). Absence of matches is the evidence for negative checks.
- **Behaviour over wording.** Judge by what the test asserts and what the code does, not by names or comments. A test without an assertion for the AC does not prove it.
- **Same input, same verdict.** Do not weigh style preferences, naming taste or alternative designs; only the listed checks count. Extra observations go in a "Non-scoring notes" section.
- **Don't trust the Generator's self-check table.** Re-verify each row; a row marked Y with no passing test is FAIL for that AC.

## Ambiguity fallback
If a check cannot be decided after one targeted look (missing file, contradictory evidence, tool crash, unreadable output):
1. Re-run the command or search once.
2. If still undecidable: **result = FAIL** with reason `AMBIGUOUS` and the exact thing the Generator must make verifiable (e.g. "add a test asserting the event payload"). Ambiguity never rounds up.
3. If an automated tool cannot run (not a code failure), verdict is FAIL with reason `TOOLING` and the orchestrator re-runs the Evaluator once before counting an iteration.

## Feedback format for each failure
`<check id> | <file>:<line> | <what is wrong> | <required change>`. One line per issue, no prose paragraphs, so the Generator can act on it without any other context.
