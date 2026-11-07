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

## Research synthesis

The Undermind literature review completed with 268 relevant papers and technical publications identified. Its summary is based on available abstracts and metadata rather than a full-text review of every paper. The main cross-cutting findings used to shape the documentation are:

- Protocol behavior depends on deployment, policy, middleboxes, and implementation—not only on the base specification.
- Every protocol family has trade-offs between service guarantees, overhead, compatibility, and recovery speed.
- Transport performance is workload- and path-dependent; QUIC's stream and connection design does not remove congestion-control, CPU, or middlebox constraints.
- Routing documentation must explain policy and trust, not just path selection; RPKI/route-origin validation reduces some BGP risks but does not eliminate all routing threats.
- DNS privacy, DNS data integrity, and DNS availability are separate properties and should not be conflated.
- IoT protocols must be explained in the context of constrained devices, loss, power, and interoperability.
- Troubleshooting is strongest when end-to-end measurements are combined with routing, topology, and protocol-state evidence.

Selected literature:
- [Cla88] D. Clark, “The Design Philosophy of the DARPA Internet Protocols” (1988), DOI: https://doi.org/10.1145/52324.52336
- [Too22] O. van der Toorn et al., “Addressing the Challenges of Modern DNS: A Comprehensive Tutorial” (2022), DOI: https://doi.org/10.1016/j.cosrev.2022.100469
- [Lan17] A. Langley et al., “The QUIC Transport Protocol: Design and Internet-Scale Deployment” (2017), DOI: https://doi.org/10.1145/3098822.3098842
- [Pol18] “A Survey on Recent Advances in Transport Layer Protocols” (2018), DOI: https://doi.org/10.1109/COMST.2019.2932905
- [Dus16] A. Dusia and A. Sethi, “Recent Advances in Fault Localization in Computer Networks” (2016), DOI: https://doi.org/10.1109/COMST.2016.2570599
- [Mit18] A. Mitseva et al., “The State of Affairs in BGP Security: A Survey of Attacks and Defenses” (2018), DOI: https://doi.org/10.1016/J.COMCOM.2018.04.013
- [Eng20] T. Enghardt et al., “A Survey of the Interaction between Security Protocols and Transport Services” (RFC 8922, 2020), DOI: https://doi.org/10.17487/RFC8922
- [Rod24] N. Rodday et al., “The Resource Public Key Infrastructure (RPKI): A Survey on Measurements and Future Prospects” (2024), DOI: https://doi.org/10.1109/TNSM.2023.3327455

Undermind research workspace: https://app.undermind.ai/projects/76efa39c-3a4c-48b2-8ecf-95b9b98c4340?path=/Network%20Protocol%20Architecture%20and%20Operations
