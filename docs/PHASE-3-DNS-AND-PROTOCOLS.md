# Phase 3 — DNS & Protocols

## Delivered scope
- DNS-over-HTTPS lookup through Cloudflare's public DNS JSON endpoint.
- Supported query types: A, AAAA, CNAME, MX, TXT, NS, SRV and CAA.
- Input normalization and validation, bounded request timeout, DNS status and answer display.
- Provider disclosure and explicit notice that the queried domain/type are sent to Cloudflare.
- Offline protocol reference for common infrastructure services and ports.
- Native Compose screen integrated into bottom navigation.

## Boundaries
- No DNS zone transfers, brute force, subdomain enumeration or bulk queries.
- No port scanning or automatic service discovery.
- Protocol port values are educational defaults, not claims that a service is running.
- DNS results are live network responses and are not cached or uploaded to FORGE-TS.

## Implementation
- `DnsLookupRepository` owns normalization, the HTTPS request and response parsing.
- `DnsProtocolsViewModel` owns screen state and coroutine lifecycle.
- `DnsProtocolsScreen` contains the DNS lookup and static protocol reference.
- Cloudflare DNS-over-HTTPS is the disclosed provider; the app does not claim to be a recursive resolver.

## Privacy
A DNS query reveals the requested domain and record type to Cloudflare. FORGE-TS does not persist query history.
