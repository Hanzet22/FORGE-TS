# Phase 18 — DNS Security Intelligence

## Scope
Adds passive interpretation of Google Public DNS DNSSEC AD signals for sampled DS, DNSKEY and RRSIG lookups, plus a summary of CAA answers already retrieved by the DNS explorer.

## DNSSEC interpretation
- AD=true is reported only as the recursive resolver's authenticated-data signal.
- An absent AD flag is reported as not observed, not proof that a zone is unsigned or bogus.
- The app does not independently construct or verify the DS → DNSKEY → RRSIG chain. A full local validator requires wire-format canonicalization, trust-anchor management, algorithm support and rigorous test vectors.

## CAA interpretation
- Displays returned CAA records and labels common issue, issuewild and iodef properties.
- No CAA answer means not detected; it is not a certificate-issuance prohibition.
- This is a summary, not a complete CA issuance simulation.

## Existing coverage
- HTTP security headers already include HSTS, CSP, Referrer-Policy, Permissions-Policy and selected cross-origin headers.
- TLS inspection reports the platform-negotiated cipher suite and peer certificate metadata when HTTPS succeeds.
- Existing mail-security checks remain passive DNS observations.

## Timeout diagnosis
The reported furmony.com failure occurs before an HTTPS connection is established: TCP connect to 203.107.45.167:443 did not complete within 12 seconds. No client-side change can guarantee that a remote server, CDN edge, firewall or route accepts a connection. The app must not bypass TLS validation, downgrade to HTTP or use an untrusted proxy. Re-test from another trusted network and compare with another known-reachable domain; if only this host fails across networks, the site operator or hosting provider needs to inspect its edge/service.

## Deferred and excluded
- Independent DNSSEC cryptographic validation awaits a maintained, tested validation library and trust-anchor strategy.
- RDAP requires a dedicated bounded client and response schema; it is not complete here.
- Carbon estimates await a defensible methodology and uncertainty display.
- Active port scanning, traceroute, subdomain enumeration, breach-data aggregation and bypass-oriented checks are excluded from this passive Android port.

## Privacy and verification
DNS queries are sent to Google Public DNS. Inspect only domains you own or are authorized to assess. Verify against signed and unsigned domains, plus domains with and without CAA records. Android Debug APK and CodeQL must pass before build verification.