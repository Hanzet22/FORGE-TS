# Phase 11 — DNS Record Explorer & Protocol Guides

## Delivered scope
- Expanded the live DNS-over-HTTPS lookup from the original eight types to A, AAAA, CNAME, MX, TXT, NS, SRV, CAA, SOA, PTR, HTTPS, SVCB, DS, DNSKEY, TLSA, SSHFP, RRSIG and NSEC.
- Added selectable Cloudflare and Google Public DNS resolvers.
- Displays DNS response status, Answer / Authority / Additional sections, record type, owner name, TTL and elapsed request time.
- Requests DNSSEC data using the DO flag and displays the resolver-reported AD (Authenticated Data) flag without claiming that the app independently validates the DNSSEC chain.
- Added an offline Protocol Library with overview, usage guidance, pros, cons, operational/security risks and references.
- Retains local input validation, bounded request timeout, no query history persistence and provider disclosure.

## DNS lookup behavior
- The app is a DNS-over-HTTPS client, not an authoritative DNS server or recursive resolver.
- Cloudflare endpoint: `https://cloudflare-dns.com/dns-query`
- Google Public DNS endpoint: `https://dns.google/resolve`
- Both providers are selectable by the user; each receives the queried domain and record type.
- TTL is the DNS cache lifetime supplied in the response, not a guaranteed record-expiry timestamp.
- DNS response codes are rendered as NOERROR, FORMERR, SERVFAIL, NXDOMAIN, NOTIMP and REFUSED where applicable.
- The AD flag is reported as the resolver's assertion that data is authenticated. FORGE-TS does not perform independent DNSSEC chain validation in this phase.
- DNSSEC records such as DS, DNSKEY and RRSIG are inspectable, but their presence alone does not establish successful validation.
- HTTPS and SVCB records are returned as resolver-provided record data; this phase does not attempt to reinterpret every service parameter.

## Protocol guide
The Protocol Library provides educational and operational context for DNS, DHCP/DHCPv6, NAT/CGNAT, TCP, UDP, TCP/IP, OSI, SSH, SSH over WebSocket, HTTP, HTTPS/TLS, NTP, SMTP, IMAP, POP3, SMB, RDP, Trojan Proxy, VLESS, VMess, Shadowsocks, ShadowsocksR, TUIC, Hysteria, QUIC, WireGuard, OpenVPN, IPsec, L2TP, DoH and DNSSEC.

Each entry includes:
- What the protocol is for.
- High-level usage and configuration context.
- Advantages and limitations.
- Security, privacy and operational risks.
- Standards or project documentation references.

The library is informational. It does not configure DNS zones, execute remote commands, create proxy connections, establish VPN tunnels or bypass network controls.

## Privacy and safety
- DNS queries reveal the requested domain and record type to the selected resolver.
- DoH encrypts transport between the client and resolver; it does not make DNS queries anonymous and does not replace DNSSEC.
- The app does not store lookup history or send query history to FORGE-TS.
- Use DNS and protocol tools only for domains, systems and networks you own or are authorized to inspect.
- No provider API key, DNS zone write credential or service-role secret is embedded in the APK.

## References
- RFC 9499 — DNS Terminology: https://www.rfc-editor.org/rfc/rfc9499
- IANA DNS Parameters: https://www.iana.org/assignments/dns-parameters/dns-parameters.xhtml
- RFC 8484 — DNS over HTTPS: https://www.rfc-editor.org/rfc/rfc8484
- RFC 9364 — DNSSEC: https://www.rfc-editor.org/rfc/rfc9364
- RFC 9076 — DNS Privacy Considerations: https://www.rfc-editor.org/rfc/rfc9076
- RFC 9460 — SVCB and HTTPS records: https://www.rfc-editor.org/rfc/rfc9460
- RFC 9520 — Negative Caching of DNS Resolution: https://www.rfc-editor.org/rfc/rfc9520
- Cloudflare DNS JSON API: https://developers.cloudflare.com/1.1.1.1/encryption/dns-over-https/make-api-requests/
- Google Public DNS JSON API: https://developers.google.com/speed/public-dns/docs/doh/json
