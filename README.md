# GitHub Organization Access Report

A Spring Boot service that connects to the GitHub REST API and generates a
report showing which users have access to which repositories within a
GitHub organization, exposed as a JSON API endpoint.

## What it does

1. Authenticates to GitHub using a Personal Access Token.
2. Lists every repository in the target organization (handles pagination).
3. For every repository, lists its collaborators and their permission level
   (handles pagination), fetched **concurrently across repositories** so it
   stays fast at scale (100+ repos, 1000+ users).
4. Aggregates the results into a `user -> [repositories + permission]` map.
5. Exposes this as `GET /api/access-report?org={org}`.

## Tech stack

- Java 17
- Spring Boot 3 (Web + WebFlux — WebFlux is used only for its reactive
  `WebClient`, to do the concurrent GitHub calls; the controller itself is a
  normal blocking REST controller)
- Maven

## How to run

### Prerequisites
- Java 17+ (`java -version`)
- Maven 3.9+ (or just use the included `mvnw` wrapper if you add one — this
  project assumes a system Maven install)
- A GitHub Personal Access Token (see [Authentication](#authentication))

### Steps

1. Open the project folder in VS Code (with the "Extension Pack for Java"
   extension installed), or any IDE / terminal.

2. Set your GitHub token as an environment variable:

   **macOS / Linux:**
   ```bash
   export GITHUB_TOKEN=YOUR_GITHUB_TOKEN
   ```

   **Windows (PowerShell):**
   ```powershell
   $env:GITHUB_TOKEN="YOUR_GITHUB_TOKEN"
   ```

3. Run the application:
   ```bash
   mvn spring-boot:run
   ```
   The app starts on `http://localhost:8080`.

4. Call the endpoint (see [How to call the API](#how-to-call-the-api-endpoint) below).

You can also run/debug it directly from VS Code by opening
`GithubAccessReportApplication.java` and clicking "Run" above the `main`
method — VS Code will pick up `GITHUB_TOKEN` if it's set in the same shell
session VS Code was launched from, or you can add it to a `launch.json`
`env` block.

## Authentication

The service authenticates to GitHub using a **Personal Access Token (PAT)**
sent as a Bearer token on every request (`Authorization: Bearer <token>`),
which is GitHub's currently recommended REST API auth scheme.

- The token is **never hard-coded**. It's read from the `GITHUB_TOKEN`
  environment variable, bound via `github.token` in
  `src/main/resources/application.yml`:
  ```yaml
  github:
    token: ${GITHUB_TOKEN:}
  ```
- If `GITHUB_TOKEN` is not set, the app fails fast on startup with a clear
  error instead of making doomed API calls.

### Token scopes needed
- **Fine-grained PAT**: Organization permissions → "Members" (Read-only)
  and repository permissions → "Administration" (Read-only) on the repos in
  the org (needed to list collaborators/permissions).
- **Classic PAT**: the `repo` scope (and `read:org` if the org restricts
  member visibility) is the simplest option for a personal/organization you
  admin.

For a production deployment, this could be swapped for a **GitHub App**
installation token (better for org-wide, non-personal auth) without
changing anything outside `GithubWebClientConfig` — that's the only class
that knows how the token is obtained.

## How to call the API endpoint

```
GET /api/access-report?org={organization-name}
```

Example:
```bash
curl "http://localhost:8080/api/access-report?org=octocat-inc"
```

Example response:
```json
{
  "organization": "octocat-inc",
  "generatedAt": "2026-08-23T10:15:30Z",
  "totalRepositories": 132,
  "totalUsers": 1042,
  "userAccess": {
    "octocat": [
      { "repository": "octocat-inc/backend-api", "permission": "admin" },
      { "repository": "octocat-inc/frontend-app", "permission": "write" }
    ],
    "hubot": [
      { "repository": "octocat-inc/backend-api", "permission": "read" }
    ]
  }
}
```

On failure (bad token, org not found, GitHub rate limit exhausted after
retries, etc.) the API returns a structured JSON error body with an
appropriate HTTP status instead of a stack trace, e.g.:
```json
{
  "timestamp": "2026-08-23T10:15:30Z",
  "status": 404,
  "error": "Not Found",
  "message": "GitHub organization or repository not found, or the token lacks access to it."
}
```

## Design decisions & assumptions

- **Concurrency, not raw parallel threads**: repository listing is
  paginated sequentially per page (each page depends on the previous one's
  `Link` header), but collaborator lookups for *different repositories* are
  fan-out with a bounded concurrency (`github.concurrency`, default `10`)
  using WebFlux's `Flux.flatMap(fn, concurrency)`. This avoids the
  "unnecessary sequential API calls" the assignment warns against, while
  still respecting GitHub's secondary rate limits (an unbounded fan-out
  across 1000+ users would trip those limits).
- **Pagination via the `Link` header**: rather than assuming a fixed page
  count, the app follows GitHub's RFC 5988 `Link: <...>; rel="next"` header
  until GitHub reports no more pages, so it's correct regardless of org
  size.
- **Retries with backoff**: GitHub returns `403`/`429` for both primary and
  secondary rate limiting. Failed requests are retried up to 3 times with
  exponential backoff (1s → 10s cap) before surfacing an error.
- **Resilience to partial failure**: if fetching collaborators for one
  specific repository fails after retries (e.g. it was deleted mid-run),
  that repository is skipped (and logged) rather than failing the entire
  report.
- **Permission representation**: GitHub's collaborators endpoint returns a
  `role_name` field (`admin`, `maintain`, `write`, `triage`, `read`, or a
  custom repository role) which is used directly as the reported
  permission string — this is simpler and more forward-compatible than
  manually deriving it from the boolean `permissions` map.
- **`affiliation=all`** is used when listing collaborators so the report
  includes users with access via direct membership, team membership, and
  org-wide (outside collaborator) access — matching "who has access", not
  just "who was explicitly added".
- **Archived/disabled repos** are still included by default (an org admin
  likely wants visibility into stale-repo access too); this is easy to
  change to filter them out in `GithubService.fetchOrganizationRepositories`
  if desired.
- **Scope**: the report is generated synchronously per request rather than
  cached/scheduled. For a very large org (thousands of repos) in a
  production system, this would be moved to a background job that
  periodically refreshes a stored report, with the endpoint just reading
  the latest cached snapshot — called out here as a natural next step
  rather than implemented, to keep the assignment's scope focused.
- **Testing**: given the time-boxed nature of this assignment, a unit test
  is included for the report DTO shape. A fuller test suite would add
  WireMock-based tests for `GithubService` (pagination + retry behavior)
  and `AccessReportService` (aggregation logic) without hitting the real
  GitHub API.

## Project structure

```
src/main/java/com/example/githubaccessreport/
├── GithubAccessReportApplication.java   # Spring Boot entry point
├── config/
│   ├── GithubProperties.java            # github.* configuration binding
│   └── GithubWebClientConfig.java       # Authenticated WebClient bean
├── controller/
│   └── AccessReportController.java      # GET /api/access-report
├── dto/
│   ├── RepositoryDto.java               # GitHub API repo response shape
│   ├── CollaboratorDto.java             # GitHub API collaborator response shape
│   ├── RepositoryAccess.java            # (repo, permission) pair
│   └── AccessReport.java                # Final aggregated API response
├── exception/
│   ├── GithubApiException.java
│   └── GlobalExceptionHandler.java      # Structured JSON error responses
└── service/
    ├── GithubService.java               # Pagination + concurrent GitHub calls
    └── AccessReportService.java         # Aggregation logic
```
