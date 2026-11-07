# FORGE-TS Roadmap — Phases 3–9

This roadmap is an implementation sequence, not a release announcement. Phase 0 foundation, Phase 1 Core Networking and Phase 2 Knowledge Center are existing milestones. Phase 2 includes MikroTik, OS command, SSH and TLS learning references. Phases 3, 4, 6, 7 and 8 have incremental implementations. Phase 5 cloud sync is gated by backend selection; Phase 9 is a preparation checklist only.

| Phase | Name | Scope | Current status and boundary |
|---|---|---|---|
| 3 | DNS & Protocols | DNS-over-HTTPS record lookup, protocol/port reference, provider and privacy disclosure | Implemented: single-domain lookups and offline reference; no enumeration or scanning |
| 4 | Repository Explorer | Public GitHub repository search/details and metadata | Implemented: read-only public search and basic metadata; no token storage or repository mutation |
| 5 | Cloud & Offline Sync | Room entities/DAOs, local-first repositories, sync queue and WorkManager; Supabase adapter | Gated: no dedicated FORGE-TS Supabase project is connected; do not repurpose the existing FurFinder project |
| 6 | Local Data & Settings | DataStore preferences, local history controls, cache lifecycle, export/import and sync preferences | Incremental: preferred DNS record type is persisted locally; no sensitive history or cloud backup |
| 7 | Security & Privacy Center | Permission inventory, provider disclosures, security guidance, privacy controls and dependency/license inventory | Incremental: provider/data disclosures and tool safety boundaries |
| 8 | Diagnostics & Monitoring | Connectivity state, bounded latency checks, single authorized endpoint history and opt-in scheduled refresh | Incremental: local Android network capability snapshot and manual refresh only |
| 9 | Quality & Release Preparation | Accessibility, localization, error handling, performance profiling, signing documentation, privacy/legal review and release checklist | Preparation checklist only; no signing, publishing or store submission |

## Cross-phase engineering rules
- Preserve namespace and application ID `com.HG.Forge.Ts`.
- Keep Room as the local source of truth for data exposed to UI; remote data is reconciled into local storage.
- Keep network calls behind repositories and run them off the main thread with bounded timeouts.
- Never bundle Supabase secret/service-role keys, GitHub personal access tokens or provider secrets in the APK.
- Apply RLS to every exposed Supabase table and test policies with the intended roles before enabling cloud sync.
- Respect public API rate limits, pagination, conditional requests and provider terms.
- No unrestricted shell, credential collection, bulk port scanning, exploitation or unauthorized target checks.
- No release/publishing action is implied by completion of these phases.

## Dependency gate
The connected Supabase account currently exposes a project named `furfinder`, not a FORGE-TS backend. It must not be repurposed. Phase 5 remains gated until a dedicated project is selected or created with explicit cost approval where applicable.
