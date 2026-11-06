# FORGE-TS Architecture — Phases 0–8

## Operating model
Local-first, online and offline. Local utilities and reference content remain usable without connectivity. Network-backed features disclose their provider and use bounded requests.

## Layers
- Presentation: Jetpack Compose and Material 3.
- State: MVVM / ViewModels.
- Data: repositories coordinating local storage and Ktor.
- Preferences: DataStore for non-sensitive user preferences.
- Remote boundary: Supabase behind an interface; no backend secrets in the Android client.
- Background work: WorkManager for future opt-in synchronization.

## Phase 1 — Core Networking
CIDR calculation is local-only. Public IP lookup uses HTTPS and identifies its provider. Host Checker performs one DNS resolution and one user-selected TCP port check; optional TLS checks use platform certificate and hostname verification.

## Phase 2 — Knowledge Center
MikroTik and OS command content is educational reference material. Search and filtering run locally. Commands are never executed.

## Phase 3 — DNS & Protocols
DNS lookup uses Cloudflare DNS-over-HTTPS for selected record types. Results are transient UI state and are not persisted. Protocol/port reference data is bundled and works offline.

## Phase 4 — Repository Explorer
Public GitHub repository search uses the unauthenticated REST API. Results are read-only. Queries are transmitted to GitHub and subject to its unauthenticated rate limits.

## Phase 5 — Cloud & Offline Sync
Remote sync remains gated until a dedicated FORGE-TS Supabase project and approved RLS model are available. The connected FurFinder project is unrelated and must not be used.

## Phase 6 — Local Data & Settings
DataStore persists the preferred DNS record type. No sensitive diagnostic history or credentials are stored.

## Phase 7 — Security & Privacy
The in-app center documents provider disclosures, locally processed features and safe-use boundaries.

## Phase 8 — Diagnostics
Network diagnostics read Android's active network capabilities locally. Manual refresh only; no background polling or probing.

## Security
Never bundle Supabase secret/service-role keys, GitHub personal access tokens or provider secrets. Apply RLS to all exposed Supabase tables before enabling cloud sync. Host checks are limited to systems the user owns or is authorized to inspect. No port-range scanning, exploitation, credential collection or shell execution is provided.
