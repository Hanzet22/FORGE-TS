# Phase 16 — Native Web-Check Expansion

## Goal
Expand Phase 15 into a native Kotlin/Jetpack Compose website-inspection workflow inspired by the upstream Web-Check project. This is a native reimplementation of selected capabilities, not a bundled copy of the upstream Astro/Express/Puppeteer application.

## Native checks implemented
The current native inspection collects:

- DNS: A, AAAA, NS, MX, TXT, CAA, SOA, CNAME, SRV, DS, DNSKEY, RRSIG, NSEC, TLSA, HTTPS and SVCB through Google Public DNS-over-HTTPS. Common DMARC and DKIM TXT names are queried as limited hints; selector guesses are not exhaustive.
- HTTP: response status, final URL, content type/length, bounded redirect chain and selected response headers.
- TLS: peer X.509 certificate subject, issuer, validity dates and SHA-256 fingerprint when a trusted HTTPS connection succeeds.
- Security headers: HSTS, CSP, X-Content-Type-Options, X-Frame-Options, Referrer-Policy, Permissions-Policy, COOP and CORP.
- Cookies: Set-Cookie names and Secure, HttpOnly and SameSite flag indicators; cookie values are not displayed or retained.
- SEO/site files: robots.txt, common sitemap.xml location and sitemap declarations in robots.txt, and security.txt at the RFC 9116 conventional locations.
- Page metadata: title, description, canonical URL, selected Open Graph and Twitter tags.
- Technology indicators: heuristic public HTML signatures for common frameworks and platforms.
- Server and edge hints: selected public Server/X-Powered-By/cache/CDN headers, without active firewall probing.
- Linked-page inventory: bounded same-host versus other-host link counts; linked pages are not crawled.
- Privacy hints: static signatures for selected analytics/tracker families and public social-profile links.
- Mixed-content hints: simple HTTP src/href references in a bounded HTML sample.

## Upstream API inventory and porting decisions
The inspected upstream tree contains 40 API check handlers and 44 check definitions (excluding shared helpers and the registry index). The native app does not assume that every server-side integration is suitable for an offline-capable Android client.

| Upstream check | Native disposition |
|---|---|
| archives | Deferred; requires an external archive service and provider policy review |
| block-lists | Deferred; external threat-list coverage and false-positive handling required |
| breaches | Excluded from this native passive inspection scope |
| caa-records | Covered by DNS CAA lookup |
| carbon | Deferred; third-party estimation service and methodology disclosure required |
| cookies | Implemented as response-cookie attribute inspection |
| dns-server | Partial; nameserver records are shown, resolver infrastructure profiling is not |
| domain-info | Deferred; registrar/RDAP enrichment needs provider discovery and privacy handling |
| hosts | Partial; A/AAAA and nameserver data are shown, no host-file or broad host enumeration |
| server-info | Partial; selected response headers only |
| dns | Implemented for selected common records |
| dnssec | Partial; resolver-reported AD metadata is shown, not an independent DNSSEC validator |
| firewall | Partial; selected public CDN/WAF response-header hints only, no evasion/probing |
| get-ip | Covered by public A/AAAA DNS results |
| headers | Implemented |
| hsts | Implemented in the security-header review |
| http-security | Implemented as a bounded header checklist |
| linked-pages | Partial; bounded link inventory only, not a crawler |
| location | Deferred; no geolocation inference from a target IP |
| mail-config | Partial; MX, SPF TXT, common DMARC and a few common DKIM selector TXT names are queried; this is not exhaustive validation |
| mixed-content | Implemented as a static HTML hint, not a browser execution test |
| ports | Excluded; active port scanning is not part of the native passive workflow |
| quality | Deferred; requires PageSpeed provider/API policy and credentials |
| rank | Deferred; external ranking provider |
| redirects | Implemented with a maximum of five manually validated redirects |
| robots-txt | Implemented |
| screenshot | Deferred; no embedded headless browser runtime |
| security-txt | Implemented |
| shodan | Deferred; third-party account/API integration and data-use review required |
| sitemap | Implemented for common path and robots.txt declarations |
| social-presence | Deferred; external provider/API lookups |
| social-tags | Implemented for selected Open Graph and Twitter metadata |
| ssl | Implemented for trusted HTTPS peer certificate metadata |
| status | Implemented as HTTP response status |
| subdomains | Deferred; certificate-transparency provider integration and scope controls required |
| tech-stack | Partial; heuristic HTML indicators only, not definitive fingerprinting |
| threats | Excluded from this native passive inspection scope |
| tls-connection | Partial; trusted HTTPS connection and peer certificate details |
| tls-client-compat | Deferred; no multi-client handshake simulation in the Android client |
| tls-security-audit | Partial; certificate metadata only, no cipher-suite enumeration or external lab scan |
| vulnerabilities | Excluded from this native passive inspection scope |
| tls-labs | Deferred; external assessment provider |
| trace-route | Excluded; active route probing is not part of this native passive workflow |
| trackers | Partial; static signatures for selected common tracker families, not a maintained exhaustive dataset |
| txt-records | Implemented through DNS TXT lookup |
| whois | Deferred; RDAP/WHOIS provider and registration-data privacy handling required |

The upstream repository also contains browser-automation checks and provider-backed checks that do not map one-to-one to a native Android implementation. Dispositions above are deliberate; a deferred or excluded feature must not be represented as completed.

## Network and privacy controls
- Only HTTP and HTTPS targets are accepted.
- Hostnames are resolved before requests; loopback, private/site-local, link-local, multicast and IPv6 unique-local destinations are rejected.
- Redirects are not followed automatically. The app validates each redirect destination, caps the chain at five hops and blocks HTTPS-to-HTTP downgrade redirects.
- Website response reads are capped at 128 KiB; robots/sitemap/security.txt reads are capped at 64 KiB.
- Requests have bounded connect/read timeouts and do not send cookies, credentials or authorization headers.
- The app does not run JavaScript, execute downloaded code, probe ports, perform traceroutes, exploit vulnerabilities or harvest credentials.
- Results are transient ViewModel state; no report history or cloud upload is introduced.
- DNS queries are sent to Google Public DNS. Website and well-known-file requests go directly to the target host. These providers may process request metadata under their own policies.
- DNS pre-resolution is a defense-in-depth check, not a complete network sandbox. Future hardening should pin validated destination addresses at the socket layer and keep redirect validation in place.

## Architecture
- UI: Jetpack Compose cards inside the existing Tools > OSINT Web destination.
- State: OsintWebViewModel.
- DNS: existing DnsLookupRepository and Google Public DNS.
- Website metadata: NativeWebCheckRepository using bounded platform HTTP/TLS APIs.
- No Node.js, Astro, Express, Puppeteer, Chromium or Web-Check server is embedded in the APK.

## Upstream reference and license
- Project: https://github.com/lissy93/web-check
- Copyright: Alicia Sykes.
- License: MIT; see `third_party/licenses/WEB-CHECK-MIT.txt`.
- FORGE-TS native code is an independent Kotlin implementation. Upstream project attribution is retained.

## Verification status
A GitHub Actions Android Debug APK build and CodeQL analysis must complete successfully before this phase is marked build-verified. Runtime behavior still requires device/network testing, especially redirect handling, certificate presentation and response limits.
