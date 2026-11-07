# Phase 12 — Dedicated Protocol Center

## Delivered scope
- Added a dedicated **Protocol Center** tab alongside DNS Lookup and the earlier Protocol Library.
- Added a searchable and category-filtered catalog containing 80+ protocol and protocol-family profiles.
- Categories cover addressing and discovery, naming, transport, routing and traffic engineering, switching and LAN, multicast, VPN/security, tunneling and overlays, application services, identity/AAA, management, IoT messaging, voice and real-time communication, remote access and proxy protocol families.
- Each profile provides: explanation, technical details, usage, function, pros, cons, operational hurdles, standards/draft/registry references, suitable and unsuitable scenarios, and recommended environment.
- The earlier Protocol Library remains available; Protocol Center is an expanded, dedicated reference experience.

## Documentation model
The catalog distinguishes protocol specifications from implementations and deployment patterns. An RFC is a document identifier, not automatically an Internet Standard. The relevant status may be Internet Standard, Proposed Standard, Experimental, Informational, Internet-Draft, IEEE/ISO/ITU-T standard, IANA registry entry, or project specification. Internet-Drafts can expire or change and must not be presented as finalized standards.

Where a project-specific protocol has no IETF standard (for example, VLESS, VMess, Trojan Proxy, TUIC, Hysteria or Shadowsocks variants), the entry identifies project documentation rather than inventing an RFC. SSH over WebSocket is a deployment pattern combining SSH and WebSocket, not a separate standardized SSH protocol. SFTP is an SSH subsystem and must not be described as FTP over TLS.

## Technical and safety boundaries
- The catalog is offline educational reference material; it does not configure routers, switches, DNS zones, VPNs, proxies, AAA systems or remote hosts.
- Suitability notes are contextual, not universal prescriptions. Actual deployment depends on version, implementation, vendor support, topology, threat model, policy and operational requirements.
- Protocol-level encryption does not automatically provide anonymity, end-to-end security or safe endpoints.
- VPN/proxy entries are descriptive only. The app does not establish tunnels or bypass network controls.
- Standards status and implementation support can change; users should consult the linked primary specification before production deployment.

## Primary reference sources
- IETF RFC Editor: https://www.rfc-editor.org/
- IETF Datatracker (RFCs and Internet-Drafts): https://datatracker.ietf.org/
- IANA Protocol Registries: https://www.iana.org/protocols
- IANA Assigned Internet Protocol Numbers: https://www.iana.org/assignments/protocol-numbers/
- IANA Service Name and Transport Protocol Port Number Registry: https://www.iana.org/assignments/service-names-port-numbers/
- IEEE 802 standards: https://standards.ieee.org/
- ITU-T Recommendations: https://www.itu.int/rec/T-REC
- ISO standards catalogue: https://www.iso.org/standards.html
- HTTP Working Group specifications: https://httpwg.org/specs/
- OpenVPN documentation: https://openvpn.net/community-resources/
- WireGuard protocol and documentation: https://www.wireguard.com/
- Project X documentation: https://www.v2fly.org/
- Trojan-GFW project: https://github.com/trojan-gfw/trojan
- Shadowsocks documentation: https://shadowsocks.org/
- TUIC project: https://github.com/EAimTY/tuic
- Hysteria documentation: https://v2.hysteria.network/

## Selected standards examples
- IPv4: RFC 791; IPv6: RFC 8200 and RFC 4291.
- ARP: RFC 826; ICMPv4: RFC 792; ICMPv6: RFC 4443; IPv6 Neighbor Discovery: RFC 4861.
- DNS: RFC 1034 and RFC 1035; DNSSEC: RFC 4033–4035 and RFC 9364.
- DHCPv4: RFC 2131; DHCPv6: RFC 8415; NAT: RFC 3022; CGNAT shared address space: RFC 6598.
- TCP: RFC 9293; UDP: RFC 768; QUIC: RFC 9000–9002.
- TLS 1.3: RFC 8446; DTLS 1.3: RFC 9147; IPsec architecture: RFC 4301; IKEv2: RFC 7296.
- BGP: RFC 4271; OSPFv2: RFC 2328; OSPFv3: RFC 5340; RIPv2: RFC 2453; IS-IS: ISO/IEC 10589 and RFC 1195.
- MPLS: RFC 3031; LDP: RFC 5036; GRE: RFC 2784; VXLAN: RFC 7348; Geneve: RFC 8926.
- PPP: RFC 1661; PPPoE: RFC 2516; VLAN: IEEE 802.1Q; Ethernet: IEEE 802.3; Wi-Fi: IEEE 802.11; LLDP: IEEE 802.1AB; LACP: IEEE 802.1AX.
- HTTP semantics: RFC 9110; HTTP/1.1: RFC 9112; HTTP/2: RFC 9113; HTTP/3: RFC 9114; WebSocket: RFC 6455.
- SNMP framework: RFC 3411; Syslog: RFC 5424; NETCONF: RFC 6241; RESTCONF: RFC 8040.
- LDAP: RFC 4511; RADIUS: RFC 2865; TACACS+: RFC 8907; Kerberos: RFC 4120.
- MQTT: OASIS MQTT 5.0; CoAP: RFC 7252; AMQP 1.0: OASIS specification; RTP/RTCP: RFC 3550; SIP: RFC 3261.


## Phase 13 — Full technical documentation
The Protocol Center now provides eight additional protocol-specific technical chapters for all 83 entries: architecture, wire/message model, operation lifecycle, security, performance, deployment/interoperability, troubleshooting, and primary references. See [Phase 13](PHASE-13-FULL-TECHNICAL-PROTOCOL-DOCUMENTATION.md).
