# FORGE-TS
**Network & Systems Toolkit** — a native Android app for network utilities, operating-system references and systems administration learning.

## Platform
- Native Android APK built with Kotlin, Jetpack Compose and Material 3.
- MVVM-oriented presentation and repository-based data boundaries.
- Light professional UI direction: white, grey, navy and blue.
- Selected namespace and application ID: `com.HG.Forge.Ts` (keep aligned unless a deliberate migration is approved).

## Implemented milestones
### Phase 0 — Foundation
- Android project foundation and GitHub Actions debug APK workflow.
- Room, DataStore, WorkManager and Ktor dependencies included for staged implementation.
- Supabase integration boundary; no privileged credentials in the APK.

### Phase 1 — Core Networking
- IPv4 and IPv6 CIDR calculator.
- Public IP lookup with provider disclosure.
- Authorized single-host DNS, TCP and optional TLS checks.
- See [Phase 1 documentation](docs/PHASE-1-CORE-NETWORKING.md).

### Phase 2 — Knowledge Center
- Searchable, filterable MikroTik and OS command references.
- Expandable explanations and read-only examples; commands are never executed.
- See [Phase 2 documentation](docs/PHASE-2-KNOWLEDGE-CENTER.md).

### Phase 3 — DNS & Protocols
- DNS-over-HTTPS lookup for A, AAAA, CNAME, MX, TXT, NS, SRV and CAA records.
- Offline reference for common network protocols and ports.
- Cloudflare provider and query disclosure.
- See [Phase 3 documentation](docs/PHASE-3-DNS-AND-PROTOCOLS.md).

## Roadmap
Phases 4–9 cover the read-only GitHub Repository Explorer, cloud/offline sync, local data and settings, Security & Privacy Center, bounded diagnostics and monitoring, and quality/release preparation.

See [Phases 3–9 roadmap](docs/ROADMAP-PHASES-3-TO-9.md).

## Safety and privacy
The command area is a reference guide, not an unrestricted remote shell. Host checks are for systems the user owns or is authorized to inspect. DNS lookups disclose the requested domain and record type to Cloudflare. FORGE-TS does not store DNS query history. No Supabase secret/service-role credentials or GitHub personal access tokens belong in the APK.

## Build
Run the Android Debug APK workflow from the GitHub Actions tab. The workflow uses JDK 17 and Gradle 8.13.
