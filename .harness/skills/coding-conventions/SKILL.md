# coding-conventions

**Purpose:** match the existing StoreOps code style so Generator output is indistinguishable from hand-written code.

## Language and style
- Java 17, 4-space indent, **no tabs** (Checkstyle `FileTabCharacter` fails the build). Max line ~120.
- Models and DTOs are `record`s; nested records for sub-structures (see `BulkStatusRequest.Item`, `BulkStatusResult.Failure`).
- Constructor injection only; fields `private final`. No Lombok, no field `@Autowired`.
- Annotate services `@Service`, in-memory repositories `@Repository`, event bus `@Component`; controllers `@RestController` with `@RequestMapping("/api/<module>")`.
- Name patterns: `<Module>Routes`, `<Module>Service`, `<Module>Repository`, `InMemory<Module>Repository`, `<Verb><Noun>Request`.

## Routes
- Take the three identity headers as existing routes do: `@RequestHeader("X-User-Id")`, `X-Store-Id`, `X-User-Role`, then `RequestContexts.of(u, s, r)`.
- Use `@Valid @RequestBody` with Jakarta constraints on the request record (`@NotBlank`, `@NotEmpty`, `@Size(max=100)`, `@NotNull`). Return the model or `ResponseEntity` with the right status (201 on create, 204 on delete).
- Declare literal paths (`/bulk-status`) so they are not shadowed by `/{id}`.

## Services
- Take `RequestContext` as first parameter. Throw only typed `AppError` subclasses (see architecture-principles rule 10).
- Emit one `DomainEvent` per state change after the repository save succeeds.
- Do not catch `Exception`/`RuntimeException`; catch `AppError` when converting to per-item failures.
- Update immutable records by constructing a new record, then `repository.save`.

## Do / Don't
- Do reuse `get(c, id)` for store-scoped lookups. Don't duplicate tenancy checks.
- Don't add dependencies to `pom.xml` unless the contract says so.
- Don't leave TODOs, commented-out code or unused imports. Prefer explicit imports in new files (existing wildcard imports may stay).

## Config
New tunables go in `src/main/resources/application.yml` under `storeops.*` and are read via `@Value` or a `@ConfigurationProperties` record with a safe default.
