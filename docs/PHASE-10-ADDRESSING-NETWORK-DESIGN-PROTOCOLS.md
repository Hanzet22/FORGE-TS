# Phase 10 — Addressing, Network Design & Protocol Knowledge

## Objective
Expand FORGE-TS into an offline-first learning and planning workspace for addressing, subnet design and modern networking protocols.

## Addressing and subnet planning
- IPv4 and IPv6 CIDR calculation.
- IPv4 subnet mask, network address, broadcast address (only where applicable), first/last usable host and usable host count.
- Total address count and binary representation for IPv4 and IPv6.
- IPv4 VLSM allocation from a parent prefix, with largest-first sorting, block alignment and address-space exhaustion checks.
- Separate IP capacity estimator for PAN, LAN, WLAN and VLAN based on expected clients, reserved addresses and growth allowance.
- IPv6 planning note: conventional LAN/VLAN subnet size is /64; IPv6 has no broadcast address.

## Network and protocol reference
- Network types: PAN, LAN, WLAN and VLAN.
- Addressing/services: DHCP/DHCPv6, NAT and CGNAT.
- Models/transports: TCP, UDP, TCP/IP, OSI and QUIC.
- Remote access/security: SSH, SSH over WebSocket (SSHWS), TLS, IPsec and L2TP.
- VPN: WireGuard and OpenVPN.
- Proxy protocol references: Shadowsocks, ShadowsocksR, VLESS, VMess, Trojan proxy protocol, TUIC and Hysteria.

## Accuracy notes
- PAN/LAN/WLAN/VLAN do not have fixed universal IP quotas. Address capacity is based on actual device and infrastructure requirements.
- A routed VLAN commonly has its own Layer 3 subnet; WLAN segmentation depends on SSID, policy and topology.
- IPv4 /31 is treated as a two-address point-to-point subnet per RFC 3021; /32 represents a single host route.
- IPv6 does not use broadcast and IPv6 host counts are not calculated by subtracting network and broadcast addresses.
- SSHWS is a deployment pattern that carries SSH through WebSocket; it is not a separate IETF SSH protocol.
- Trojan in the proxy-protocol section means the TLS-based Trojan proxy protocol, not Trojan horse malware.
- VLESS, VMess, Shadowsocks/R, TUIC and Hysteria are described as project protocol families; do not imply that they are IETF standards.
- QUIC is a secure transport carried over UDP; it is not simply another name for UDP.

## Safety and privacy
- Calculations and bundled references work locally and do not upload entered addresses or network plans.
- This milestone adds no VPN client, proxy client, tunnel establishment, traffic interception, remote shell or network-control bypass feature.
- Protocol descriptions focus on architecture, intended roles, security properties and limitations.
- Do not store private keys, passwords, tokens or proxy credentials in lesson examples.

## Source references
- IPv4: RFC 791 — https://www.rfc-editor.org/rfc/rfc791.html
- IPv6: RFC 8200 and RFC 4291 — https://www.rfc-editor.org/rfc/rfc8200.html ; https://www.rfc-editor.org/rfc/rfc4291.html
- CIDR: RFC 4632 — https://www.rfc-editor.org/rfc/rfc4632.html
- IPv4 /31: RFC 3021 — https://www.rfc-editor.org/rfc/rfc3021.html
- DHCP: RFC 2131 — https://www.rfc-editor.org/rfc/rfc2131.html
- CGN shared address space: RFC 6598 — https://www.rfc-editor.org/rfc/rfc6598.html
- TCP / UDP: RFC 9293 and RFC 768 — https://www.rfc-editor.org/rfc/rfc9293.html ; https://www.rfc-editor.org/rfc/rfc768.html
- TCP/IP host requirements: RFC 1122 — https://www.rfc-editor.org/rfc/rfc1122.html
- OSI: ITU-T X.200 — https://www.itu.int/rec/T-REC-X.200/en
- SSH: RFC 4251 — https://www.rfc-editor.org/rfc/rfc4251.html
- TLS 1.3: RFC 8446 — https://www.rfc-editor.org/rfc/rfc8446.html
- QUIC: RFC 9000 and RFC 9001 — https://www.rfc-editor.org/rfc/rfc9000.html ; https://www.rfc-editor.org/rfc/rfc9001.html
- IPsec: RFC 4301 — https://www.rfc-editor.org/rfc/rfc4301.html
- L2TP: RFC 2661 and RFC 3193 — https://www.rfc-editor.org/rfc/rfc2661.html ; https://www.rfc-editor.org/rfc/rfc3193.html
- VLAN: IEEE 802.1Q — https://www.ieee802.org/1/pages/802.1Q.html
- WireGuard: https://www.wireguard.com/protocol/
- OpenVPN: https://openvpn.net/community-docs/openvpn-protocol/
- Shadowsocks: https://github.com/Shadowsocks-NET/shadowsocks-specs
- VLESS / VMess / Trojan: https://xtls.github.io/en/config/outbounds/vless.html ; https://xtls.github.io/en/config/outbounds/vmess.html ; https://github.com/trojan-gfw/trojan/blob/master/docs/protocol.md
- TUIC: https://github.com/tuic-protocol/tuic/blob/master/SPEC.md
- Hysteria 2: https://hysteria.network/docs/developers/Protocol/

## Verification
The implementation was committed directly to the main branch. Build verification is not claimed until a single Android Debug APK workflow completes successfully.
