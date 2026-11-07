# Phase 13 — Full Technical Protocol Documentation

## Delivered scope
- Expanded all **83 protocol and protocol-family entries** in Protocol Center with a protocol-specific technical chapter.
- Added eight technical chapters to each detail page:
  1. Architecture and component roles.
  2. Packet, frame, and message model.
  3. Operation lifecycle and state progression.
  4. Security model and trust boundaries.
  5. Performance characteristics and trade-offs.
  6. Deployment and interoperability considerations.
  7. Troubleshooting methodology.
  8. Primary specifications and references.
- Retained the original ten overview sections: explanation, technical summary, usage, function, advantages, limitations, operational hurdles, standards references, suitability, and environment.
- Kept the material offline and readable without a network connection.

## Documentation quality rules
- Technical details are tailored to each protocol; related protocols may share concepts but are not represented as interchangeable.
- Packet layouts and state sequences are described at a conceptual reference level. Version- and implementation-dependent behavior is explicitly called out.
- An RFC is a publication identifier, not automatically an Internet Standard. The detail view directs readers to verify the current RFC status, STD assignment, updates, obsoletes, and errata.
- IEEE, ISO/IEC, ITU-T, OASIS, and project specifications are identified separately from IETF RFC status.
- Internet-Drafts are work in progress and may be revised, replaced, or expire.
- Project-defined proxy protocols are documented from official project references where available; no RFC or standards-body status is invented for them.
- Security sections describe trust boundaries, limitations, and defensive controls. They do not provide attack procedures or instructions to bypass network policy.

## Research workflow and primary sources
The implementation research used Exa, Firecrawl, Tavily, Parallel Search, Geekflare, and an Undermind literature-review workspace. Search results were treated as discovery aids; primary specifications and official project documentation take precedence.

- RFC Editor: https://www.rfc-editor.org/
- IETF Datatracker: https://datatracker.ietf.org/
- IANA Protocol Registries: https://www.iana.org/protocols
- IANA Protocol Numbers: https://www.iana.org/assignments/protocol-numbers/
- IANA Service Name and Port Number Registry: https://www.iana.org/assignments/service-names-port-numbers/
- IEEE Standards: https://standards.ieee.org/
- ITU-T Recommendations: https://www.itu.int/rec/T-REC
- ISO Standards: https://www.iso.org/standards.html
- OASIS Standards: https://www.oasis-open.org/standards/
- HTTP Working Group: https://httpwg.org/specs/
- WireGuard: https://www.wireguard.com/
- OpenVPN: https://openvpn.net/community-resources/
- Project V: https://www.v2fly.org/
- Trojan-GFW: https://github.com/trojan-gfw/trojan
- Shadowsocks: https://shadowsocks.org/
- TUIC: https://github.com/EAimTY/tuic
- Hysteria: https://v2.hysteria.network/

## Representative primary specifications
- TCP: RFC 9293 / STD 7 — https://www.rfc-editor.org/rfc/rfc9293
- QUIC: RFC 9000–9002 — https://www.rfc-editor.org/rfc/rfc9000
- TLS 1.3: RFC 8446 — https://www.rfc-editor.org/rfc/rfc8446
- DNS: RFC 1034 and RFC 1035 — https://www.rfc-editor.org/rfc/rfc1034
- DNS over QUIC: RFC 9250 — https://www.rfc-editor.org/rfc/rfc9250
- BGP: RFC 4271 — https://www.rfc-editor.org/rfc/rfc4271
- OSPFv2: RFC 2328 / STD 54 — https://www.rfc-editor.org/rfc/rfc2328
- MPLS: RFC 3031 — https://www.rfc-editor.org/rfc/rfc3031
- VXLAN: RFC 7348 — https://www.rfc-editor.org/rfc/rfc7348
- Geneve: RFC 8926 — https://www.rfc-editor.org/rfc/rfc8926
- NTP Security (NTS): RFC 8915 — https://www.rfc-editor.org/rfc/rfc8915
- MQTT 5.0: OASIS specification — https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html

## Safety and privacy boundaries
- Protocol Center is a learning/reference feature. It does not configure devices, run shell commands, establish VPN/proxy tunnels, or modify network policy.
- Host checks remain limited to systems the user owns or is authorized to inspect.
- Protocol security is not a guarantee of secure endpoints, correct authorization, anonymity, or safe deployment.
- Production decisions require checking the exact protocol version, implementation, vendor support, threat model, and applicable organizational policy.

## Files
- `ProtocolDeepDocs.kt`: offline technical chapters for all 83 entries.
- `ProtocolCenterScreen.kt`: dedicated detail sections and standards-status verification guidance.
