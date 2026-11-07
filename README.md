# FORGE-TS
**Network & Systems Toolkit** — a native Android app for network utilities, operating-system references and systems administration learning.

## Platform
- Native Android APK built with Kotlin, Jetpack Compose and Material 3.
- MVVM-oriented presentation and repository-based data boundaries.
- Light professional UI direction: white, grey, navy and blue.
- Selected namespace and application ID: `com.HG.Forge.Ts`.

## Implemented milestones
### Phase 0 — Foundation
Android project foundation and GitHub Actions debug APK workflow. Room, DataStore, WorkManager and Ktor dependencies are staged for incremental implementation.

### Phase 1 — Core Networking
IPv4/IPv6 CIDR calculator with subnet/broadcast/usable-host details and binary conversion; local VLSM and PAN/LAN/WLAN/VLAN IP allocation planners; public IP lookup and authorized single-host checks. See [Phase 1](docs/PHASE-1-CORE-NETWORKING.md).

### Phase 2 — Knowledge Center
Searchable offline MikroTik, OS command, addressing, network type, transport, VPN, tunneling and proxy protocol references. SSH host identity, key safety, TLS concepts and certificate verification are educational references only; the app does not execute commands, open SSH sessions or establish proxy/VPN tunnels. See [Phase 2](docs/PHASE-2-KNOWLEDGE-CENTER.md).

### Phase 3 — DNS & Protocols
Expanded live DNS-over-HTTPS lookup for A, AAAA, CNAME, MX, TXT, NS, SRV, CAA, SOA, PTR, HTTPS, SVCB, DS, DNSKEY, TLSA, SSHFP, RRSIG and NSEC records. Supports Cloudflare and Google Public DNS resolvers, TTL and DNS response sections/status, response time and resolver-reported DNSSEC AD metadata. Includes protocol detail cards for usage, pros, cons, risks and references. See [Phase 3](docs/PHASE-3-DNS-AND-PROTOCOLS.md) and [Phase 11](docs/PHASE-11-DNS-RECORD-EXPLORER-AND-PROTOCOL-GUIDES.md).

### Phase 4 — Repository Explorer
Read-only public GitHub repository search and basic repository metadata. See [Phase 4](docs/PHASE-4-REPOSITORY-EXPLORER.md).

### Phase 6 — Local Data & Settings
DataStore persistence for the preferred DNS record type. See [Phase 6](docs/PHASE-6-LOCAL-DATA-AND-SETTINGS.md).

### Phase 7 — Security & Privacy
Provider/data disclosures and safe-use boundaries. See [Phase 7](docs/PHASE-7-SECURITY-PRIVACY.md).

### Phase 8 — Diagnostics
Local Android network capability snapshot with manual refresh. See [Phase 8](docs/PHASE-8-DIAGNOSTICS.md).

## Gated and preparation phases
- **Phase 5 — Cloud & Offline Sync:** waiting for a dedicated FORGE-TS Supabase project and approved RLS schema. The connected FurFinder project will not be reused.
- **Phase 9 — Quality & Release Preparation:** checklist only. No release signing, publishing or store submission has been performed.

See [Phases 3–9 roadmap](docs/ROADMAP-PHASES-3-TO-9.md) and [Phase 10 — Addressing, Network Design & Protocol Knowledge](docs/PHASE-10-ADDRESSING-NETWORK-DESIGN-PROTOCOLS.md).

## Safety and privacy
The command area is a reference guide, not an unrestricted remote shell. Host checks are for systems the user owns or is authorized to inspect. DNS lookups disclose the requested domain and record type to Cloudflare. Repository search terms are sent to GitHub. No Supabase secret/service-role credentials or GitHub personal access tokens belong in the APK.

## Build
Run the Android Debug APK workflow from the GitHub Actions tab. The workflow uses JDK 17 and Gradle 8.13.
