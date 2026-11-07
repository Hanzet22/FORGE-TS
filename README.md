# FORGE-TS
**Network & Systems Toolkit** — a native Android app for network utilities, operating-system references and systems administration learning.

## Platform
- Native Android APK built with Kotlin, Jetpack Compose and Material 3.
- **Minimum supported OS: Android 10 (API 29).**
- **Android 16 (API 36) and later:** built against API 36 and configured with target SDK 36; newer Android releases remain subject to compatibility verification.
- Android 10 through Android 16+ is the intended supported device range; devices below Android 10 are not supported.
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
Expanded live DNS-over-HTTPS lookup for A, AAAA, CNAME, MX, TXT, NS, SRV, CAA, SOA, PTR, HTTPS, SVCB, DS, DNSKEY, TLSA, SSHFP, RRSIG and NSEC records. Uses Google Public DNS over HTTPS, TTL and DNS response sections/status, response time and resolver-reported DNSSEC AD metadata. Includes a legacy Protocol Library plus the dedicated searchable Protocol Center with 80+ structured protocol profiles, category filters, technical details, use cases, functions, pros/cons, deployment hurdles, standards/specification references, suitability and environment guidance. See [Phase 3](docs/PHASE-3-DNS-AND-PROTOCOLS.md) and [Phase 11](docs/PHASE-11-DNS-RECORD-EXPLORER-AND-PROTOCOL-GUIDES.md).

### Phase 4 — Repository Explorer
Read-only public GitHub repository search and basic repository metadata. See [Phase 4](docs/PHASE-4-REPOSITORY-EXPLORER.md).

### Phase 6 — Local Data & Settings
DataStore persistence for the preferred DNS record type. See [Phase 6](docs/PHASE-6-LOCAL-DATA-AND-SETTINGS.md).

### Phase 7 — Security & Privacy
Provider/data disclosures and safe-use boundaries. See [Phase 7](docs/PHASE-7-SECURITY-PRIVACY.md).

### Phase 8 — Diagnostics
Local Android network capability snapshot with manual refresh. See [Phase 8](docs/PHASE-8-DIAGNOSTICS.md).

### Phase 12 — Dedicated Protocol Center
A standalone tab expands the catalog across addressing, transport, routing, switching, multicast, VPN/security, overlays, application services, identity, management, IoT and real-time communication. See [Phase 12](docs/PHASE-12-DEDICATED-PROTOCOL-CENTER.md).

### Phase 13 — Full Technical Protocol Documentation
All 83 protocol and protocol-family entries now include dedicated technical chapters covering architecture, packet/frame/message models, operation lifecycle, security boundaries, performance, deployment/interoperability, troubleshooting and primary references. The detail view also explains how to verify RFC status, updates, obsoletes, errata and non-IETF standards. See [Phase 13](docs/PHASE-13-FULL-TECHNICAL-PROTOCOL-DOCUMENTATION.md).

### Phase 14 — Application Protocol Center
A fourth dedicated section adds a searchable application-protocol catalog covering HTTP/1.1, HTTP/2, HTTP/3, HTTPS, TLS 1.3, WebSocket, Server-Sent Events, WebDAV, REST, GraphQL, gRPC, SOAP 1.2, JSON-RPC 2.0 and Protocol Buffers. Each entry includes a technical detail view for architecture, message model, lifecycle, security, performance, deployment, troubleshooting, advantages, limitations and primary references. The documentation distinguishes wire protocols from architectural styles, query specifications and serialization formats. See [Phase 14](docs/PHASE-14-APPLICATION-PROTOCOL-CENTER.md).

### Phase 15 — OSINT Web
Introduces a native OSINT workspace under Tools, inspired by the public Web-Check project. The initial implementation performs passive Google Public DNS-over-HTTPS collection for A, AAAA, NS, MX, TXT and CAA records, with per-check status, answers, TTL, timing and resolver-reported DNSSEC metadata. Results remain transient and are not uploaded or persisted. This is a selective native port, not an embedded copy of the Astro/Express application. See [Phase 15](docs/PHASE-15-OSINT-WEB.md).

## Gated and preparation phases
- **Phase 5 — Cloud & Offline Sync:** waiting for a dedicated FORGE-TS Supabase project and approved RLS schema. The connected FurFinder project will not be reused.
- **Phase 9 — Quality & Release Preparation:** checklist only. No release signing, publishing or store submission has been performed.

See [Phases 3–9 roadmap](docs/ROADMAP-PHASES-3-TO-9.md) and [Phase 10 — Addressing, Network Design & Protocol Knowledge](docs/PHASE-10-ADDRESSING-NETWORK-DESIGN-PROTOCOLS.md).

## Safety and privacy
The command area is a reference guide, not an unrestricted remote shell. Host checks are for systems the user owns or is authorized to inspect. DNS lookups disclose the requested domain and record type to Google Public DNS. Repository search terms are sent to GitHub. OSINT Web currently collects passive public DNS metadata only; future URL-fetching modules must apply strict target validation and SSRF protections. No Supabase secret/service-role credentials or GitHub personal access tokens belong in the APK.

## Build
Run the Android Debug APK workflow from the GitHub Actions tab. The workflow uses JDK 17 and Gradle 8.13.
