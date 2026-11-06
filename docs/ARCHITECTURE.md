# FORGE-TS Architecture — Phases 0–4

## Operating model
Local-first, online and offline. Local utilities and reference content remain usable without connectivity. Network-backed features disclose their provider and use bounded requests.

## Layers
- Presentation: Jetpack Compose and Material 3.
- State: MVVM / ViewModels.
- Data: repositories coordinating local storage and Ktor.
- Remote boundary: Supabase behind an interface; no backend secrets in the Android client.
- Background work: WorkManager for future opt-in synchronization.

## Data responsibilities
- Room: planned structured offline cache and local records.
- DataStore: planned lightweight preferences.
- Supabase: future remote source of truth for shared catalog data after a dedicated FORGE-TS project and RLS design exist.
- Upstash Redis: optional future cache, deferred.

## Phase 1 — Core Networking
- CIDR calculation is local-only and supports IPv4 and IPv6.
- Public IP lookup uses HTTPS and identifies the provider. The request exposes the connection's public IP to that provider; FORGE-TS does not maintain IP history.
- Host Checker performs one DNS resolution and one user-selected TCP port check. Optional TLS checks use platform certificate and hostname verification.
- Network operations run away from the main thread and use bounded timeouts.

## Phase 2 — Knowledge Center
- MikroTik and OS command content is educational reference material.
- Search and category filtering run locally on the device.
- Commands are displayed as text and are never executed.

## Phase 3 — DNS & Protocols
- DNS lookup uses Cloudflare DNS-over-HTTPS for selected record types.
- Domain normalization, validation, response parsing and timeout are repository responsibilities.
- DNS results are transient UI state and are not persisted.
- Protocol/port reference data is bundled and works offline.

## Phase 4 — Repository Explorer
- Public GitHub repository search uses the unauthenticated REST API.
- Repository results are read-only and show metadata returned by GitHub.
- Queries are transmitted to GitHub and subject to its unauthenticated rate limits.
- No access token, private repository data or write operation is included.

## Security
- Never bundle Supabase secret/service-role keys, GitHub personal access tokens or provider secrets.
- Apply RLS to all exposed Supabase tables before any cloud sync is enabled.
- Host checks are limited to systems the user owns or is authorized to inspect.
- No port-range scanning, exploitation, credential collection or shell execution is provided.
