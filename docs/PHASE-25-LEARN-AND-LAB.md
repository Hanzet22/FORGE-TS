# Phase 25 — Learn & Lab

Status: implementation started on `main`  
Primary screen: `app/src/main/java/com/HG/Forge/Ts/ui/screens/KnowledgeCenterScreen.kt`

## Goal

Upgrade the existing offline Knowledge Center into a structured learning experience while preserving the existing lesson catalog, search, category filters, and expandable-card navigation.

Each expanded lesson now presents:
1. What it is
2. Explanation
3. Function
4. Advantages
5. Limitations and trade-offs
6. Suitable environments
7. Step-by-step tutorial
8. A local simulation where a matching exercise exists
9. Bugs and troubleshooting
10. Standards and further reading

## Existing app constraints verified

- Native Kotlin + Jetpack Compose + Material 3.
- `minSdk 29`, `compileSdk 36`, `targetSdk 36`.
- Existing Compose BOM and Material 3 are already present.
- The Learn tab is already routed to `KnowledgeCenterScreen`; navigation does not need to be rewritten.
- Current catalog is a static in-app list and the lesson examples are reference-only.
- No new dependency, remote content service, Room table, WorkManager job, or Supabase backend is required for this feature.

## Implemented in this change

- Added a structured lesson detail presentation while retaining existing lesson IDs and source entries.
- Added context-aware tutorial and troubleshooting guidance for key lessons.
- Added standards/further-reading links for core protocols.
- Added new catalog entries for DNS fundamentals, HTTP request/response, and network security/privacy.
- Added deterministic local learning panels:
  - DNS resolution stages using `portal.example.com` and `192.0.2.25`.
  - IPv4 prefix calculator for documentation-only `192.0.2.0/24` through `/30`.
  - DHCPv4 DORA walkthrough using a synthetic lease.
  - TCP vs UDP conceptual timeline.
  - Longest-prefix route selection using documentation-only address ranges.
  - HTTP request/response stages using a synthetic GET and 200 OK fixture.
- Every simulation carries a visible notice: **SIMULATED — No packets sent — No network inspected**.
- Simulations use local state only. They do not create sockets, inspect device traffic, send DNS queries, scan hosts, or accept credentials.
- Existing read-only command examples remain read-only and are not executed.

## Research synthesis

### Standards facts versus product recommendations

**Standards facts** should be attributed to RFCs or relevant standards. **FORGE-TS recommendations** describe learning design and implementation choices; they are not protocol requirements.

Important content checks:
- DNS concepts should follow RFC 1034 and RFC 1035.
- IPv4 and private addressing should follow RFC 791 and RFC 1918.
- IPv6 should follow RFC 8200 and RFC 4291.
- CIDR should follow RFC 4632.
- DHCPv4 should follow RFC 2131; DHCPv6 should be taught separately using RFC 8415.
- TCP should follow RFC 9293; UDP should follow RFC 768.
- HTTP semantics should follow RFC 9110 and the relevant HTTP version RFCs.
- SSH architecture should follow RFC 4251 and related SSH RFCs.
- QUIC should follow RFC 9000 and RFC 9001.
- TLS 1.3 material must follow RFC 9846, which the RFC Editor record identifies as obsoleting RFC 8446; do not copy old references blindly.
- HTTP semantics should follow RFC 9110; HTTP/1.1 and HTTP/3 wire mappings are specified separately (RFC 9112 and RFC 9114).

### Scholarly evidence

The Undermind literature review completed on 2026-11-08. Its summary is based on available abstracts and metadata, not full-text review. The strongest applicable findings are:

