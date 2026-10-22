# app-context

**Purpose:** shared facts about StoreOps that every agent must know before acting.

## What it is
`storeops-api`: Spring Boot 3.3.5, Java 17, Maven. In-memory retail store-operations API (no database). Base package `com.cognizant.storeops`. Port 8080. Docker image `Dockerfile` (UBI9 OpenJDK 17, copies `target/*.jar`).

## Modules (one package each)
| Module | Owns | Notes |
|---|---|---|
| `activities` | `Task` (id, storeId, programmeId, title, status, priority, category, assigneeId, ownerId, createdAt), `/api/activities` | the "tasks" of the feature prompts |
| `programmes` | `Project`, `ProjectMember`, `/api/programmes` | store programmes |
| `staff` | `User`, `StaffRole`, `AuthToken` | roles: `REGIONAL_MANAGER, STORE_MANAGER, DEPARTMENT_LEAD, ASSOCIATE` |
| `alerts` | `Notification`, `AlertType`, `/api/alerts` | per-user notifications |
| `reports` | `Report`, `ReportType`, `ReportStatus` | **read-only** for API consumers; written only via event-bus reaction |
| `shared` | `auth` (`RequestContext`), `error` (`AppError` family, `GlobalExceptionHandler`), `events` (`EventBus`, `DomainEvent`) | no business logic |

Enums: `TaskStatus` (TODO, IN_PROGRESS, DONE, BLOCKED), `TaskPriority`, `TaskCategory` = `RESTOCKING, PLANOGRAM, AUDIT, COMPLIANCE, GENERAL`.

## Request identity
No real auth. Every request carries headers `X-User-Id`, `X-Store-Id`, `X-User-Role`; routes build `RequestContext` via `RequestContexts.of(...)`. A bad role gives `VALIDATION_ERROR` 400. Data is scoped by `storeId`; reading another store's task returns `NOT_FOUND` (never 403).

## Cross-module effects
Modules never call each other's repositories. Cross-module effects go through `EventBus.emit(new DomainEvent(type, Instant, Map payload))`. Existing event types: `ACTIVITY_CREATED`, `ACTIVITY_DELETED`, `ACTIVITY_STATUS_CHANGED` (payload: activityId, from, to, changedBy; this is the audit record).

## Build and verification
`mvn clean verify` = compile, Checkstyle, unit/MockMvc/ArchUnit tests, JaCoCo gates. This is the single source of truth locally. CI (`.github/workflows/docker-publish.yml`) remains authoritative for merge.

## Harness locations
Agents `.harness/agents/`, skills `.harness/skills/`, handoffs `.harness/output/`, archive `.harness/reviews/`.
