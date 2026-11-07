# Phase 15 — OSINT Web

## Objective
Introduce a native Android OSINT workspace inspired by the public Web-Check project. FORGE-TS ports capabilities and workflows selectively rather than embedding the Astro/Express web application inside the Android APK.

## Initial delivery
- Adds an OSINT Web destination under Tools.
- Adds a domain inspection screen and ViewModel.
- Performs passive DNS-over-HTTPS lookups through the existing Google Public DNS repository.
- Collects A, AAAA, NS, MX, TXT and CAA records.
- Shows per-record status, answers, TTL, resolver timing and resolver-reported DNSSEC AD metadata.
- Keeps results in transient screen state; no report history or cloud upload is introduced.

## Planned modular expansion
1. Domain and registrar intelligence.
2. HTTP status, response headers and redirect metadata.
3. TLS certificate and connection metadata.
4. robots.txt, sitemap.xml, security.txt and page metadata.
5. Public technology and platform indicators.
6. Security-header and cookie configuration review.
7. Unified evidence-based report with source, retrieval time and limitations.

Each capability must identify its data source, disclose external providers, apply bounded timeouts and avoid collecting unnecessary personal data.

## Architecture
- Presentation: Jetpack Compose.
- State: ViewModel and coroutine scope.
- Data: existing DnsLookupRepository and Ktor client.
- No Node.js, Astro, Express, Puppeteer or browser runtime is embedded in the APK.
- Web-Check is a reference/upstream project, not a runtime dependency.

## Safety and privacy
- Passive public metadata is the default.
- The user should inspect domains they own or are authorized to assess.
- No port-range scanning, exploitation, credential harvesting, private-network probing or shell execution is part of this phase.
- Any future URL-fetching feature must validate targets, restrict redirects, defend against private/link-local destinations and DNS rebinding, enforce response-size/time limits, and clearly disclose requests.
- DNS queries are sent to Google Public DNS. The resolver may process or log queried names according to its own policies.
- Results are not persisted or uploaded by default.

## Upstream attribution
Web-Check: https://github.com/lissy93/web-check
Copyright (c) 2023 Alicia Sykes. Licensed under the MIT License. See third_party/licenses/WEB-CHECK-MIT.txt.
