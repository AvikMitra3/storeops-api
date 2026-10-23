# architecture-principles

**Purpose:** the non-negotiable structural rules. Generator must obey them; Evaluator verifies them. Rules marked [AUTO] are enforced by `ArchitectureTest` (ArchUnit) or Checkstyle; the rest are reviewed.

## Layering: routes -> service -> repository
1. [AUTO] Classes in `..routes..` must not depend on `..repository..`. Controllers (`@RestController`) hold only header parsing, `@Valid` binding and a single service call.
2. Business rules, orchestration, event emission and authorization checks live in `service`. Services depend on repository *interfaces*, never on `InMemory*` classes.
3. Repositories: interface plus `InMemory<Name>Repository` implementation in `<module>.repository`. No business rules.
4. Models are immutable Java records in `<module>.model`. Request DTOs are records in `<module>.routes`.

## Module boundaries
5. [AUTO] Only the owning module may touch its `<module>.repository` package (activities, programmes, staff, alerts, reports each have an ArchUnit rule). No class outside `alerts`/`reports` may depend on `alerts.service`/`reports.service` (event bus only). Repositories may not depend on `service`/`routes`. `reports` may not depend on any other module repository (read-only).
6. A module may call another module's *service* or consume its *model*, never its repository. Prefer events for effects (notifications, reports).
7. `shared` must not depend on any feature module, except the existing `staff.model.StaffRole` use in `RequestContext`.

## Events
8. Cross-module side effects (alert on SLA breach, report on rollup) are emitted as `DomainEvent` through `EventBus`; the emitting service does not call the receiving module's repository.
9. One audit event per mutated task. Event type names are UPPER_SNAKE_CASE; payload values are Strings.

## Error contract
10. [AUTO] Never `throw new RuntimeException(...)` or `Error(...)` (Checkstyle regex fails the build). Use `ValidationError` (400, `VALIDATION_ERROR`), `NotFoundError` (404, `NOT_FOUND`), `ForbiddenError` (403, `FORBIDDEN`) or a new `AppError` subclass in `shared.error`.
11. `GlobalExceptionHandler` renders `ErrorResponse{code,message,status,timestamp}`; routes never build error bodies. Bean-validation failures also map to `VALIDATION_ERROR` 400.
12. Partial-failure endpoints (bulk operations) return 200 with `{updated:[...], failed:[{id, code, message}]}` where `code` is the `AppError` code; they never abort the batch for one bad item. A fully invalid envelope (empty list, over limit) is a 400.

## Reports
13. `reports` is read-only through HTTP: only `GET`. Report records are created by an event-bus consumer, never by a POST.

## Tenancy
14. Every read/write of a task checks `storeId == RequestContext.storeId()`; mismatch is `NotFoundError`.
