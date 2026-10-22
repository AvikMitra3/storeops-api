# how-to-test

**Purpose:** what tests the Generator must write and the coverage thresholds the build enforces.

## Commands
- Full gate: `mvn clean verify` (must exit 0).
- Fast loop: `mvn -q test -Dtest=ApiMockMvcTest` or `-Dtest=ServiceCoverageTest`.
- Coverage report: `target/site/jacoco/index.html`.

## Thresholds (JaCoCo `check`, `pom.xml`, LINE covered ratio)
| Scope | Minimum |
|---|---|
| Whole bundle | 70% |
| `*.service` packages | **80%** |
| `*.routes` packages | 70% |
| `*.shared.*` packages | 60% |
A drop below any threshold fails `verify`; do not lower thresholds to pass.

## Test types and where they live
- Route/HTTP: `src/test/java/com/cognizant/storeops/routes/ApiMockMvcTest.java`, `@SpringBootTest` + `@AutoConfigureMockMvc`; its `headers(...)` helper adds the three identity headers. Assert status, JSON paths and error `code`.
- Service logic: `src/test/java/com/cognizant/storeops/services/ServiceCoverageTest.java` style: plain JUnit + Mockito unit tests that mock the repository interface and `EventBus`, call the service with a `RequestContext`, and `verify`/capture the emitted `DomainEvent`. This is what keeps `*.service` above 80%.
- Architecture: `ArchitectureTest` (ArchUnit). Add a rule when you introduce a new repository package.

## Requirements per sprint
1. Every AC in the contract has a test whose name is listed in its `Proof:` line.
2. For each endpoint: one happy path, one validation 400, one not-found/other-store case.
3. For state changes: assert the emitted `DomainEvent` type and payload.
4. Partial-failure paths: assert both `updated` and `failed` contents and order.
5. Tests are deterministic: no sleeps, no wall clock (inject a clock), no ordering dependence between tests. MockMvc tests share one Spring context and in-memory repositories, so create fresh ids per test and filter events by id.
