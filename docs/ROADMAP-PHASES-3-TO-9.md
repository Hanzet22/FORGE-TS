# FORGE-TS Roadmap — Phases 3–9

This roadmap is an implementation sequence, not a release announcement. Phase 0 foundation, Phase 1 Core Networking and Phase 2 Knowledge Center are existing milestones. Phase 3 is the current implementation milestone.

| Phase | Name | Scope | Completion boundary |
|---|---|---|---|
| 3 | DNS & Protocols | DNS-over-HTTPS record lookup, protocol/port reference, provider and privacy disclosure | Single-domain queries and offline reference; no enumeration or scanning |
| 4 | Repository Explorer | Public GitHub repository search/details, README and license metadata, paginated results and rate-limit-aware errors | Public read-only data; no token storage, issue creation, commits or repository mutation |
| 5 | Cloud & Offline Sync | Room entities/DAOs, local-first repositories, sync queue and WorkManager; Supabase adapter behind a repository interface | Only after a dedicated FORGE-TS Supabase project is identified and RLS schema is approved |
| 6 | Local Data & Settings | DataStore preferences, local history controls, cache lifecycle, export/import and sync preferences | User-controlled local data; no hidden telemetry |
| 7 | Security & Privacy Center | Permission inventory, provider disclosures, security guidance, privacy controls and dependency/license inventory | Informational and configuration checks; no offensive security features |
| 8 | Diagnostics & Monitoring | Connectivity state, bounded latency checks, single authorized endpoint history and opt-in scheduled refresh | User-selected targets only; background work is explicit and battery/network-aware |
| 9 | Quality & Release Preparation | Accessibility, localization, error handling, performance profiling, signing documentation, privacy/legal review and release checklist | Preparation only; no automatic publishing or store submission |

## Cross-phase engineering rules
- Preserve namespace and application ID `com.HG.Forge.Ts`.
- Keep Room as the local source of truth for data exposed to UI; remote data is reconciled into local storage.
- Keep network calls behind repositories and run them off the main thread with bounded timeouts.
- Never bundle Supabase secret/service-role keys, GitHub personal access tokens or provider secrets in the APK.
- Apply RLS to every exposed Supabase table and test policies with the intended roles before enabling sync.
- Respect public API rate limits, pagination, conditional requests and provider terms.
- No unrestricted shell, credential collection, bulk port scanning, exploitation or unauthorized target checks.
- No release/publishing action is implied by completion of these phases.

## Dependency gate
The currently connected Supabase account exposes a project named `furfinder`, not a FORGE-TS backend. It must not be repurposed. Phase 5 remains gated until a dedicated project is selected or created with explicit cost approval where applicable.
