# Phases 18–22 — Domain, DNSSEC, TLS and Carbon Intelligence

## Phase 18 — DNSSEC and CAA evidence
- Uses the existing Google Public DNS record explorer for DS, DNSKEY, RRSIG and NSEC records.
- Presents the resolver's authenticated-data (AD) flag as resolver-provided validation evidence.
- Explicitly does not claim local cryptographic verification of the DS → DNSKEY → RRSIG chain.
- A missing AD flag is not automatically a DNSSEC-bogus result; SERVFAIL is not uniquely attributable to DNSSEC.
- Interprets returned CAA issue, issuewild and iodef directives as a concise policy summary. It does not simulate every CA issuance decision.
- Nameserver/DNS server information remains in the existing NS/A/AAAA explorer rather than duplicating another DNS module.

## Phase 19 — RDAP Domain Intelligence
- Uses the HTTPS RDAP bootstrap service at rdap.org to retrieve public domain registration metadata.
- Displays domain handle, registrar name when exposed, statuses, registration/expiry/change events and nameservers.
- Does not collect registrant contact details, vCards, email addresses or postal addresses.
- Enforces HTTPS-only redirects, public-address validation, bounded redirects, connect/read timeouts and a 256 KiB response cap.
- RDAP availability and data fields vary by registry; a missing field is displayed as unavailable rather than inferred.

## Phase 20 — TLS Connection Intelligence
- Displays the TLS cipher suite and peer certificate metadata available through Android's standard HTTPS connection API. The negotiated protocol version is not exposed by this implementation's current API path and is not guessed.
- Certificate subject, issuer, validity dates and SHA-256 fingerprint remain informational.
- The Android TLS stack performs certificate-chain and hostname verification; FORGE-TS does not disable trust checks.
- A TCP connection timeout is not classified as a TLS/certificate failure because the handshake may never have started.

## Phase 21 — HSTS and HTTP Security Analysis
- Adds HSTS interpretation for max-age, includeSubDomains and preload directives.
- A six-month max-age is shown as a baseline indicator, not a universal compliance verdict.
- Does not claim browser preload-list inclusion or test subdomains.
- Existing CSP, Referrer-Policy, Permissions-Policy, frame and content-type header presence checks remain passive response analysis.

## Phase 22 — Website Carbon Intelligence
- Adds an optional, off-by-default Website Carbon API lookup for an estimated page-load footprint.
- The user must explicitly enable it before the page URL is sent to the third-party service.
- Displays provider-reported grid/renewable CO2 estimates, transfer bytes, rating, cleaner-than percentile and green-hosting signal when available.
- Estimates are model outputs, not direct energy or emissions measurements; they depend on the provider's assumptions and current methodology.
- Carbon API failures do not fail the core DNS/HTTP inspection. No carbon result is fabricated when the provider does not return a field.

## Network error diagnostics
- Separates DNS resolution, no-route, TCP refusal, TCP connection timeout, TLS handshake and response-read timeout where the platform exposes enough evidence.
- Redacts the device's local socket endpoint from displayed low-level exception details.
- Increasing a timeout cannot make a remote endpoint accept connections. The app does not bypass CDN/WAF controls, retry aggressively, or probe alternate ports.

## Deliberately excluded or deferred upstream checks
- Active port scans, traceroute, broad subdomain enumeration, vulnerability exploitation, breach lookups and provider-key-dependent threat intelligence are not included.
- Archive metadata and external reputation/blocklist feeds remain deferred until data sources, privacy impact, licensing, false-positive handling and safe query limits are reviewed.
- The project remains a selective native Kotlin/Compose adaptation of Web-Check, not a full feature-for-feature copy.

## Verification
Android Debug APK and CodeQL workflows must pass before these phases are marked build-verified. Runtime testing should use domains the operator owns or is authorized to inspect. Third-party RDAP and carbon providers may rate-limit, change schemas or be unavailable.