- A meta-analysis of 145 empirical studies reported a large overall positive effect for simulation-based learning of complex skills (g = 0.85). It also found that learners with lower prior knowledge benefit from examples, while learners with higher prior knowledge benefit more from reflection phases. This supports a worked-example-first path and optional challenge/reflection prompts, but the meta-analysis spans higher education domains beyond networking. [Chernikova et al., 2020](https://doi.org/10.3102/0034654320933544)
- ProtoSim, a networking learning environment evaluated with 211 high-school students, visualized information flow across TCP/IP layers and combined the simulator with short-answer missions. The study reports improved pre/post understanding of protocol-layer mechanisms. [Kitamura et al., 2026](https://doi.org/10.1109/TLT.2026.3699503)
- ProtoSim 2 was evaluated for self-learning with 18 non-CS undergraduate and graduate participants; all completed the materials within one hour without teacher support, with significant pre/post gains across four TCP/IP layers. The small sample and non-CS participant group limit generalization. [Kitamura et al., 2025](https://doi.org/10.1109/TALE66047.2025.11346737)
- A mobile network-animation study evaluated accessibility/usability with disabled and non-disabled participants and found improvements while also identifying remaining screen-reader and interaction gaps. This reinforces the need for both semantic accessibility tests and manual assistive-technology review. [Svaigen & Martimiano, 2018](https://doi.org/10.1109/TLA.2018.8291484)
- A systematic literature review and bibliometric analysis specifically examines simulation-based learning for computer/networking education. [Asadi et al., 2024](https://doi.org/10.1007/s10639-024-12476-7)

These studies support a short guided simulator plus question/feedback cycle. They do not prove that any single UI design will work equally well for all learners or devices; the app still needs its own usability and learning-outcome evaluation.

### Learning design

The lesson format should use progressive disclosure: start with a concise definition, then explain the model, then let learners run a small guided simulation, and finally diagnose a deliberately seeded issue. Provide an example before asking a beginner to solve a task. Give immediate, specific feedback and explain why the answer is correct. A simulation must teach a concept, not masquerade as a live network diagnostic.

### Architecture recommendation

Keep the content bundled and available immediately. A small static catalog does not justify adding a database or synchronization stack. If the catalog grows substantially, introduce a repository and versioned content schema; use Room only when relational queries or persistent content updates justify it. Keep simulation logic pure and deterministic so it can be unit-tested without Android networking APIs.

### Accessibility recommendation

Use meaningful section headings, explicit button labels, accessible simulator status, predictable focus order, scalable text, and text equivalents for diagrams. Test semantics and state restoration, then perform manual TalkBack and keyboard/D-pad checks. Color must not be the only signal for success, failure, or packet direction.

## Research sources

### Standards and protocol references
- RFC Editor index: https://www.rfc-editor.org/
- DNS RFC 1034: https://www.rfc-editor.org/rfc/rfc1034
- DNS RFC 1035: https://www.rfc-editor.org/rfc/rfc1035
- IPv4 RFC 791: https://www.rfc-editor.org/rfc/rfc791
- Private IPv4 RFC 1918: https://www.rfc-editor.org/rfc/rfc1918
- IPv6 RFC 8200: https://www.rfc-editor.org/rfc/rfc8200
- IPv6 addressing architecture RFC 4291: https://www.rfc-editor.org/rfc/rfc4291
- CIDR RFC 4632: https://www.rfc-editor.org/rfc/rfc4632
- DHCPv4 RFC 2131: https://www.rfc-editor.org/rfc/rfc2131
- DHCPv6 RFC 8415: https://www.rfc-editor.org/rfc/rfc8415
- UDP RFC 768: https://www.rfc-editor.org/rfc/rfc768
- TCP RFC 9293: https://www.rfc-editor.org/rfc/rfc9293
- HTTP semantics RFC 9110: https://www.rfc-editor.org/rfc/rfc9110
- TLS 1.3 RFC Editor record: https://www.rfc-editor.org/info/rfc9846/
- SSH architecture RFC 4251: https://www.rfc-editor.org/rfc/rfc4251
- QUIC RFC 9000: https://www.rfc-editor.org/rfc/rfc9000
- QUIC TLS RFC 9001: https://www.rfc-editor.org/rfc/rfc9001
- ITU-T OSI reference model X.200: https://www.itu.int/rec/T-REC-X.200

### Android and implementation
- Android offline-first architecture: https://developer.android.com/topic/architecture/data-layer/offline-first
- Android UI layer and unidirectional data flow: https://developer.android.com/topic/architecture/ui-layer
- Lifecycle-aware coroutines: https://developer.android.com/topic/libraries/architecture/coroutines
- Compose accessibility: https://developer.android.com/develop/ui/compose/accessibility
- Compose accessibility testing: https://developer.android.com/develop/ui/compose/accessibility/testing
- Compose testing patterns: https://developer.android.com/develop/ui/compose/testing/common-patterns
- Adaptive Compose layouts: https://developer.android.com/develop/ui/compose/layouts/adaptive/get-started-with-adaptive-apps
- Room testing and migration guidance (only relevant if the catalog later requires a database): https://developer.android.com/training/data-storage/room/testing-db

### Learning and practical references
- Cloudflare Learning Center: https://www.cloudflare.com/learning/
- Cloudflare University: https://learn.cloudflare.com/
- Cisco Networking Academy: https://www.cisco.com/site/us/en/learn/training-certifications/training/netacad/index.html
- Cisco subnetting guide: https://www.cisco.com/c/en/us/support/docs/ip/routing-information-protocol-rip/13788-3.html
- Red Hat networking documentation: https://docs.redhat.com/en/documentation/red_hat_enterprise_linux/8/html/configuring_and_managing_networking/
- MDN HTTP documentation: https://developer.mozilla.org/en-US/docs/Web/HTTP
- Microsoft DNS troubleshooting: https://learn.microsoft.com/en-us/troubleshoot/windows-server/networking/troubleshoot-dns-guidance

## Research process and caveats

Research used Exa, IOConnex Web Search, Firecrawl, Geekflare, Tavily Research, Parallel Research, and Undermind literature search. Search excerpts were treated as discovery signals, not automatically as authoritative facts. Standards claims are anchored to the RFC Editor and standards bodies wherever possible. Undermind's academic literature review is asynchronous and remains a separate follow-up input; this implementation does not claim that its paper review is complete.

Supabase was not modified or connected to this feature: the current Learn experience is deliberately local/offline and does not need a remote database. No external content was downloaded into the app, so this change does not embed third-party copyrighted lesson text.

## Validation still required

- Run Android Debug build and CodeQL after the final Phase 25 commit.
- Review all generated lesson guidance for protocol-specific accuracy.
- Add automated unit tests for CIDR arithmetic and route selection if the repository's current test setup supports them.
- Run an offline/manual device pass, including small-screen scrolling and accessibility.
- Verify that the old catalog count and stable IDs remain unchanged.
