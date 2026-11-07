# Phase 2 — Knowledge Center

## Objective
Deliver an offline-first learning area for MikroTik fundamentals, operating-system networking commands, and secure administration protocols.

## Included in this milestone
- Native Jetpack Compose screen reachable from the Learn navigation item.
- Search across titles, summaries, explanations and examples.
- Category filters: MikroTik, OS Commands, SSH & TLS, Addressing, Network Types, Core Protocols, Remote Access, Transport & Tunneling, VPN & Tunneling, and Proxy Protocols.
- Expandable reference cards with explanations and read-only examples.
- MikroTik fundamentals: RouterOS orientation, interfaces, IP addressing, routes, DHCP and firewall inspection.
- Operating-system references: interface configuration, routes, DNS, ping, path tracing and system information.
- SSH references: encrypted remote administration, host identity, key pairs and safe host-key handling.
- TLS references: HTTPS, handshake concepts, certificate identity, validity, issuer and verification.
- Addressing: IPv4, IPv6, CIDR, VLSM and binary notation.
- Network types: PAN, LAN, WLAN and VLAN, with capacity planning guidance.
- Protocols: DHCP/DHCPv6, NAT, CGNAT, TCP, UDP, TCP/IP, OSI, SSHWS, QUIC, WireGuard, OpenVPN, Shadowsocks/SSR, VLESS, VMess, Trojan proxy protocol, TUIC, Hysteria, IPsec and L2TP.
- Static content works without a network connection.

## Safety boundary
- This module is a learning/reference library, not a terminal or proxy client.
- Proxy and tunnel entries describe architecture and security properties; they do not provide traffic-evasion, access-control bypass or credential-harvesting instructions.
- Trojan proxy protocol is explicitly distinguished from Trojan horse malware.
- SSHWS is documented as a deployment pattern (SSH over WebSocket), not a separate IETF standard.
- It does not connect to routers, run commands, change device settings or store credentials.
- MikroTik examples are inspection-only.
- SSH private keys must remain private; never paste them into support requests or logs.
- Certificate verification must not be disabled to bypass trust or hostname errors.
- Network diagnostics must only be used on systems and networks the learner is authorized to inspect.

## Not included
- RouterOS remote management or command execution.
- SSH client sessions, key generation or credential storage inside FORGE-TS.
- Automated TLS scanning, certificate bypass or bulk endpoint probing.
- Downloaded/synchronized lesson catalog.
- User accounts, cloud progress tracking or Supabase integration.

## Verification
The GitHub Actions Android Debug APK workflow is the build gate. A successful workflow confirms compilation and artifact creation; it does not replace on-device functional testing.
