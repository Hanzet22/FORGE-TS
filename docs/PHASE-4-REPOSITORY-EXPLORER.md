# Phase 4 — Repository Explorer

> **Status context:** This document records phase scope, design assumptions or acceptance criteria and may describe work that was later changed. The live implementation state is summarized in the [development handoff](DEVELOPMENT-HANDOFF.md); verify current source and CI before claiming a feature is available.

## Delivered scope
- Search public GitHub repositories using the official REST API.
- Display repository name, description, star count, primary language and SPDX license identifier when available.
- Open the repository's canonical GitHub page in the device browser.
- Read-only implementation with bounded request timeout and a clear rate-limit error state.
- No GitHub account, personal access token or private repository access is required.

## Boundaries
- Search results are limited to the first 15 entries per query in this milestone.
- No repository writes, issue creation, pull requests, commits, downloads or private data access.
- GitHub search terms are transmitted to GitHub. Unauthenticated rate limits apply.
- A missing license identifier is displayed as "Not specified"; the app does not infer a license.

## Implementation
- `RepositoryExplorerRepository` owns the API request and JSON parsing.
- `RepositoryExplorerViewModel` owns loading, result and error state.
- `RepositoryExplorerScreen` renders read-only result cards.
- Tools navigation groups DNS & Protocols and Repository Explorer to preserve a compact five-item bottom navigation.
