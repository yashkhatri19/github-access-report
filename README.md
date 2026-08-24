# GitHub Organization Access Report

A Spring Boot service that connects to the GitHub REST API and generates a report showing which users have access to which repositories within a GitHub organization, exposed as a JSON API endpoint.

## What it does

1. Authenticates to GitHub using a Personal Access Token.
2. Lists every repository in the target organization (handles pagination).
3. For every repository, lists its collaborators and their permission level (handles pagination), fetched **concurrently across repositories** so it stays fast at scale (100+ repos, 1000+ users).
4. Aggregates the results into a `user -> [repositories + permission]` map.
5. Exposes this as `GET /api/access-report?org={org}`.

## Tech stack

- Java 17+
- Spring Boot 3 (Web + WebFlux — WebFlux is used for its reactive `WebClient` to handle concurrent GitHub API calls)
- Maven

## How to run

### Prerequisites
- Java 17 or higher (`java -version`)
- A GitHub Personal Access Token (PAT)

### Steps

1. Clone the repository:
   ```bash
   git clone <YOUR_GITHUB_REPOSITORY_URL>
   cd github-access-report