# Phase 3 — DNS & Protocols

## Delivered scope
- DNS-over-HTTPS lookup through Google Public DNS's JSON endpoint.
- Original supported query types: A, AAAA, CNAME, MX, TXT, NS, SRV and CAA. Phase 11 expands the list; see the Phase 11 document.
- Input normalization and validation, bounded request timeout, DNS status and answer display.
- Provider disclosure and explicit notice that the queried domain/type are sent to the selected resolver.
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
- Google Public DNS over HTTPS is the sole configured provider; the app does not claim to be an authoritative or recursive resolver.

## Privacy
A DNS query reveals the requested domain and record type to Google Public DNS. FORGE-TS does not persist query history.
