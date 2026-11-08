# Phase 24 — Passive WAF Fingerprint Intelligence

## Goal
Expand the existing edge/firewall hint into a dedicated, evidence-based WAF fingerprint finding using only the ordinary HTTP response already collected by NativeWebCheckRepository.

This is a selective native Kotlin implementation inspired by the upstream Web-Check firewall check. It is not a port of the upstream server-side API handler and does not claim complete vendor coverage.

## Detection inputs
The classifier inspects:
- Selected response headers, including CF-RAY, X-Iinfo, X-Akamai-Request-BC, X-Akamai-Transformed, X-Akamai-Request-ID and X-Sucuri-ID.
- The Server response header for explicit vendor signatures.
- Cookie names from Set-Cookie headers. Cookie values are not displayed or retained.

Initial vendor signatures:
- Cloudflare
- Imperva / Incapsula
- Akamai
- F5 BIG-IP
- Sucuri
- Barracuda
- ModSecurity (only when explicitly identified by the Server header)

## Confidence and interpretation
- A single matching indicator is reported as a possible fingerprint.
- Two or more independent indicators for the same vendor are reported as a likely fingerprint.
- Evidence is listed so users can inspect why a vendor was suggested.
- Multiple vendors may appear because CDN, bot management and WAF services can coexist.
- The result is an indicator, not proof of active enforcement, product ownership, configuration quality or origin infrastructure.
- No match is not proof that a WAF is absent. Transparent, masked, customized and unrecognized deployments may expose no supported signature.

## Safety and privacy boundaries
- Uses only the ordinary page response already fetched by the existing inspection flow.
- Sends no additional requests for WAF detection.
- Does not send malformed paths, attack strings, exploit payloads or vendor-specific challenge requests.
- Does not attempt WAF bypass, evasion, rule discovery, rate-limit testing or vulnerability testing.
- Does not retain cookie values; only cookie names are matched.
- Existing public-host validation, redirect validation, response limits and timeout controls remain unchanged.

## Architecture
- Repository: NativeWebCheckRepository.
- UI: existing generic NativeWebFinding card renderer; no separate screen or external runtime.
- No Playwright, Puppeteer, Chromium, Node.js service or provider API key is introduced.

## References
- Web-Check: https://github.com/lissy93/web-check
- Nmap HTTP WAF fingerprint reference (used only as public background for passive header/cookie indicators; its active probing behavior is not ported): https://nmap.org/nsedoc/scripts/http-waf-fingerprint.html
- OWASP Web Security Testing Guide, information gathering and server fingerprinting: https://owasp.org/www-project-web-security-testing-guide/latest/4-Web_Application_Security_Testing/01-Information_Gathering/02-Fingerprint_Web_Server

## Verification
GitHub Actions Android Debug APK and CodeQL workflows must pass before this phase is marked build-verified. Device-level testing is still required to assess false positives and real-world header variations.
