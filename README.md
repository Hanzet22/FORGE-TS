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

### Phase 16 — Native Web-Check Expansion
Expands OSINT Web with native Kotlin/Compose DNSSEC/mail-record hints, HTTP status and redirects, TLS peer-certificate metadata, security headers, cookie attributes, robots.txt, sitemap and security.txt, page/social metadata, heuristic technology indicators, mixed-content hints, server/CDN signals, bounded link inventory and selected tracker signatures. Requests are bounded and public-host validated; private/local destinations and HTTPS downgrade redirects are blocked. This is a selective native port: active port scans, traceroute, vulnerability/breach checks and provider-key-dependent checks are deliberately excluded or deferred. See [Phase 16](docs/PHASE-16-NATIVE-WEB-CHECK.md).

### Phase 18 — DNS Security Intelligence
Adds transparent interpretation of Google Public DNS DNSSEC AD response signals and passive CAA record policy summaries. The app does not label an absent AD bit as DNSSEC failure or claim independent DS/DNSKEY/RRSIG cryptographic-chain validation. See [Phase 18](docs/PHASE-18-DNSSEC-AND-CAA-INTELLIGENCE.md).

### Phase 17 — Mail Security Intelligence
Adds passive DNS-based checks for MX, SPF, DMARC, common DKIM selector candidates, MTA-STS and SMTP TLS Reporting (TLS-RPT). Results are evidence indicators, not a complete mail-server audit: DKIM selector guesses are not exhaustive, and the MTA-STS policy file is not fetched or validated. Native website inspection now uses a 12-second connect/read timeout and explains common DNS, TCP-connect and TLS failure classes without treating a timeout as proof that a website is offline. See [Phase 17](docs/PHASE-17-MAIL-SECURITY-INTELLIGENCE.md).

### Phases 18–21 — Domain, DNSSEC & TLS Intelligence
Adds resolver-reported DNSSEC authentication evidence and passive CAA policy interpretation; public RDAP metadata without registrant contact data; TLS cipher-suite and certificate details; and HSTS directive analysis. DNSSEC is not locally chain-validated. Network diagnostics distinguish DNS, route, TCP refusal/timeout, TLS handshake and response-read failures while redacting local socket details. See [Phases 18–21](docs/PHASE-18-21-DOMAIN-TLS-INTELLIGENCE.md).
## Gated and preparation phases
- **Phase 5 — Cloud & Offline Sync:** waiting for a dedicated FORGE-TS Supabase project and approved RLS schema. The connected FurFinder project will not be reused.
- **Phase 9 — Quality & Release Preparation:** checklist only. No release signing, publishing or store submission has been performed.

See [Phases 3–9 roadmap](docs/ROADMAP-PHASES-3-TO-9.md) and [Phase 10 — Addressing, Network Design & Protocol Knowledge](docs/PHASE-10-ADDRESSING-NETWORK-DESIGN-PROTOCOLS.md).

### Phase 29 — Central Authentication Gate
Protects every executable feature in the Simulate and Tools tabs behind one Supabase Auth session. The tabs remain visible, but unauthenticated users see the shared sign-in/sign-up gate instead of feature screens. Email-verification and password-recovery deep links are routed back into the app. The account screen provides Create Account, Sign-in Account and Reset Password flows using Supabase Auth. Password reset requests send the recovery email; the verified recovery callback opens the new-password form. The configured password policy requires at least 20 characters with uppercase, lowercase, a number and a symbol. The app shell owns session restoration and refresh; session tokens are encrypted with Android Keystore-backed storage and restored when the app reopens. Sign-out revokes the server session when possible and clears local encrypted session data. The former Remote Access panel has been removed; Simulation Center now presents local labs only. Home, Learn, Network and Settings remain available without login. The existing RLS-protected lab table and JWT-verified Redis gateway remain backend authorization boundaries; the gate is not a substitute for backend authorization. See [Phase 29 technical scope](docs/PHASE-29-AUTHENTICATION-GATE.md).

## Safety and privacy
The command area is a reference guide, not an unrestricted remote shell. Host checks are for systems the user owns or is authorized to inspect. DNS lookups disclose the requested domain and record type to Google Public DNS. Repository search terms are sent to GitHub. OSINT Web currently collects passive public DNS metadata only; future URL-fetching modules must apply strict target validation and SSRF protections. No Supabase secret/service-role credentials or GitHub personal access tokens belong in the APK.

## Build
Run the Android Debug APK workflow from the GitHub Actions tab. The workflow uses JDK 17 and Gradle 8.13.


## Phase 23 — IP & Network Registry Intelligence

- Resolves the first available public A/AAAA answer and performs at most one read-only IP RDAP lookup.
- Displays registry network name, handle, address range, IP version and country code when supplied.
- Blocks private/reserved destinations, requires HTTPS, bounds redirects/timeouts and caps response size.
- Does not collect registrant contact data or claim physical hosting location.
- Adds best-effort ASN, holder and announced-prefix enrichment through RIPEstat; failure is non-fatal and the provider disclosure is documented.
- See [Phase 23 technical scope](docs/PHASE-23-IP-NETWORK-RDAP-INTELLIGENCE.md).

### Phase 24 — Passive WAF Fingerprint Intelligence
Expands the existing edge hints into evidence-based passive WAF fingerprinting using the ordinary HTTP response headers and cookie names already collected. Includes initial signatures for Cloudflare, Imperva/Incapsula, Akamai, F5 BIG-IP, Sucuri, Barracuda and explicitly identified ModSecurity. No additional requests, attack payloads, WAF bypass attempts or cookie-value retention are used. A fingerprint is heuristic evidence, not proof of enforcement or absence. See [Phase 24 technical scope](docs/PHASE-24-PASSIVE-WAF-FINGERPRINT-INTELLIGENCE.md).

### Phase 25 — Learn & Lab
Upgrades the existing Learn tab without replacing its catalog or navigation. Expanded lessons now provide a definition, explanation, function, advantages, limitations, suitable environments, step-by-step guidance, troubleshooting and standards references. New DNS fundamentals, HTTP request/response and network security/privacy lessons complement the existing catalog. Local deterministic learning panels cover DNS resolution, IPv4 prefix sizing, DHCPv4 DORA, TCP/UDP behavior, HTTP request/response and longest-prefix routing. Each lab includes resettable interactions and a local comprehension check with immediate feedback. Every lab is visibly marked simulated; it sends no packets, inspects no network traffic and executes no commands. No new dependency or remote backend is required. See [Phase 25 technical scope](docs/PHASE-25-LEARN-AND-LAB.md).



### Phase 26 — Hybrid Simulation Center
Adds a dedicated Simulate tab with automatic validated-network awareness, local Redis-style key/value and TTL operations, a local Supabase-style sample table, and a real HTTP health check against a temporary server bound only to `127.0.0.1`. A deterministic loopback simulator is also available and opens no socket. Local data is temporary and is not uploaded. Cloud Redis and Supabase integrations remain gated: the existing HyperAI Redis database and FurFinder Supabase project are not reused; a dedicated Upstash database and dedicated Supabase project with RLS are required before online connectors can be activated. See [Phase 26 technical scope](docs/PHASE-26-HYBRID-SIMULATION-CENTER.md).
