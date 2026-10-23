# StoreOps API Status, GitHub, and Test Coverage Report

Date: 2026-10-23
Hosted application: http://20.41.223.214:80

## 1. GitHub and repository details

- Repository remote: https://github.com/AvikMitra3/storeops-api.git
- Current branch: Harness_Test
- Repository status: the local branch is tracking origin/Harness_Test and contains modified project files from the active harness work.
- Source location: the workspace is the project root for the StoreOps API service.

## 2. Endpoint result status

The live application was checked with HTTP requests against the deployed service.

| Endpoint | HTTP result | Observed behavior |
| --- | --- | --- |
| /v3/api-docs | 200 OK | OpenAPI JSON was returned successfully. |
| /docs | 200 OK after redirect | Swagger UI endpoint responds and redirects to /swagger-ui/index.html. |
| /api/activities | 200 OK | Returned an empty JSON array [] when called with the required headers. |
| /api/alerts | 200 OK | Returned an empty JSON array [] when called with X-User-Id. |
| /api/programmes | 400 Bad Request | Returned a validation error when called without the expected required input/context. |

### Notes on endpoint behavior

- The public API is reachable and serving responses on the hosted environment.
- The activity and alert endpoints are returning successful responses with valid headers.
- The programmes endpoint is not a no-arg GET that works without context; it expects a valid request body or route-specific request shape.
- The Swagger OpenAPI docs are available and confirm the API is exposed at the deployed host.

## 3. Test coverage status

The project is configured with JaCoCo in pom.xml and defines minimum coverage thresholds:

- BUNDLE: 80% line coverage minimum
- service package: 80% line coverage minimum
- routes package: 70% line coverage minimum
- shared.* package: 70% line coverage minimum

### Current verification result

A fresh verification run was executed with:

```bash
mvn test jacoco:report
```

Observed result:

- Maven reported BUILD SUCCESS.
- However, the run output also showed:
  - "No tests to run."
  - "Skipping JaCoCo execution due to missing execution data file."

This means the project is configured for coverage enforcement, but the current run did not produce usable coverage data because no tests were executed during that verification. As a result, there is no fresh measured percentage to report from the current run.

### Available tests in the project

The repository contains test classes such as:

- ApiMockMvcTest
- ServiceCoverageTest
- ArchitectureTest

These indicate the project intends to validate route behavior, service logic, and architectural constraints, but the latest execution did not generate a valid JaCoCo report in this session.

## 4. Summary

- The hosted application at http://20.41.223.214:80 is responsive and serving the API and docs endpoints.
- The GitHub repository is available at https://github.com/AvikMitra3/storeops-api.git on the Harness_Test branch.
- The app is configured with JaCoCo coverage gates, but the current verification did not execute tests and therefore no reliable coverage percentage was generated in this run.

## 5. Recommended next check

To establish a verified coverage percentage, the project should be run in a clean environment where tests are actually discovered and executed, followed by a fresh JaCoCo report generation step.
