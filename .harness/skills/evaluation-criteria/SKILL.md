# evaluation-criteria

**Purpose:** the graded evaluation framework: dimensions, weights, hard gates, checklists and the deterministic verdict formula.

## Automated commands (run in repo root, in this order)
| Id | Command | Pass when |
|---|---|---|
| A1 | `mvn -B clean verify` | exit code 0 (covers compile, Checkstyle `check`, all tests, JaCoCo `check`) |
| A2 | read `target/surefire-reports/*.txt` | `Failures: 0, Errors: 0, Skipped: 0` in every file |
| A3 | `mvn -B -q test -Dtest=ArchitectureTest` | exit code 0 |
| A4 | JaCoCo gates from A1 output | line `All coverage checks have been met.` present (bundle 70%, service 80%, routes 70%, shared 60%) |
| A5 | `grep -c "<testcase name=\"<proof-method>\"" target/surefire-reports/TEST-*.xml` for each `Proof:` method | count >= 1 and no `<failure>`/`<error>` child (feeds D3-G1) |

## Dimensions and weights (sum = 100%)
| Dimension | Weight |
|---|---|
| D1 Build and Test Integrity | 40% |
| D2 Architecture Compliance | 30% |
| D3 Requirement Fulfilment | 30% |

Within a dimension every check carries equal weight. Dimension score = (passed checks / total checks) x weight. Total = sum of the three.

## Hard gates (any one triggers immediate FAIL regardless of score)
Deterministic: the verdict follows from the check result alone.
- **D1-G1** A1 exit code is not 0.
- **D1-G2** A2 shows any failed, errored or skipped test, or a new test class has zero assertions.
- **D1-G3** A4 coverage check not met (any threshold in how-to-test).
- **D2-G1** A3 fails (ArchUnit: repository boundary or routes-to-repository dependency).
- **D2-G2** `grep -rnE "throw new (Error|RuntimeException)\(" src/main` returns a match (also Checkstyle-enforced).
- **D2-G3** A `routes` class imports any `..repository..` type, or a service imports another module's repository.
- **D3-G1** (automated) Any AC `Proof:` test named in the sprint contract is absent from `target/surefire-reports/*.txt`/`TEST-*.xml` or not passing, or any AC has no verifiable evidence.
- **D3-G2** An endpoint, path or status code differs from the contract (e.g. wrong HTTP method or path).

## Checklists (each PASS/FAIL with cited evidence)
### D1 Build and Test Integrity
- D1-1 A1 passes. D1-2 A2 clean. D1-3 A4 coverage met. D1-4 Every AC has a named test from the contract's `Proof:` line and it exists. D1-5 Each new endpoint has happy-path, 400 and not-found/other-store tests. D1-6 State changes have an assertion on emitted event type. D1-7 No test depends on sleep/wall clock. D1-8 `generator-summary.md` reports the same test counts as the A2 output.

### D2 Architecture Compliance
- D2-1 A3 passes. D2-2 No routes-to-repository import. D2-3 Business logic only in service (no conditionals on domain state in routes). D2-4 Only typed `AppError` subclasses thrown. D2-5 Cross-module effects use `EventBus`. D2-6 Models and DTOs are records; services use constructor injection. D2-7 Reports remain read-only over HTTP. D2-8 Store scoping (`storeId`) enforced on every task read/write. D2-9 No new dependency in `pom.xml` unless in the contract.

### D3 Requirement Fulfilment
- D3-1..D3-n One check per AC: PASS only if the code path exists AND the proving test asserts the THEN clause. D3-R1 Error codes/status match the error contract in spec. D3-R2 Partial-failure shape `{updated, failed}` honoured where specified. D3-R3 No out-of-scope changes (files outside "Files expected" are justified). D3-R4 `generator-summary.md` has AC table, files changed, known gaps and no claim contradicted by evidence.

## Verdict formula (fixed)
1. If any hard gate triggered -> `FAIL`.
2. Else if total >= 90 and every D3 check PASS -> `PASS`.
3. Else if total >= 75 -> `CONDITIONAL PASS` (list each failed check as a condition; all must be fixable without changing the contract).
4. Else -> `FAIL`.
Output exactly one `VERDICT:` line. No other verdict text may appear on a line starting with `VERDICT`.

## Handling LLM variability
- Convert everything to binary via evidence (see how-to-review). Automated checks (A1-A4) decide D1-1..3, D2-1 and the corresponding gates, so assessor leniency cannot alter them.
- Ambiguous result => FAIL with reason `AMBIGUOUS` (never PASS). Tool crash => `TOOLING` re-run once.
- The score is computed from the table, not estimated; list the arithmetic in the feedback file.
- CONDITIONAL PASS is only reachable with zero hard gates, so retries always have a concrete, finite list to fix.
