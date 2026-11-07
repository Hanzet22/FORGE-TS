package com.HG.Forge.Ts.ui.screens

/**
 * Protocol-specific technical chapters for the offline Protocol Center.
 * References point to primary specifications or official project documentation.
 * RFC publication does not, by itself, mean Internet Standard status.
 */
internal data class ProtocolDeepDoc(
    val architecture: String,
    val wireModel: String,
    val lifecycle: String,
    val security: String,
    val performance: String,
    val operations: String,
    val troubleshooting: String,
    val references: String
)

private fun chapter(
    architecture: String,
    wireModel: String,
    lifecycle: String,
    security: String,
    performance: String,
    operations: String,
    troubleshooting: String,
    references: String
) = ProtocolDeepDoc(architecture, wireModel, lifecycle, security, performance, operations, troubleshooting, references)

internal val protocolDeepDocs: Map<String, ProtocolDeepDoc> = mapOf(
    "ARP" to chapter(
        "ARP operates at the IPv4-to-link-layer boundary. A host resolves the hardware address of an on-link destination or its configured next-hop router; it does not discover remote hosts across routed boundaries.",
        "An Ethernet ARP message carries hardware and protocol address types and lengths, an operation code, sender hardware/protocol addresses, and target hardware/protocol addresses. Ethernet EtherType 0x0806 identifies ARP.",
        "A sender broadcasts a request for an IPv4 address. The owner normally returns a unicast reply; the requester updates its neighbor cache and later ages or replaces the entry according to implementation policy.",
        "Classic ARP has no cryptographic peer authentication. Untrusted link participants can send misleading announcements, so segmentation, switch protections, static bindings where justified, and monitoring are defense-in-depth controls.",
        "A cache hit avoids a broadcast exchange. Cache misses add local-link latency; excessive churn or large broadcast domains increase background traffic and complicate convergence.",
        "Keep address plans and DHCP reservations consistent. Understand proxy ARP, gratuitous ARP, VLAN scope, virtual IP failover, and duplicate-address detection behavior on the chosen platform.",
        "If a host can reach its gateway by IP but not a local peer, inspect the neighbor entry, VLAN membership, duplicate addresses, and switch port security before assuming an IP routing fault.",
        "Primary reference: RFC 826 — https://www.rfc-editor.org/rfc/rfc826"
    ),
    "ICMPv4" to chapter(
        "ICMPv4 is the control and error-reporting companion to IPv4. It is carried inside IPv4 packets and informs hosts and routers about delivery conditions; it is not an application transport.",
        "IPv4 Protocol field value 1 identifies ICMP. The common message layout contains Type, Code, Checksum, and type-specific fields; error messages may include part of the triggering packet.",
        "Routers or hosts generate errors such as Destination Unreachable and Time Exceeded. Echo Request/Reply supports diagnostics, while path-MTU mechanisms rely on relevant error signaling.",
        "ICMP errors can reveal topology or be abused for reflection when poorly handled. Apply rate limits and filtering carefully; blanket blocking can break path-MTU discovery and diagnostics.",
        "Messages are small, but diagnostic accuracy depends on rate limits, routing symmetry, and middlebox policy. ICMP response time is not equivalent to application latency.",
        "Permit necessary error signaling between trusted network boundaries. Interpret Type and Code values with the relevant RFC rather than treating every ICMP packet as a ping.",
        "A failed ping does not prove a host is down. Compare route behavior, TCP/UDP service reachability, and specific ICMP Type/Code responses.",
        "Primary reference: RFC 792 — https://www.rfc-editor.org/rfc/rfc792 ; IANA Protocol Numbers — https://www.iana.org/assignments/protocol-numbers/"
    ),
    "ICMPv6" to chapter(
        "ICMPv6 is integral to IPv6 operation: it provides error reporting, diagnostics, and messages used by Neighbor Discovery and other IPv6 mechanisms.",
        "IPv6 Next Header value 58 identifies ICMPv6. Messages begin with Type, Code, and Checksum followed by type-specific data; the checksum includes an IPv6 pseudo-header.",
        "Hosts and routers exchange error and informational messages as packets are processed. Neighbor Discovery messages use ICMPv6 and have their own hop-limit and link-scope requirements.",
        "Filtering essential ICMPv6 can silently break Path MTU Discovery and neighbor functions. Apply scoped firewall policy, validate source/link context, and use RA Guard or equivalent protections where appropriate.",
        "Control traffic is low volume in healthy networks. Loss of key messages can cause black holes, slow connection setup, or apparently random failures with large packets.",
        "Document which ICMPv6 types are allowed at each boundary. Treat Echo Request separately from Packet Too Big, Destination Unreachable, Time Exceeded, and Neighbor Discovery traffic.",
        "When small packets work but larger transfers stall, check Packet Too Big delivery and path MTU. For local reachability, inspect NDP and router advertisements.",
        "Primary reference: RFC 4443 — https://www.rfc-editor.org/rfc/rfc4443 ; IPv6 architecture: RFC 8200 — https://www.rfc-editor.org/rfc/rfc8200"
    ),
    "IPv6 Neighbor Discovery (NDP)" to chapter(
        "NDP provides IPv6 on-link neighbor resolution, router discovery, prefix discovery, and reachability confirmation. It replaces ARP and IPv4 router discovery functions but is carried by ICMPv6.",
        "Router Solicitation/Advertisement and Neighbor Solicitation/Advertisement messages carry options such as source/target link-layer address, prefix information, MTU, and route information.",
        "A host learns routers and prefixes from Router Advertisements, resolves a neighbor with Neighbor Solicitation, and confirms reachability using Neighbor Unreachability Detection. Duplicate Address Detection checks tentative addresses.",
        "Rogue Router Advertisements can alter a host's default route or prefixes. Use managed switch RA Guard, sensible trust boundaries, SEND where supported, and monitoring of unexpected advertisements.",
        "NDP is link-local control traffic. Large L2 domains, excessive multicast, slow neighbor resolution, or overly aggressive reachability timers can affect startup and failover behavior.",
        "Plan Router Advertisement lifetimes, prefix lifetimes, managed/other flags, link-local addressing, and DHCPv6 interaction as one design. NDP is not an inter-domain routing protocol.",
        "Inspect RA visibility, neighbor-cache state, Duplicate Address Detection results, and ICMPv6 filtering. A missing default route often points to RA handling rather than DHCPv6.",
        "Primary reference: RFC 4861 — https://www.rfc-editor.org/rfc/rfc4861 ; SLAAC: RFC 4862 — https://www.rfc-editor.org/rfc/rfc4862"
    ),
    "DNS" to chapter(
        "DNS is a hierarchical distributed database. Stub resolvers ask recursive resolvers, which follow referrals through the root, top-level-domain, and authoritative server hierarchy or use cached data.",
        "DNS messages have a 12-byte header, question section, and resource-record sections. Records contain owner name, type, class, TTL, RDLENGTH, and type-specific RDATA; name compression is defined by the base protocol.",
        "A resolver checks local policy/cache, queries upstream servers, follows referrals, and returns an answer with TTL. Negative answers are cached under defined rules; TCP is also required for some responses and zone transfers.",
        "Classic DNS is not inherently confidential or authenticated. DNSSEC authenticates data origin and integrity, not secrecy; encrypted transports protect the channel but do not replace DNSSEC validation.",
        "Caching reduces latency and upstream load but creates propagation delay. Large responses, truncation, DNSSEC records, resolver distance, and cache hit rate affect response behavior.",
        "Separate authoritative and recursive roles where appropriate. Maintain delegation, glue, TTL, split-horizon policy, redundancy, and resolver access controls; use current IANA registries for record types.",
        "Check the exact resolver, response code, authoritative flag, delegation chain, TTL, and DNSSEC validation state. Compare authoritative answers with recursive cached answers.",
        "Core references: RFC 1034 — https://www.rfc-editor.org/rfc/rfc1034 ; RFC 1035 — https://www.rfc-editor.org/rfc/rfc1035 ; DNS terminology: RFC 9499 — https://www.rfc-editor.org/rfc/rfc9499 ; IANA DNS parameters — https://www.iana.org/assignments/dns-parameters/"
    ),
    "DHCPv4" to chapter(
        "DHCPv4 centrally assigns IPv4 addresses and configuration parameters. A server may serve directly connected clients or receive relayed requests from other IP subnets.",
        "Client/server messages use UDP ports 68/67 and include BOOTP-compatible fields, transaction ID, client hardware address, flags, and typed options terminated by an End option.",
        "The common DORA sequence is Discover, Offer, Request, Acknowledge. Renewals use lease timers; clients may rebind when the original server is unavailable.",
        "DHCP does not authenticate ordinary clients or servers by default. Rogue servers can supply harmful network settings; use trusted switch ports, DHCP snooping, and controlled relay paths.",
        "Broadcast discovery is local to a segment unless relayed. Lease duration, pool size, relay delay, and server redundancy determine address-allocation resilience.",
        "Plan non-overlapping scopes, exclusions, reservations, option consistency, lease duration, relay addresses, and failover behavior. Keep DNS registration and IPAM data aligned.",
        "A client stuck at self-assigned addressing may not receive an Offer or ACK. Check VLAN, relay/gateway address, pool exhaustion, server logs, and snooping counters.",
        "Primary references: RFC 2131 — https://www.rfc-editor.org/rfc/rfc2131 ; options: RFC 2132 — https://www.rfc-editor.org/rfc/rfc2132"
    ),
    "DHCPv6" to chapter(
        "DHCPv6 provides stateful address assignment, configuration options, and prefix delegation. It coexists with IPv6 Router Advertisements and SLAAC rather than replacing all router-discovery behavior.",
        "UDP ports 546 and 547 are used by clients and servers/relays. DUID identifies a client or server; IA_NA, IA_TA, and IA_PD represent different identity-association purposes.",
        "Solicit, Advertise, Request, and Reply form a common exchange. Renew, Rebind, Confirm, Release, and Decline support lease lifecycle and address validation.",
        "DHCPv6 does not by itself authenticate every network participant. Protect server/relay paths and use link-layer controls; do not mistake DHCPv6 for protection against rogue Router Advertisements.",
        "Prefix delegation can simplify downstream router addressing but depends on pool sizing and lease lifecycle. Relay topology and client DUID persistence affect operational behavior.",
        "Coordinate DHCPv6 mode with Router Advertisement M/O flags, DNS options, prefix delegation, and firewall rules. Document whether clients use SLAAC, stateful DHCPv6, or both.",
        "Check whether the client is requesting IA_NA or IA_PD, whether the relay reaches the correct server, and whether RA policy permits the expected IPv6 behavior.",
        "Primary reference: RFC 8415 — https://www.rfc-editor.org/rfc/rfc8415"
    ),
    "NAT" to chapter(
        "Network Address Translation rewrites address and sometimes transport-port information between addressing realms. Basic NAT and NAPT differ in whether translation includes transport identifiers.",
        "A translation device tracks tuples and state for flows, rewrites packet headers, and updates checksums. Mapping and filtering behavior varies by implementation and NAT type.",
        "Outbound traffic creates or matches a translation binding; return traffic is mapped to the internal endpoint while the binding remains valid. Static mappings provide explicit inbound publication.",
        "NAT is not a firewall or encryption mechanism. State exposure, weak filtering, logging gaps, and application-layer address embedding can create security and privacy problems.",
        "Translation adds state and processing overhead. Port exhaustion, table size, timeouts, hairpin behavior, and asymmetric paths can become capacity or reliability limits.",
        "Document inside/outside interfaces, address pools, port ranges, timeouts, static mappings, logging, and routing symmetry. Preserve end-to-end firewall policy independently.",
        "For one-way connectivity, compare translation state, return routing, timeout, port allocation, and checksum handling. Verify whether the issue is NAT or a separate firewall rule.",
        "Foundational reference: RFC 3022 — https://www.rfc-editor.org/rfc/rfc3022 ; IANA protocol numbers — https://www.iana.org/assignments/protocol-numbers/"
    ),
    "CGNAT" to chapter(
        "Carrier-Grade NAT shares provider-controlled public IPv4 addresses among many subscribers. It is a provider-scale address/port translation layer, not a feature that an end user normally controls.",
        "Large-scale NAT maps subscriber-side addresses and transport ports to shared public addresses. The shared address block 100.64.0.0/10 is reserved for this purpose by RFC 6598.",
        "Subscriber flows create time-bounded mappings in provider equipment. Return packets are associated with subscriber state; providers may use deterministic port allocation or logging systems.",
        "CGNAT concentrates trust and attribution responsibilities at the provider. Subscriber identity cannot be inferred from a public IP alone; timestamp and source-port records may be needed under lawful processes.",
        "Capacity depends on public IPv4 inventory, port utilization, subscriber concurrency, state-table scale, and logging. Port exhaustion or restrictive mapping behavior can affect applications.",
        "Providers should plan address pools, port blocks, state capacity, logging retention, abuse response, and IPv6 transition. Subscribers should understand that unsolicited inbound reachability may not be available.",
        "If multiple services fail only for inbound connections, determine whether the access provider uses CGNAT and whether a supported public-address service exists.",
        "Primary reference: RFC 6598 — https://www.rfc-editor.org/rfc/rfc6598"
    ),
    "IPv4" to chapter(
        "IPv4 is a connectionless network-layer protocol using 32-bit addresses. Routers forward datagrams independently according to routing tables and longest-prefix matching.",
        "The base header includes version, IHL, DSCP/ECN, total length, identification, flags, fragment offset, TTL, protocol, header checksum, and source/destination addresses.",
        "A host selects a route and next hop, resolves the local link address, and sends the datagram. TTL decreases at routers; fragmentation may occur subject to flags and path behavior.",
        "IPv4 has no built-in cryptographic authentication or confidentiality. Source-address filtering, secure routing, host firewalls, and higher-layer encryption provide complementary protections.",
        "A 32-bit address space encourages subnetting and address conservation. Header checksum and possible fragmentation add processing and operational considerations.",
        "Use CIDR-based allocation, route summarization, private/public address separation, DHCP/IPAM, MTU planning, and ingress/egress filtering. Avoid overlapping private networks across connected sites.",
        "Check prefix length, default route, ARP, TTL expiry, fragmentation-needed signaling, and ACLs. Distinguish address reachability from application availability.",
        "Primary reference: RFC 791 — https://www.rfc-editor.org/rfc/rfc791 ; CIDR: RFC 4632 — https://www.rfc-editor.org/rfc/rfc4632"
    ),
    "IPv6" to chapter(
        "IPv6 expands network addressing to 128 bits and simplifies the base header. Extension headers carry optional functions; routers forward packets without fragmenting them.",
        "The fixed 40-byte base header contains Version, Traffic Class, Flow Label, Payload Length, Next Header, Hop Limit, and 128-bit source/destination addresses.",
        "Hosts form addresses through static assignment, SLAAC, DHCPv6, or combinations. NDP discovers routers and neighbors; routers forward based on prefixes and Hop Limit.",
        "IPv6 is not secure merely because addresses are large. Use stateful/stateless firewall policy, secure NDP/RA design, routing controls, and application-layer encryption.",
        "Large address space supports hierarchical allocation. Extension-header handling, dual-stack operation, PMTU, and monitoring can affect performance and operational complexity.",
        "Plan global-unicast and ULA use, prefix delegation, reverse DNS, RA policy, DHCPv6 interaction, DNS AAAA, and security telemetry. Avoid treating NAT as a mandatory IPv6 security layer.",
        "Check RA, NDP cache, route table, DNS AAAA, ICMPv6 Packet Too Big, and extension-header handling when IPv6-only paths fail.",
        "Primary references: RFC 8200 — https://www.rfc-editor.org/rfc/rfc8200 ; addressing: RFC 4291 — https://www.rfc-editor.org/rfc/rfc4291"
    ),
    "TCP" to chapter(
        "TCP provides a reliable, ordered byte stream between two endpoints. It maintains connection state and uses sequence numbers, acknowledgments, retransmission, flow control, and congestion control.",
        "The TCP header contains source/destination ports, sequence and acknowledgment numbers, data offset, control flags, receive window, checksum, urgent pointer, and optional parameters.",
        "A normal active open uses SYN, SYN-ACK, and ACK. Data is sequenced and acknowledged; FIN closes one direction gracefully, while RST aborts a connection.",
        "TCP does not encrypt application data. Use TLS or another authenticated security layer; protect against spoofing and resource exhaustion with current stacks and sensible service limits.",
        "Throughput depends on congestion window, receive window, RTT, loss, and path capacity. Head-of-line blocking within one byte stream means later bytes wait behind missing earlier data.",
        "Tune only after measuring RTT, bandwidth-delay product, retransmissions, and CPU. Keep kernel stacks current and expose connection-state metrics rather than guessing from a single speed test.",
        "Inspect handshake completion, retransmissions, duplicate ACKs, zero windows, resets, and path MTU. A successful TCP handshake does not prove the application is healthy.",
        "Current base specification: RFC 9293 / STD 7 — https://www.rfc-editor.org/rfc/rfc9293 ; congestion control: RFC 5681 — https://www.rfc-editor.org/rfc/rfc5681"
    ),
    "UDP" to chapter(
        "UDP provides a minimal connectionless datagram service. It preserves message boundaries but does not provide delivery, ordering, retransmission, or congestion control by itself.",
        "The UDP header is eight bytes: source port, destination port, length, and checksum. It is encapsulated directly in IP and identified by the UDP protocol number.",
        "An application sends datagrams to a destination port and receives independent datagrams. Applications that need reliability, sequencing, or congestion control must implement or adopt those mechanisms.",
        "UDP source addresses can be spoofed in some network contexts. Services should avoid amplification, validate requests where appropriate, and implement rate and size controls.",
        "Low header overhead and no handshake can reduce latency, but loss, fragmentation, and application-managed recovery affect real performance. Congestion control remains essential.",
        "Use UDP when message semantics and application behavior fit. Consider PMTU, datagram size, NAT mapping lifetime, and network policy; do not assume UDP is automatically faster.",
        "Check socket binding, port reachability, NAT timeout, packet loss, fragmentation, and application-level acknowledgments. A port scan or ping alone cannot establish UDP service health.",
        "Primary reference: RFC 768 — https://www.rfc-editor.org/rfc/rfc768 ; UDP usage guidance: RFC 8085 — https://www.rfc-editor.org/rfc/rfc8085"
    ),
    "QUIC" to chapter(
        "QUIC is a secure transport over UDP. It offers multiple independently flow-controlled streams, connection migration, and an integrated TLS 1.3 handshake.",
        "QUIC packets contain packet headers and frames. Frames carry stream data, acknowledgments, flow-control updates, path validation, and connection management; connection IDs decouple connections from a single address tuple.",
        "Endpoints negotiate version and transport parameters during the handshake, establish packet protection keys, open streams, acknowledge data, detect loss, and close or migrate connections as needed.",
        "QUIC encrypts most transport metadata and authenticates packets. 0-RTT data can be replayable, so applications must restrict early data to replay-safe operations.",
        "Independent streams avoid TCP's cross-stream head-of-line blocking, but congestion control still applies at connection level. UDP blocking, CPU cryptography, and packet pacing affect results.",
        "Ensure UDP reachability, modern TLS libraries, observability that understands QUIC, and compatible load-balancer behavior. Do not treat QUIC as a transparent TCP replacement.",
        "Compare UDP path availability, handshake errors, version negotiation, loss recovery, and connection migration. Middleboxes may hide encrypted transport details from legacy monitors.",
        "Core: RFC 9000 — https://www.rfc-editor.org/rfc/rfc9000 ; TLS mapping: RFC 9001 — https://www.rfc-editor.org/rfc/rfc9001 ; recovery: RFC 9002 — https://www.rfc-editor.org/rfc/rfc9002"
    ),
    "TLS 1.3" to chapter(
        "TLS 1.3 is a security protocol used above a reliable transport to authenticate peers and protect application records. It separates handshake negotiation from protected application traffic.",
        "Handshake messages negotiate protocol version, cipher suite, key shares, certificates, extensions, and Finished verification. Application data is carried in authenticated-encrypted records.",
        "ClientHello and ServerHello establish handshake secrets; authentication and key confirmation follow; application traffic uses derived traffic secrets with key updates as needed.",
        "Use certificate validation, hostname verification, trusted roots, secure private-key handling, and modern cipher suites. Disable obsolete protocol versions and avoid unsafe early-data assumptions.",
        "Session resumption and modern key exchange reduce setup cost. Certificate chains, public-key operations, record sizing, and handshake round trips influence latency and CPU.",
        "Automate certificate renewal, monitor expiry, use correct SNI/ALPN, and maintain supported TLS libraries. TLS protects a channel but does not repair insecure application authorization.",
        "Check certificate chain, hostname, clock, trust store, SNI, ALPN, version negotiation, and intermediate certificates before changing cipher policy.",
        "Primary reference: RFC 8446 — https://www.rfc-editor.org/rfc/rfc8446 ; deployment guidance: RFC 9325 — https://www.rfc-editor.org/rfc/rfc9325"
    ),
    "DTLS" to chapter(
        "DTLS adapts TLS security properties to datagram transports. It preserves datagram semantics rather than turning the application into a reliable byte stream.",
        "DTLS records carry epoch and sequence information to support replay handling and record association. Handshake messages may be fragmented and retransmitted over lossy paths.",
        "Peers negotiate parameters and keys, authenticate as configured, then exchange protected datagrams. Handshake retransmission timers account for loss and reordering.",
        "Use certificate or PSK authentication appropriately, enforce replay protection, and validate peer identity. Datagram security does not prevent application-level abuse or denial of service.",
        "Loss and reordering can trigger handshake retransmissions. MTU and record fragmentation are important; DTLS does not guarantee delivery of application datagrams.",
        "Use for applications that need datagram semantics with cryptographic protection. Validate NAT traversal, retransmission behavior, and library interoperability.",
        "Check handshake retransmissions, certificate validation, datagram size, firewall policy, and clock/certificate state. Separate transport loss from authentication failure.",
        "DTLS 1.3: RFC 9147 — https://www.rfc-editor.org/rfc/rfc9147 ; DTLS 1.2: RFC 6347 — https://www.rfc-editor.org/rfc/rfc6347"
    ),
    "IPsec" to chapter(
        "IPsec is a network-layer security architecture for protecting IP traffic between hosts or security gateways. Security policy determines which packets are bypassed, discarded, or protected.",
        "ESP carries a Security Parameters Index, sequence number, encrypted/authenticated payload, and optional integrity data. AH authenticates selected IP fields but is less common in NAT-heavy deployments.",
        "IKE negotiates peer authentication and Security Associations. The data plane then applies outbound and inbound policies, sequence checks, and cryptographic processing.",
        "Use modern authenticated encryption, strong peer identity, secure key lifecycle, anti-replay windows, and narrow traffic selectors. Misconfigured selectors can expose traffic or break connectivity.",
        "Encryption and encapsulation add CPU and MTU overhead. Hardware acceleration, packet size, tunnel count, rekey frequency, and path MTU influence throughput.",
        "Plan route-based or policy-based design, identity, certificate lifecycle, NAT traversal, rekey timers, failover, and logging. Keep IKE control-plane access restricted.",
        "Investigate IKE negotiation separately from ESP data flow. Check peer identity, proposals, traffic selectors, NAT-T, SA lifetime, routing symmetry, and MTU.",
        "Architecture: RFC 4301 — https://www.rfc-editor.org/rfc/rfc4301 ; ESP: RFC 4303 — https://www.rfc-editor.org/rfc/rfc4303 ; IKEv2: RFC 7296 — https://www.rfc-editor.org/rfc/rfc7296"
    ),
    "WireGuard" to chapter(
        "WireGuard is a layer-3 VPN protocol built around peer public keys and cryptokey routing. The interface associates allowed IP prefixes with authenticated peers.",
        "The protocol defines handshake initiation/response, cookie replies, and transport data messages. Noise-based key exchange derives session keys; UDP carries protocol packets.",
        "A peer is selected by destination prefix, a handshake establishes fresh keys, and encrypted IP packets flow until rekeying or endpoint roaming changes the path.",
        "Protect private keys, restrict peer AllowedIPs, and remove obsolete peers promptly. The protocol does not provide a user directory, certificate authority, or centralized authorization system by itself.",
        "A compact protocol and kernel implementations can be efficient, but throughput depends on CPU, MTU, routing, peer count, and platform implementation.",
        "Design key provisioning, revocation, endpoint discovery, address allocation, routing, and monitoring. Treat peer configuration as an access-control decision.",
        "Check peer handshake age, endpoint reachability, AllowedIPs, route selection, key pairing, and MTU. A recent handshake alone does not prove application routing is correct.",
        "Official protocol and implementation documentation: https://www.wireguard.com/ ; project source: https://git.zx2c4.com/wireguard-linux/"
    ),
    "OpenVPN" to chapter(
        "OpenVPN is a VPN implementation that uses TLS-based control-channel authentication and a protected data channel. It can operate with routed TUN or bridged TAP interfaces.",
        "Control-channel messages negotiate TLS sessions and credentials; data-channel packets encapsulate network traffic with negotiated cryptographic protection. TCP or UDP can carry the outer connection.",
        "Peers authenticate, negotiate keys and parameters, establish the virtual interface, then exchange routed or bridged packets through the tunnel.",
        "Use trusted certificates or supported authentication, protect profile files and private keys, and keep cryptographic settings current. Do not expose management interfaces unnecessarily.",
        "UDP is commonly preferred for tunnel transport; TCP transport can produce TCP-over-TCP performance problems under loss. Encryption, compression policy, MTU, and CPU affect throughput.",
        "Plan certificate issuance/revocation, client profile delivery, route advertisement, DNS behavior, MFA integration, and server capacity. Keep client and server versions compatible.",
        "Separate TLS control-channel errors from data-channel routing failures. Check certificate validity, cipher negotiation, pushed routes, DNS, TUN/TAP permissions, and MTU.",
        "Official documentation: https://openvpn.net/community-resources/ ; project source: https://github.com/OpenVPN/openvpn"
    ),
    "IKEv2" to chapter(
        "IKEv2 is the key-management and peer-authentication protocol used to establish IPsec Security Associations. It negotiates policy; ESP carries most protected user traffic.",
        "IKE messages contain a fixed header and typed payloads such as SA, KE, ID, CERT, AUTH, NONCE, Notify, and traffic selectors. UDP 500 and NAT traversal UDP 4500 are common transports.",
        "IKE_SA_INIT negotiates cryptographic parameters and key exchange; IKE_AUTH authenticates peers and creates Child SAs. CREATE_CHILD_SA and INFORMATIONAL exchanges manage rekey and status.",
        "Peer identity validation and strong authentication are essential. Avoid weak legacy algorithms, protect credentials, and monitor rekey behavior and certificate validity.",
        "Control-plane exchanges are relatively infrequent; data-plane performance depends on ESP algorithms, packet size, hardware acceleration, and rekey scheduling.",
        "Document identity formats, certificates/PSKs, proposals, traffic selectors, lifetimes, MOBIKE, NAT-T, and peer policy. Keep IKE and IPsec diagnostics distinct.",
        "Check proposal mismatch, ID mismatch, clock/certificate errors, NAT detection, traffic selectors, and Child SA state. A working IKE SA may still have no matching data-plane route.",
        "Primary reference: RFC 7296 — https://www.rfc-editor.org/rfc/rfc7296 ; algorithm guidance: RFC 8247 — https://www.rfc-editor.org/rfc/rfc8247"
    ),
    "GRE" to chapter(
        "GRE is a generic encapsulation protocol that carries a payload protocol inside an outer network-layer packet. It creates a logical tunnel but does not establish encryption or peer identity.",
        "The GRE header contains flags, version, protocol type, and optional checksum, key, or sequence fields. IP protocol number 47 identifies GRE in IPv4.",
        "A router encapsulates a supported payload, forwards it across the underlay, and the remote endpoint decapsulates it before normal forwarding.",
        "GRE has no native confidentiality or robust authentication. Use a separate security layer when traffic crosses an untrusted network and restrict permitted tunnel endpoints.",
        "Encapsulation reduces effective MTU and can cause fragmentation. Tunnel throughput depends on underlay path, packet size, and forwarding hardware.",
        "Plan tunnel source/destination, routing, MTU, keepalive strategy, and security wrapper. Avoid recursive routes and ensure the underlay can reach both endpoints.",
        "Check outer reachability, protocol 47 filtering, tunnel interface state, route recursion, MTU, and encapsulation counters.",
        "GRE: RFC 2784 — https://www.rfc-editor.org/rfc/rfc2784 ; key and sequence extensions: RFC 2890 — https://www.rfc-editor.org/rfc/rfc2890"
    ),
    "L2TP" to chapter(
        "L2TP transports PPP sessions across a packet-switched network. It separates control signaling for tunnel/session management from data messages carrying PPP frames.",
        "Control messages establish tunnels and sessions; data messages carry PPP payload. L2TPv2 commonly uses UDP port 1701 and includes tunnel/session identifiers.",
        "A control connection is established, a session is created, PPP negotiation occurs, and user packets are encapsulated until session or tunnel teardown.",
        "L2TP itself does not encrypt payloads. When confidentiality and peer authentication are required, deployments historically pair it with IPsec and appropriate identity controls.",
        "Double encapsulation can reduce MTU and increase overhead. TCP-like retransmission is not a built-in data-channel guarantee; PPP and outer network behavior matter.",
        "Use only where a PPP-based access design or compatibility requirement justifies it. Plan tunnel IDs, session limits, IPsec pairing, MTU, and lifecycle management.",
        "Check UDP reachability, IPsec negotiation if used, tunnel/session identifiers, PPP authentication, and fragmentation caused by nested headers.",
        "L2TP: RFC 2661 — https://www.rfc-editor.org/rfc/rfc2661 ; IPsec protection: RFC 3193 — https://www.rfc-editor.org/rfc/rfc3193"
    ),
    "VXLAN" to chapter(
        "VXLAN is a Layer-2 overlay over an IP underlay. Virtual Tunnel End Points encapsulate tenant Ethernet frames so logical segments can span routed infrastructure.",
        "VXLAN adds an outer Ethernet/IP/UDP header and an eight-byte VXLAN header containing flags and a 24-bit VNI. UDP destination port 4789 is the standard assigned port.",
        "A source VTEP learns or receives destination endpoint information, encapsulates the inner frame, and sends it through the underlay. The destination VTEP decapsulates and forwards within the tenant segment.",
        "VXLAN does not encrypt tenant traffic. Use underlay isolation, access controls, and an independent encryption layer when confidentiality is required; validate VTEP membership and control-plane trust.",
        "Encapsulation consumes MTU and adds lookup state. Multicast replication or head-end replication, VTEP scale, MAC learning, and hardware offload affect performance.",
        "Design VNI allocation, underlay MTU, VTEP addressing, control plane (often EVPN), anycast gateways, and route/ARP suppression. Avoid uncontrolled flooding.",
        "Check VTEP reachability, VNI mismatch, UDP 4789, underlay MTU, MAC learning, replication, and EVPN route state.",
        "VXLAN: RFC 7348 — https://www.rfc-editor.org/rfc/rfc7348 ; EVPN: RFC 7432 — https://www.rfc-editor.org/rfc/rfc7432"
    ),
    "Geneve" to chapter(
        "Geneve is an extensible network-virtualization encapsulation format for carrying tenant packets across an IP underlay. It is designed to support evolving metadata needs.",
        "The Geneve header includes version, option length, OAM and critical-option bits, protocol type, VNI, and a variable options area followed by the encapsulated payload.",
        "A tunnel endpoint encapsulates an inner frame with a VNI and optional metadata; the remote endpoint validates supported options and decapsulates the payload.",
        "Geneve is encapsulation, not encryption or tenant authorization. Protect the underlay and validate option handling; unknown critical options must be treated according to specification.",
        "Variable-length options offer flexibility but can affect parsing and hardware offload. MTU, option size, and implementation support influence throughput.",
        "Confirm both tunnel endpoints support the same option semantics, VNI mapping, underlay MTU, and control-plane design. Avoid proprietary assumptions in multi-vendor fabrics.",
        "Investigate option-length errors, unsupported critical options, VNI mismatch, UDP path reachability, and MTU drops.",
        "Primary reference: RFC 8926 — https://www.rfc-editor.org/rfc/rfc8926"
    ),
    "MPLS" to chapter(
        "MPLS forwards packets using short labels rather than performing a full IP lookup at every hop. It can carry multiple network-layer payloads and support traffic-engineered paths.",
        "A label stack entry contains a 20-bit label, 3-bit traffic-class field, bottom-of-stack bit, and 8-bit TTL. Label operations include push, swap, and pop.",
        "An ingress Label Edge Router classifies traffic and pushes labels; transit Label Switch Routers swap labels; an egress router removes labels and forwards the payload.",
        "MPLS labels do not encrypt or authenticate customer traffic. Isolation depends on control-plane correctness, label-space design, filtering, and any separate cryptographic protection.",
        "Label switching can improve forwarding scale and support engineered paths, but label distribution, table capacity, stack depth, and traffic engineering add complexity.",
        "Plan the underlay IGP, label distribution, LSPs, MTU, PHP behavior, QoS mapping, and failure convergence. Keep data-plane labels aligned with control-plane state.",
        "Trace the label stack hop by hop, check LDP/RSVP/SR state as applicable, verify MTU and next-hop resolution, and compare FIB with control-plane routes.",
        "Architecture: RFC 3031 — https://www.rfc-editor.org/rfc/rfc3031 ; terminology: RFC 3032 — https://www.rfc-editor.org/rfc/rfc3032"
    ),
    "LDP" to chapter(
        "LDP distributes label bindings between MPLS-capable routers. It commonly follows the routing topology to establish label-switched paths for forwarding equivalence classes.",
        "LDP uses discovery messages, session messages, and label advertisement/release messages. FEC elements associate IP prefixes or other forwarding classes with labels.",
        "Routers discover LDP peers, establish sessions, exchange label mappings, and install forwarding entries consistent with routing and label policy.",
        "LDP peer discovery and session trust must be controlled. Restrict neighbor formation, protect management access, and monitor unexpected label advertisements.",
        "Session scale and label churn affect control-plane CPU and forwarding-table updates. Convergence depends on IGP synchronization and implementation behavior.",
        "Ensure router IDs, transport addresses, targeted/ link discovery policy, IGP reachability, and label retention mode are understood across the domain.",
        "Check LDP neighbor state, transport-address reachability, label bindings, IGP next hops, and FIB installation when an LSP is missing.",
        "Primary reference: RFC 5036 — https://www.rfc-editor.org/rfc/rfc5036"
    ),
    "BGP" to chapter(
        "BGP is the inter-domain path-vector routing protocol. It exchanges reachability between autonomous systems and applies policy to select routes rather than relying only on shortest-path cost.",
        "BGP messages are OPEN, UPDATE, KEEPALIVE, and NOTIFICATION. UPDATE carries withdrawn routes, path attributes, and NLRI; TCP port 179 provides the transport session.",
        "Peers establish a TCP connection, negotiate capabilities in OPEN, exchange route updates, maintain liveness, and apply import/export policy to the routing information base.",
        "BGP sessions should use peer authentication where supported, TTL security or equivalent controls, prefix limits, route filtering, RPKI origin validation, and protected management planes.",
        "Policy and path selection dominate route behavior. Large tables, update churn, route reflectors, convergence timers, and CPU/memory capacity influence scale.",
        "Define ASNs, peer roles, address families, import/export policy, max-prefix limits, communities, next-hop handling, and route-origin validation before enabling exchange.",
        "Inspect session state, received/advertised routes, path attributes, policy counters, next hop, TCP reachability, and RPKI validation state.",
        "Base specification: RFC 4271 — https://www.rfc-editor.org/rfc/rfc4271 ; current BGP security guidance: RFC 7454 — https://www.rfc-editor.org/rfc/rfc7454"
    ),
    "OSPFv2" to chapter(
        "OSPFv2 is a link-state interior gateway protocol for IPv4. Routers build a synchronized link-state database per area and run the shortest-path-first algorithm to calculate routes.",
        "OSPF packets have a common header with version, type, packet length, router ID, area ID, checksum, authentication fields, and type-specific body. IP protocol number 89 carries OSPF.",
        "Routers discover neighbors with Hello packets, form adjacencies, synchronize databases, flood LSAs, and calculate routes when topology changes.",
        "Use supported authentication, passive interfaces, area boundaries, and strict neighbor controls. Incorrect or hostile LSAs can affect routing; protect routing infrastructure and management access.",
        "Incremental SPF and area hierarchy help scale, but LSA flooding, adjacency count, timers, and database size influence CPU and convergence.",
        "Plan router IDs, area design, network types, costs, authentication, summarization, and stub/NSSA behavior. Avoid unstable timers without measured need.",
        "Check neighbor state progression, area/network-type mismatch, MTU, authentication, duplicate router IDs, LSA database, and route installation.",
        "Primary reference: RFC 2328 / STD 54 — https://www.rfc-editor.org/rfc/rfc2328"
    ),
    "OSPFv3" to chapter(
        "OSPFv3 extends link-state routing to IPv6 and uses a revised addressing and flooding model. Modern extensions can also carry non-IPv6 address families.",
        "OSPFv3 uses protocol number 89 over IPv6. Link-local addressing, instance IDs, and separate LSA types distinguish interfaces, routers, networks, prefixes, and external information.",
        "Hello packets establish neighbor relationships; database description, link-state request, and update packets synchronize LSDBs; SPF computes routes from the database.",
        "Use interface-scoped policy and supported authentication mechanisms such as IPsec or OSPFv3 authentication extensions. Protect link-local control traffic and router management.",
        "The link-state algorithm resembles OSPFv2, but LSA scope and address-family extensions affect database size and operational interpretation.",
        "Plan instance IDs, areas, interface costs, address-family support, authentication, and link-local reachability. Keep IPv6 NDP and ICMPv6 functioning.",
        "Check neighbor state, interface scope, instance ID, area ID, LSA scope, MTU, and route-family configuration.",
        "Primary reference: RFC 5340 — https://www.rfc-editor.org/rfc/rfc5340 ; authentication extensions: RFC 7166 — https://www.rfc-editor.org/rfc/rfc7166"
    ),
    "IS-IS" to chapter(
        "IS-IS is a link-state interior routing protocol originally defined for OSI and extended to carry IP reachability. Level 1 provides intra-area routing and Level 2 inter-area backbone routing.",
        "IS-IS runs directly over the data-link layer using CLNS frames, not TCP or UDP. TLVs extend Hello, Link State PDU, and Sequence Number PDU information.",
        "Routers discover neighbors with IIH packets, synchronize link-state databases using LSPs, and compute shortest paths within Level 1/Level 2 topology.",
        "Use authentication for control PDUs where supported, protect routing adjacencies, and constrain L2 domain boundaries. Validate overload and attached-bit behavior.",
        "TLV extensibility supports multiple address families and traffic engineering. Large LSDBs, LSP flooding, and SPF scheduling affect scale.",
        "Plan NET/system IDs, area addresses, levels, metrics, wide metrics, authentication, and multi-topology behavior. Confirm hardware support for required TLVs.",
        "Check adjacency state, circuit type, area compatibility, MTU, authentication, LSP sequence/checksum, and whether the route is leaked between levels.",
        "ISO/IEC 10589; IP integration: RFC 1195 — https://www.rfc-editor.org/rfc/rfc1195"
    ),
    "RIP" to chapter(
        "RIP is a distance-vector interior routing protocol that selects paths using hop count. RIPv2 supports classless prefixes and authentication extensions but has a limited metric range.",
        "RIPv2 uses UDP port 520. Response messages contain route entries with address family, route tag, IP address, subnet mask, next hop, and metric.",
        "Routers exchange periodic and triggered route updates, increment metrics, and apply split horizon, route poisoning, and hold-down behavior to limit loops.",
        "Use authentication and passive interfaces where supported. RIPv2 is not designed for hostile routing environments or large-scale policy control.",
        "A 15-hop maximum makes RIP unsuitable for large topologies. Periodic updates and slow convergence can create stale routes after failures.",
        "Use only for small, simple networks with explicit justification. Document timers, version, authentication, redistribution, and loop-prevention behavior.",
        "Check version mismatch, UDP 520 filtering, subnet masks, metric values, route timers, and redistribution loops.",
        "RIPv2: RFC 2453 — https://www.rfc-editor.org/rfc/rfc2453 ; RIP authentication: RFC 4822 — https://www.rfc-editor.org/rfc/rfc4822"
    ),
    "PIM" to chapter(
        "Protocol Independent Multicast builds multicast distribution trees using unicast routing information. PIM Sparse Mode uses explicit joins toward a Rendezvous Point before source-specific paths may be used.",
        "PIM control messages include Hello, Join/Prune, Register, Bootstrap, and Assert. PIM uses IP protocol number 103 rather than TCP or UDP.",
        "Routers discover neighbors, learn RP information, process receiver joins, build shared or source trees, and forward multicast only where downstream interest exists.",
        "Control-plane trust and RP configuration are important. Apply multicast boundary policy, source filtering, and authenticated management of routing infrastructure.",
        "Multicast efficiency depends on tree shape, receiver distribution, RP placement, and state scale. Incorrect RPF decisions can stop delivery even when unicast works.",
        "Plan multicast address allocation, RP redundancy, SSM versus ASM, RPF, TTL scope, and boundaries. Coordinate with IGMP/MLD on receiver-facing segments.",
        "Check multicast routing table, RPF interface, RP mapping, Join/Prune state, group membership, and upstream source reachability.",
        "PIM-SM: RFC 7761 — https://www.rfc-editor.org/rfc/rfc7761 ; PIM security: RFC 4601 security considerations within the specification"
    ),
    "IGMP" to chapter(
        "IGMP manages IPv4 multicast group membership between hosts and neighboring multicast routers. It signals interest; it does not itself route multicast across the network.",
        "IGMPv3 reports include group records and source filters. IGMP messages are carried directly in IPv4 and identified by Protocol number 2.",
        "Hosts report group membership, routers send membership queries, and switches may snoop reports to constrain Layer-2 multicast flooding.",
        "Spoofed membership reports can affect multicast delivery. Use VLAN boundaries, querier controls, and multicast access policy where needed.",
        "Snooping reduces unnecessary traffic but depends on a functioning querier and correct switch state. Group/source state scales with receiver count.",
        "Choose IGMP version consistently, configure a querier per VLAN where required, and coordinate IGMP snooping with multicast routing.",
        "Check querier presence, membership timers, report visibility, snooping table, VLAN, and source-specific filter state.",
        "IGMPv3: RFC 3376 — https://www.rfc-editor.org/rfc/rfc3376 ; source-specific multicast: RFC 4604 — https://www.rfc-editor.org/rfc/rfc4604"
    ),
    "MLD" to chapter(
        "Multicast Listener Discovery performs IPv6 multicast group membership signaling. It is the IPv6 counterpart to IGMP and is carried in ICMPv6.",
        "MLDv2 reports carry multicast address records and source filters. Messages use ICMPv6 types and IPv6 Hop Limit 1 on the local link.",
        "Hosts send listener reports, routers query group state, and Layer-2 switches may use MLD snooping to limit multicast forwarding.",
        "As with other local-link control protocols, untrusted membership signaling can affect traffic distribution. Apply segment controls and monitor unexpected group state.",
        "State size depends on group and source memberships. MLD snooping and querier behavior must remain synchronized to avoid unwanted flooding or lost streams.",
        "Coordinate MLD version, querier, IPv6 multicast routing, switch snooping, and firewall treatment of essential ICMPv6 messages.",
        "Check ICMPv6 filtering, querier presence, group records, source filters, VLAN membership, and switch snooping state.",
        "MLDv2: RFC 3810 — https://www.rfc-editor.org/rfc/rfc3810"
    ),
    "VRRP" to chapter(
        "VRRP provides first-hop gateway redundancy by allowing multiple routers to present a shared virtual router identity. One router forwards as Master while backups monitor advertisements.",
        "VRRP advertisements carry version, type, virtual-router ID, priority, address count, authentication/reserved fields, and virtual addresses. IPv4 and IPv6 use distinct protocol specifications.",
        "Routers elect a Master based on priority and preemption policy. Backups take over when advertisements stop; virtual MAC/address behavior preserves the host gateway setting.",
        "Protect the shared segment against spoofed advertisements and duplicate VRIDs. Use authentication features only as supported by the current specification and vendor implementation.",
        "Failover time depends on advertisement interval and detection policy. Gratuitous ARP or unsolicited Neighbor Advertisements update local neighbor state after role change.",
        "Plan VRID, priority, virtual IP, tracking, preemption, advertisement interval, and routing symmetry. Ensure only intended peers share a virtual router.",
        "Check Master/Backup state, VRID, priority, advertisement visibility, interface tracking, duplicate virtual IPs, and host neighbor cache.",
        "Current VRRP specification: RFC 9568 — https://www.rfc-editor.org/rfc/rfc9568"
    ),
    "BFD" to chapter(
        "Bidirectional Forwarding Detection provides rapid detection of forwarding-path failures between adjacent systems. Routing protocols can consume BFD state without implementing their own fast timers.",
        "BFD control packets negotiate desired transmit/receive intervals, detection multiplier, state, diagnostic code, and discriminators. Asynchronous and demand modes are defined.",
        "Peers establish a session, exchange control packets, calculate a detection time from negotiated intervals, and notify clients when the session changes state.",
        "BFD sessions should be bound to intended peers and protected by authentication where supported. Avoid accepting unexpected sessions from untrusted interfaces.",
        "Aggressive intervals increase CPU and control traffic and can cause false failure detection under congestion. Hardware offload and scale vary by platform.",
        "Tune intervals based on platform capacity and path behavior. Coordinate BFD with routing protocol failure policy and ensure timers are consistent with network design.",
        "Check session state, negotiated timers, discriminator, packet loss, CPU load, and whether control packets use the expected path.",
        "Base specification: RFC 5880 — https://www.rfc-editor.org/rfc/rfc5880 ; single-hop: RFC 5881 — https://www.rfc-editor.org/rfc/rfc5881"
    ),
    "STP / RSTP" to chapter(
        "Spanning Tree Protocol prevents Layer-2 loops by selecting a loop-free logical topology. RSTP accelerates convergence using explicit port roles and synchronization behavior.",
        "Bridge Protocol Data Units carry root bridge ID, root path cost, sender bridge/port IDs, timers, and flags. RSTP uses additional role/state signaling.",
        "Switches elect a root bridge, calculate root/designated/alternate ports, and place redundant paths into non-forwarding states until topology changes.",
        "Rogue or misconfigured switches can influence root election. Use BPDU Guard, Root Guard, Loop Guard, and edge-port protections according to the topology.",
        "A loop can cause broadcast storms and MAC-table instability. RSTP generally converges faster than classic STP, but topology and vendor interoperability matter.",
        "Set intentional root bridges, document VLAN spanning-tree mode, edge ports, link aggregation, and protection features. Avoid unmanaged switching loops.",
        "Check root ID, port role/state, BPDU counters, topology-change count, blocked paths, and inconsistent guard states.",
        "IEEE 802.1Q bridging standard; overview: https://standards.ieee.org/standard/802_1Q-2022.html"
    ),
    "VLAN / 802.1Q" to chapter(
        "IEEE 802.1Q defines bridged LAN behavior including VLAN-aware forwarding. VLANs create logical Layer-2 broadcast domains over shared switching infrastructure.",
        "An 802.1Q tag inserts TPID and TCI fields containing PCP, DEI, and a 12-bit VLAN identifier between source MAC and EtherType fields.",
        "Ingress port policy classifies frames; bridges learn source MAC addresses within VLAN context and forward according to VLAN membership and spanning-tree state.",
        "VLANs are segmentation, not encryption. Trunk misconfiguration, native VLAN mismatch, or overly broad membership can expose traffic across intended boundaries.",
        "VLANs reduce broadcast scope but add operational complexity and finite identifier space. Hardware table limits and ACL/QoS pipeline design affect scale.",
        "Document access/trunk modes, allowed VLANs, native VLAN policy, MTU, voice VLANs, and inter-VLAN routing. Keep unused VLANs and ports controlled.",
        "Check tag presence, native VLAN, allowed list, access-port assignment, STP state, and routed gateway interface.",
        "IEEE 802.1Q standard: https://standards.ieee.org/standard/802_1Q-2022.html ; IANA Ethernet parameters: https://www.iana.org/assignments/ieee-802-numbers/"
    ),
    "Ethernet" to chapter(
        "Ethernet defines common wired LAN physical and data-link technologies. IEEE 802.3 covers MAC framing and physical media variants; switching behavior is complemented by bridging standards.",
        "A basic Ethernet frame includes destination/source MAC addresses, EtherType or length, payload, and frame check sequence. Preamble and inter-packet gap are physical-layer framing elements.",
        "NICs transmit frames onto a link; switches learn source MAC addresses and forward frames based on destination and VLAN context. Full-duplex links avoid classic shared-medium collisions.",
        "MAC addresses can be spoofed and are not strong identity credentials. Use port security, 802.1X, segmentation, and higher-layer authentication as appropriate.",
        "Speed, duplex, cable category, optical module, frame size, flow control, and switch buffering affect throughput and loss.",
        "Plan cable/media type, speed negotiation, duplex, MTU/jumbo-frame consistency, PoE requirements, and link aggregation. Verify transceiver compatibility.",
        "Inspect link state, negotiated speed/duplex, CRC/FCS errors, drops, cable diagnostics, and interface counters at both ends.",
        "IEEE 802.3 standard: https://standards.ieee.org/standard/802_3-2022.html"
    ),
    "Wi-Fi (IEEE 802.11)" to chapter(
        "IEEE 802.11 defines wireless LAN MAC and PHY operation. Stations associate with access points and share radio airtime using contention and scheduling mechanisms.",
        "802.11 frames include frame control, duration/ID, address fields, sequence control, optional QoS/control fields, payload, and frame check sequence; PHY framing varies by amendment.",
        "A station scans, authenticates, associates, negotiates security, and exchanges frames over a shared radio medium. Roaming depends on client behavior and network support.",
        "Use WPA3 or appropriately configured WPA2-Enterprise/Personal, strong credentials, protected management frames where supported, and secure guest isolation.",
        "Airtime, interference, channel width, signal quality, client count, retransmissions, and shared-medium contention matter more than advertised PHY rate.",
        "Plan channels, power, band steering, roaming, SSID/VLAN mapping, authentication, guest isolation, and RF coverage. Avoid treating more transmit power as a universal fix.",
        "Check RSSI/SNR, channel utilization, retries, association/authentication logs, DHCP after association, and roaming transitions.",
        "IEEE 802.11 standards family: https://standards.ieee.org/standard/802_11-2020.html"
    ),
    "LLDP" to chapter(
        "LLDP is a one-hop link-layer discovery protocol that advertises device and port identity to directly connected neighbors. It supports inventory and topology awareness.",
        "LLDP frames use a reserved destination MAC and EtherType 0x88CC. Information is encoded as Type-Length-Value elements, including chassis ID, port ID, TTL, and optional capabilities.",
        "A device periodically transmits advertisements and stores received neighbor information until TTL expiry. Optional organizational TLVs can carry vendor or application-specific data.",
        "LLDP advertisements can expose device names, ports, capabilities, and topology. Restrict it on untrusted edge ports and avoid leaking management details unnecessarily.",
        "The protocol is low overhead, but frequent refreshes across large port counts create modest control-plane load. Neighbor data is only as current as the advertisement/TTL cycle.",
        "Use LLDP for inventory, voice VLAN hints, and troubleshooting; define which ports may advertise and consume organizational TLVs.",
        "Check transmit/receive enablement, TTL expiry, VLAN/port policy, and whether a device is using CDP or another proprietary discovery mechanism instead.",
        "IEEE 802.1AB standard: https://standards.ieee.org/standard/802_1AB-2016.html"
    ),
    "LACP" to chapter(
        "Link Aggregation Control Protocol negotiates aggregation of multiple physical links into one logical link. It coordinates member selection and synchronization between partner systems.",
        "LACPDU frames carry actor/partner system IDs, keys, port priorities/numbers, state flags, and reserved fields. LACP uses a link-local slow-protocol destination MAC.",
        "Ports exchange LACPDUs, compare actor/partner keys and system identity, and select compatible members into an aggregator. Periodic or fast timers maintain state.",
        "A rogue or misconfigured peer can affect aggregation. Restrict physical access, align system identity and keys, and avoid relying on aggregation as a security boundary.",
        "Aggregation increases total capacity across multiple flows and adds link resilience; a single flow is often limited to one member's capacity by hashing.",
        "Align speed, duplex, VLAN/trunk policy, MTU, and LACP mode on both ends. Understand hashing and minimum-link behavior.",
        "Check actor/partner state, selected versus standby members, key mismatch, physical errors, hashing, and switch MLAG/vPC peer state.",
        "IEEE 802.1AX link aggregation standard: https://standards.ieee.org/standard/802_1AX-2020.html"
    ),
    "PPP" to chapter(
        "PPP provides a point-to-point data-link protocol with framing, link configuration, and optional authentication. Network-layer protocols are negotiated after the link is established.",
        "PPP frames contain Flag, Address, Control, Protocol, Information, optional padding, and Frame Check Sequence. LCP negotiates link parameters; NCP configures network-layer protocols.",
        "Peers establish framing, negotiate LCP options, authenticate if configured, negotiate an NCP such as IPCP or IPv6CP, exchange packets, and terminate the link.",
        "Use strong supported authentication such as EAP or CHAP variants; PAP exposes credentials without a protected channel. Protect the physical or access network.",
        "PPP overhead is small, but serial framing, link quality, negotiation, and encapsulation determine throughput and effective MTU.",
        "Document authentication, MRU/MTU, keepalive, compression policy, multilink behavior, and NCP requirements. PPP is commonly embedded in access technologies.",
        "Check LCP state, authentication logs, NCP negotiation, framing errors, and MTU/MRU mismatch.",
        "PPP: RFC 1661 — https://www.rfc-editor.org/rfc/rfc1661 ; authentication protocols are specified separately."
    ),
    "PPPoE" to chapter(
        "PPPoE carries PPP sessions over Ethernet, commonly for broadband subscriber access. It combines Ethernet discovery with a PPP session phase.",
        "Discovery uses EtherTypes 0x8863 and 0x8864 for session traffic. PADI/PADO/PADR/PADS establish a session ID before PPP frames are carried.",
        "A host discovers an access concentrator, negotiates a session, runs PPP LCP/authentication/NCP, then exchanges IP traffic within the session.",
        "PPPoE does not itself encrypt traffic. Protect subscriber authentication and management systems; isolate access circuits and use secure higher-layer protocols.",
        "The 8-byte PPPoE/PPP overhead reduces conventional Ethernet payload MTU unless the network supports larger frames. Session scale affects access concentrators.",
        "Plan discovery domains, service names, session limits, authentication backend, MTU/MRU, and IPv6 support. Ensure all intermediate links support the required frame size.",
        "Check PADI visibility, PADO response, session ID, concentrator capacity, PPP authentication, and MTU-related fragmentation.",
        "PPPoE: RFC 2516 — https://www.rfc-editor.org/rfc/rfc2516"
    ),
    "HTTP" to chapter(
        "HTTP is a stateless application request/response protocol with extensible semantics. HTTP/1.1 uses textual message framing; HTTP/2 and HTTP/3 use binary framing over different transports.",
        "HTTP semantics define methods, status codes, fields, and representations. HTTP/1.1 message syntax is specified separately from shared semantics; framing differs substantially in HTTP/2 and HTTP/3.",
        "A client establishes the relevant transport/security context, sends a request, and receives a response. Persistent connections, multiplexing, caching, and intermediaries affect the exchange.",
        "Use HTTPS with modern TLS, validate certificates, protect cookies and credentials, and apply application authorization. HTTP itself does not encrypt content.",
        "Caching and multiplexing can improve efficiency. Protocol version, connection reuse, compression, RTT, server processing, and intermediary behavior affect latency.",
        "Choose HTTP version based on platform support, configure caching and timeouts, use correct proxy semantics, and monitor status codes and latency by route.",
        "Check DNS, TLS, ALPN, request framing, proxy behavior, server logs, redirects, and cache headers. Separate transport success from application status.",
        "Semantics: RFC 9110 — https://www.rfc-editor.org/rfc/rfc9110 ; HTTP/1.1: RFC 9112 — https://www.rfc-editor.org/rfc/rfc9112 ; HTTP/2: RFC 9113 — https://www.rfc-editor.org/rfc/rfc9113 ; HTTP/3: RFC 9114 — https://www.rfc-editor.org/rfc/rfc9114"
    ),
    "WebSocket" to chapter(
        "WebSocket provides bidirectional message-oriented communication over a connection established through an HTTP opening handshake. It is distinct from ordinary repeated HTTP requests.",
        "The opening handshake negotiates an Upgrade and Sec-WebSocket fields. Data frames contain FIN/RSV/opcode bits, masking key where required, payload length, and payload.",
        "A client requests an upgrade, the server validates and accepts it, then both sides exchange text/binary messages, control frames, pings/pongs, and a closing handshake.",
        "Use wss:// with TLS, validate Origin and authentication, limit message sizes, and authorize each action. A persistent connection does not imply trusted identity.",
        "Persistent connections reduce repeated setup cost but consume server resources. Heartbeat frequency, fan-out, message size, and backpressure determine scalability.",
        "Design reconnect/backoff, heartbeat, message schema versioning, per-connection authorization, rate limits, and graceful shutdown.",
        "Check upgrade response, proxy idle timeouts, TLS, ping/pong health, close codes, Origin policy, and server-side connection limits.",
        "Primary reference: RFC 6455 — https://www.rfc-editor.org/rfc/rfc6455 ; IANA registries: https://www.iana.org/assignments/websocket/websocket.xhtml"
    ),
    "FTP" to chapter(
        "FTP is a legacy file-transfer protocol that separates a control connection from data connections. Active and passive modes differ in which endpoint initiates the data channel.",
        "Commands and replies are exchanged on the control channel; data connections carry directory listings or file contents. Control uses TCP port 21 by default.",
        "A client authenticates, selects transfer mode and representation, opens a separate data connection, transfers content, and closes the data channel.",
        "Classic FTP sends credentials and data without encryption. Do not use it across untrusted networks; prefer SFTP or FTPS with correctly validated certificates.",
        "Separate data channels complicate NAT/firewall traversal. Transfer mode, TCP windowing, server disk, and network path affect throughput.",
        "If legacy FTP is unavoidable, isolate it, restrict accounts and directories, and explicitly configure passive ranges and firewall policy.",
        "Check control-channel login separately from data-channel setup. Passive port range, NAT helper behavior, permissions, and active-mode routing are common failure areas.",
        "FTP: RFC 959 — https://www.rfc-editor.org/rfc/rfc959 ; FTP security extensions: RFC 4217 — https://www.rfc-editor.org/rfc/rfc4217"
    ),
    "TFTP" to chapter(
        "TFTP is a minimal file-transfer protocol often used for controlled bootstrapping and device provisioning. It deliberately omits user authentication and directory semantics.",
        "RRQ/WRQ initiate read/write; DATA, ACK, and ERROR packets use UDP. The initial request uses port 69, then a transfer-specific UDP port is selected.",
        "A client requests a file, server responds from a transfer ID port, and DATA/ACK blocks advance until the final short block or error.",
        "TFTP has no built-in confidentiality, authentication, or authorization beyond server-side policy. Restrict it to isolated provisioning networks and read-only content where possible.",
        "Small block sizes and stop-and-wait behavior can limit throughput; negotiated block-size and window options improve performance when supported.",
        "Use dedicated provisioning VLANs, narrow server roots, allowlisted clients, and integrity verification outside TFTP. Avoid exposing writable boot directories.",
        "Check UDP 69 reachability, dynamic transfer ports, file path/case, permissions, block-size negotiation, and firewall state tracking.",
        "TFTP: RFC 1350 — https://www.rfc-editor.org/rfc/rfc1350 ; options: RFC 2347 — https://www.rfc-editor.org/rfc/rfc2347"
    ),
    "SFTP" to chapter(
        "SFTP is a file-transfer subsystem commonly carried over SSH. It is not FTP over TLS and does not use FTP's separate data-channel model.",
        "SFTP uses SSH channel data with its own packet framing, request IDs, operation codes, attributes, and file handles. Version extensions vary by implementation.",
        "An SSH connection authenticates the server and client; a subsystem channel starts SFTP requests such as open, read, write, stat, rename, and close.",
        "Security inherits SSH transport protection, but host-key verification and account authorization remain essential. Restrict filesystem access and avoid trusting unknown host keys.",
        "Pipelined requests and channel windows influence throughput. Many small operations are latency-sensitive; large sequential transfers depend on disk and cipher performance.",
        "Document supported SFTP versions/extensions, chroot or filesystem boundaries, key lifecycle, permissions, and audit logging. Confirm client/server interoperability.",
        "Check SSH authentication first, then subsystem availability, server-side file permissions, channel limits, and SFTP extension compatibility.",
        "SSH architecture: RFC 4251 — https://www.rfc-editor.org/rfc/rfc4251 ; transport: RFC 4253 — https://www.rfc-editor.org/rfc/rfc4253 ; SFTP is an SSH subsystem with implementation-specific extensions."
    ),
    "NFS" to chapter(
        "NFS provides remote filesystem access using client/server operations. NFSv4 integrates stateful file access, locking, and security features more tightly than earlier versions.",
        "NFSv4 uses XDR-encoded COMPOUND operations over RPC. Filehandles identify objects; attributes, stateids, sessions, and callback mechanisms support filesystem semantics.",
        "A client mounts an export, establishes session state, issues compound operations, and renews state as needed. Delegations and leases coordinate cached access.",
        "Use strong authentication such as Kerberos where required, export restrictions, secure transport options, and least-privilege filesystem permissions.",
        "Metadata round trips and network latency can dominate workloads with many small operations. Caching, delegation, session slots, and transport choice affect throughput.",
        "Plan export policy, identity mapping, UID/GID consistency, NFS version, locking, failover, and backup semantics. Understand soft versus hard mount implications.",
        "Check mount negotiation, server export permissions, name/identity mapping, RPC connectivity, state recovery, and client/server version compatibility.",
        "NFSv4.1: RFC 8881 — https://www.rfc-editor.org/rfc/rfc8881 ; NFSv4.2: RFC 7862 — https://www.rfc-editor.org/rfc/rfc7862"
    ),
    "SMB" to chapter(
        "SMB is a network file and printer-sharing protocol family used widely in enterprise environments. Modern SMB supports signing, encryption, durable handles, and multi-channel features.",
        "SMB2/3 messages have a binary header with command, message ID, session/tree identifiers, flags, and command-specific structures. Negotiate, Session Setup, Tree Connect, and file operations are distinct commands.",
        "Client and server negotiate dialect/capabilities, authenticate a session, connect to a share, then perform create/read/write/close operations with stateful handles.",
        "Use SMB signing and encryption according to risk, modern authentication, patching, share/NTFS permissions, and network segmentation. Disable obsolete SMB1 where possible.",
        "Large I/O benefits from credits, multichannel, and durable handles; latency and signing/encryption CPU may affect workloads. Storage and metadata performance remain critical.",
        "Document dialect policy, signing/encryption requirements, identity provider integration, share permissions, backups, and client compatibility.",
        "Check dialect negotiation, authentication, DNS/SPN, share versus filesystem permissions, signing policy, SMB service availability, and event logs.",
        "Microsoft protocol specifications: https://learn.microsoft.com/en-us/openspecs/windows_protocols/ms-smb2/ ; SMB overview: https://learn.microsoft.com/en-us/windows-server/storage/file-server/smb-overview"
    ),
    "SMTP" to chapter(
        "SMTP transfers and relays email between mail systems. Submission by end-user clients is commonly separated from server-to-server relay and protected with TLS policy.",
        "Commands such as EHLO, MAIL FROM, RCPT TO, DATA, and QUIT are exchanged with numeric replies. Extensions are advertised through EHLO and registered by IANA.",
        "A client establishes TCP, identifies itself, negotiates supported extensions, submits envelope and message content, and receives acceptance or rejection status.",
        "Use TLS, authentication for submission, SPF/DKIM/DMARC policy, relay restrictions, and abuse controls. SMTP acceptance does not guarantee final inbox delivery.",
        "Queueing, DNS MX lookup, greylisting, message size, attachment scanning, and remote server response affect delivery time.",
        "Separate port 25 relay from authenticated submission, manage queue/retry policy, maintain reverse DNS, and publish correct domain authentication records.",
        "Read SMTP reply codes, queue IDs, DNS MX results, TLS negotiation, policy rejection, and downstream delivery logs.",
        "SMTP: RFC 5321 — https://www.rfc-editor.org/rfc/rfc5321 ; message format: RFC 5322 — https://www.rfc-editor.org/rfc/rfc5322"
    ),
    "IMAP" to chapter(
        "IMAP provides server-resident mailbox access and synchronization. Clients can inspect folders and message metadata without downloading every message body.",
        "IMAP commands are tagged and responses include untagged updates and tagged completion. Message sequence numbers, UIDs, flags, and mailbox state support synchronization.",
        "A client connects, authenticates, selects a mailbox, fetches metadata or content, updates flags, and closes or logs out. IDLE can notify clients of changes.",
        "Use TLS and modern authentication, protect refresh tokens, restrict mailbox access, and apply server-side audit and retention policy.",
        "Many small metadata commands are sensitive to RTT. UID-based synchronization and batching reduce redundant transfers; large attachments are bandwidth-bound.",
        "Plan mailbox quotas, folder naming, UID validity, client synchronization, TLS, and authentication compatibility.",
        "Check TLS, authentication mechanism, mailbox selection, UIDVALIDITY changes, quota, server limits, and tagged error responses.",
        "IMAP4rev2: RFC 9051 — https://www.rfc-editor.org/rfc/rfc9051"
    ),
    "POP3" to chapter(
        "POP3 is a simpler mail retrieval protocol that traditionally downloads messages from a server. It offers less synchronization functionality than IMAP.",
        "POP3 commands include USER/PASS or an authorization mechanism, STAT, LIST, RETR, DELE, NOOP, RSET, and QUIT; replies begin with positive or negative status indicators.",
        "A client authenticates, lists messages, retrieves selected messages, marks deletions, and commits them on a successful QUIT.",
        "Use TLS and secure authentication; avoid sending passwords over plaintext connections. Server-side deletion behavior should be understood before enabling clients.",
        "Download-oriented operation can be efficient for simple clients but is less suitable for multi-device mailbox synchronization and server-side folder workflows.",
        "Use only where simple retrieval is needed. Document leave-on-server behavior, retention, TLS, and client synchronization expectations.",
        "Check authentication, TLS, mailbox lock, server limits, message size, and whether deletion is committed at QUIT.",
        "POP3: RFC 1939 — https://www.rfc-editor.org/rfc/rfc1939 ; TLS update: RFC 8314 — https://www.rfc-editor.org/rfc/rfc8314"
    ),
    "SNMP" to chapter(
        "SNMP is a management framework for monitoring and configuring network devices through managed objects. Managers query agents using a Management Information Base.",
        "SNMP messages carry version, community or security parameters, PDU type, request ID, error fields, and variable bindings containing object identifiers and values.",
        "A manager polls an agent or receives traps/informs. SNMPv3 security models authenticate and optionally encrypt management messages.",
        "Prefer SNMPv3 with authentication and privacy. Restrict manager addresses, use read-only access where possible, and avoid exposing community strings or write access broadly.",
        "Polling frequency, table size, device CPU, and walk scope influence load. Traps provide event-driven signals but may be lost without reliable transport.",
        "Maintain MIB versions, OID inventory, credentials, access views, polling intervals, and trap destinations. Separate monitoring networks from user traffic.",
        "Check version/security model, credentials, context, access view, UDP reachability, OID support, and agent CPU before assuming a device is offline.",
        "SNMP architecture: RFC 3411 — https://www.rfc-editor.org/rfc/rfc3411 ; SNMPv3 USM: RFC 3414 — https://www.rfc-editor.org/rfc/rfc3414"
    ),
    "Syslog" to chapter(
        "Syslog transports event messages from devices and applications to collectors for operational monitoring and audit. Modern deployments can use structured data and TLS transport.",
        "A syslog message includes priority, version, timestamp, hostname, app name, process ID, message ID, structured data, and message content in the modern format.",
        "A source emits an event, transport delivers it to a collector, and downstream systems parse, index, correlate, and retain it according to policy.",
        "Logs can contain personal data, tokens, or sensitive topology. Use transport protection, access controls, redaction, integrity controls, and retention limits.",
        "UDP syslog is lightweight but lossy; TCP/TLS adds reliability and security overhead. High event rates require buffering and backpressure planning.",
        "Standardize timestamps, severity, facility, structured fields, time synchronization, collector redundancy, and retention. Avoid logging secrets.",
        "Check source facility/severity filters, transport, TLS trust, collector capacity, timestamp skew, parser rules, and dropped-message counters.",
        "Syslog protocol: RFC 5424 — https://www.rfc-editor.org/rfc/rfc5424 ; TLS transport: RFC 5425 — https://www.rfc-editor.org/rfc/rfc5425"
    ),
    "NTP" to chapter(
        "NTP synchronizes clocks across packet networks using timestamp exchange and clock-discipline algorithms. Accurate time supports logs, certificates, authentication, and distributed systems.",
        "NTP messages include leap indicator, version, mode, stratum, poll, precision, root delay/dispersion, reference ID, and four timestamps.",
        "A client samples server time, estimates offset and delay, filters samples, selects sources, and disciplines its local clock gradually.",
        "Unauthenticated time can be spoofed or manipulated. Use trusted sources, access controls, monitoring, and NTS where authenticated time is required.",
        "Accuracy depends on path asymmetry, jitter, server quality, oscillator stability, polling, and network load. Stratum alone is not a quality score.",
        "Use multiple independent time sources, sensible hierarchy, local stratum policy, and alerting for offset/dispersion. Keep time service reachable without circular dependencies.",
        "Check source selection, offset, root dispersion, reachability, stratum, clock state, and firewall treatment of UDP 123.",
        "NTPv4: RFC 5905 — https://www.rfc-editor.org/rfc/rfc5905"
    ),
    "LDAP" to chapter(
        "LDAP is a directory access protocol for querying and modifying hierarchical directory entries. It is commonly used for identity, address-book, and policy integration.",
        "LDAP messages use ASN.1 BER encoding and contain a message ID plus protocol operation. Search requests include base DN, scope, filter, requested attributes, and limits.",
        "A client connects, negotiates TLS or StartTLS as appropriate, binds, performs searches or updates, and unbinds. Directory referrals and controls extend behavior.",
        "Use TLS, secure bind methods, least-privilege service accounts, access controls, and careful password policy. Anonymous or simple binds over plaintext expose credentials.",
        "Search filters, indexes, referrals, result size, and directory replication affect latency and consistency. Large subtree searches can be expensive.",
        "Design DIT structure, schema, indexes, naming, replication, bind identity, group mapping, and certificate trust. Escape filter input in applications.",
        "Check bind DN, TLS trust, base DN, search scope/filter, referrals, schema, server timeouts, and replication state.",
        "LDAPv3: RFC 4511 — https://www.rfc-editor.org/rfc/rfc4511 ; TLS: RFC 4513 — https://www.rfc-editor.org/rfc/rfc4513"
    ),
    "RADIUS" to chapter(
        "RADIUS centralizes network access authentication, authorization, and accounting. Network access servers act as clients to a RADIUS service.",
        "Classic RADIUS packets contain Code, Identifier, Length, Authenticator, and attributes. Common exchanges include Access-Request, Access-Accept, Access-Reject, and Accounting messages.",
        "A NAS sends credentials and context, the server evaluates policy or consults an identity backend, and the response returns authorization attributes or rejection.",
        "Classic RADIUS uses shared secrets and has legacy transport limitations. Protect the management path, use strong secrets, and adopt modern transport/security options where supported.",
        "UDP retransmission and timeout behavior affect perceived login latency. Backend directory lookup and accounting volume influence server capacity.",
        "Document NAS client addresses, unique secrets, authentication methods, VLAN/role attributes, accounting, failover, and certificate validation for EAP deployments.",
        "Check request source, shared secret, authenticator, attribute format, backend identity lookup, timeout, and NAS/server clock/log correlation.",
        "RADIUS: RFC 2865 — https://www.rfc-editor.org/rfc/rfc2865 ; accounting: RFC 2866 — https://www.rfc-editor.org/rfc/rfc2866"
    ),
    "TACACS+" to chapter(
        "TACACS+ is an AAA protocol commonly used for administrative access to network infrastructure. It separates authentication, authorization, and accounting decisions.",
        "TACACS+ messages include a fixed header with version, type, sequence number, flags, session ID, and length; body content is packet-type-specific.",
        "A network device opens a TCP session, performs authentication exchanges, requests authorization for commands or services, and records accounting events.",
        "Use protected transport or network isolation, strong shared secrets, strict server access, and redundant AAA infrastructure. Do not expose administrative AAA endpoints publicly.",
        "TCP sessions and per-command authorization can add latency to interactive administration. Server redundancy and timeout policy affect availability.",
        "Define command authorization, privilege mapping, fallback policy, accounting destinations, and break-glass access with audit controls.",
        "Check TCP reachability, shared secret, packet sequence/session, server policy, command authorization, and local fallback behavior.",
        "TACACS+ protocol: RFC 8907 — https://www.rfc-editor.org/rfc/rfc8907"
    ),
    "Kerberos" to chapter(
        "Kerberos provides ticket-based network authentication using a trusted Key Distribution Center. Services validate tickets instead of receiving a user's password for every service.",
        "The protocol uses ASN.1 structures and encrypted tickets. Authentication Service and Ticket Granting Service exchanges issue tickets and session keys with lifetimes and flags.",
        "A client obtains a TGT, requests a service ticket, and presents it to the target service; the service validates the ticket and optionally performs mutual authentication.",
        "Protect KDC keys, enforce clock synchronization, use strong encryption types, limit ticket lifetimes, and secure service principal names. Compromise of the KDC is high impact.",
        "Ticket caching reduces repeated password exchanges. KDC availability, cross-realm trust, clock skew, and directory lookup influence authentication latency.",
        "Plan realms, principals, SPNs, keytabs, DNS, time service, delegation policy, and cross-realm trust. Rotate service keys in a controlled manner.",
        "Check clock skew, realm mapping, DNS canonicalization, SPN uniqueness, ticket expiry, encryption type compatibility, and KDC reachability.",
        "Kerberos V5: RFC 4120 — https://www.rfc-editor.org/rfc/rfc4120"
    ),
    "NETCONF" to chapter(
        "NETCONF is a network configuration management protocol with structured operations and datastore semantics. It provides a model-driven interface rather than screen-scraping device CLI output.",
        "RPC messages are XML-encoded and include message IDs, operation elements, and structured replies. YANG models define data schemas used by clients and servers.",
        "A client establishes a secure session, exchanges capabilities, locks or edits a datastore, validates/commits changes, and receives structured results or notifications.",
        "Use SSH transport or another secure supported mapping, authenticate automation identities, restrict writable datastores, and audit configuration changes.",
        "Candidate datastores and confirmed commit can reduce risk, but model size, transaction locking, and device processing affect operation time.",
        "Validate YANG module revisions, capabilities, datastore support, rollback policy, and concurrency. Treat configuration automation as privileged access.",
        "Check capability exchange, namespace/module revision, datastore lock, validation errors, authorization, and transport subsystem availability.",
        "NETCONF: RFC 6241 — https://www.rfc-editor.org/rfc/rfc6241 ; YANG: RFC 7950 — https://www.rfc-editor.org/rfc/rfc7950"
    ),
    "RESTCONF" to chapter(
        "RESTCONF exposes YANG-modeled configuration and state data through HTTP-style resources. It complements NETCONF with a REST-like interface.",
        "Resources are addressed by URI and exchanged using JSON or XML encodings. HTTP methods map to retrieval, creation, replacement, modification, and deletion operations.",
        "A client authenticates over HTTPS, discovers YANG modules/resources, sends an HTTP operation, and interprets structured data or protocol error information.",
        "Use TLS, scoped credentials, least-privilege authorization, input validation, and audit logging. Treat configuration endpoints as high-impact administrative interfaces.",
        "HTTP caching and connection reuse help read-heavy operations; large YANG trees and expensive server-side validation affect latency.",
        "Document module revisions, content types, ETags, concurrency semantics, authentication, and safe change/rollback workflows.",
        "Check HTTP status, content type, URI encoding, YANG module availability, authorization, ETag conflicts, and server error payloads.",
        "RESTCONF: RFC 8040 — https://www.rfc-editor.org/rfc/rfc8040 ; YANG: RFC 7950 — https://www.rfc-editor.org/rfc/rfc7950"
    ),
    "SIP" to chapter(
        "SIP establishes, modifies, and terminates multimedia sessions. It handles signaling and session descriptions; media usually flows separately using RTP or related protocols.",
        "SIP requests and responses contain start lines, headers, and optional bodies. Common methods include INVITE, ACK, BYE, CANCEL, REGISTER, and OPTIONS.",
        "Endpoints register where needed, send an INVITE, negotiate session parameters, complete dialog establishment, exchange media, and terminate with BYE.",
        "Use TLS for signaling, strong authentication, secure media such as SRTP, and topology-aware SBC policy. Protect against toll fraud and unauthorized registration.",
        "Signaling latency affects call setup; media quality depends on RTP path, jitter, loss, codec, and QoS. NAT traversal may require ICE/STUN/TURN.",
        "Plan SIP domains, registrar/proxy roles, codecs, SDP, NAT traversal, media ranges, emergency calling, and failover. Keep signaling and media troubleshooting separate.",
        "Check registration, response codes, dialog identifiers, SDP address/port, codec agreement, firewall pinholes, and one-way audio symptoms.",
        "SIP: RFC 3261 — https://www.rfc-editor.org/rfc/rfc3261 ; SIP over TLS: RFC 5630 — https://www.rfc-editor.org/rfc/rfc5630"
    ),
    "RTP / RTCP" to chapter(
        "RTP carries real-time audio/video payloads while RTCP provides reception reports and session monitoring. RTP does not guarantee delivery or reserve network capacity.",
        "RTP headers include payload type, sequence number, timestamp, SSRC, and optional CSRC/extensions. RTCP sender/receiver reports summarize loss, jitter, and timing.",
        "A session selects payload formats and clock rates; senders timestamp and sequence packets; receivers reorder within a jitter buffer and use RTCP feedback for quality insight.",
        "RTP is not encrypted by default. Use SRTP/SRTCP where confidentiality, integrity, and replay protection are required; protect signaling that negotiates keys.",
        "Latency and jitter matter more than perfect retransmission. Packet loss, jitter-buffer depth, codec bitrate, and path congestion determine perceived quality.",
        "Plan payload types, clock rates, SSRC collision handling, RTCP reporting, QoS, jitter buffers, and SRTP key management.",
        "Inspect sequence gaps, jitter, RTCP loss reports, clock drift, codec negotiation, NAT traversal, and whether media follows the intended path.",
        "RTP/RTCP: RFC 3550 — https://www.rfc-editor.org/rfc/rfc3550 ; SRTP: RFC 3711 — https://www.rfc-editor.org/rfc/rfc3711"
    ),
    "MQTT" to chapter(
        "MQTT is a lightweight publish/subscribe messaging protocol. Clients connect to a broker, publish to topics, and subscribe to topic filters rather than addressing each other directly.",
        "The fixed header identifies packet type and remaining length; packet-specific variable headers and payloads carry CONNECT, PUBLISH, SUBSCRIBE, acknowledgments, and control data.",
        "A client establishes a session, authenticates, subscribes, receives matching publications, and uses QoS handshakes to manage delivery semantics.",
        "Use TLS, broker authentication, per-topic authorization, client identity controls, and retained-message governance. QoS is not a security guarantee.",
        "QoS 0 minimizes overhead; QoS 1 may duplicate; QoS 2 adds handshake cost. Broker fan-out, retained messages, session persistence, and topic cardinality affect scale.",
        "Design topic hierarchy, QoS, clean-start/session expiry, keepalive, retained messages, will messages, quotas, and offline delivery policy.",
        "Check CONNECT reason code, keepalive, ACL, topic filter, QoS exchange, broker limits, retained state, and session expiry.",
        "OASIS MQTT 5.0 specification: https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html"
    ),
    "CoAP" to chapter(
        "CoAP is a constrained application protocol designed for resource-limited devices and networks. It uses a REST-like model with compact messages and optional reliability features.",
        "CoAP messages include version, type, token length, code, message ID, token, options, and payload marker. UDP is common; other transports are defined by separate mappings.",
        "A client sends a request to a resource URI; confirmable messages are acknowledged or retransmitted, while non-confirmable messages are sent without message-level acknowledgment.",
        "Use DTLS or OSCORE according to deployment requirements, authenticate device identity, and restrict resource authorization. Constrained devices still need secure update and key lifecycle policy.",
        "Compact encoding reduces overhead. Retransmission timers, sleepy-device availability, blockwise transfer, and proxy caching influence response time and battery use.",
        "Plan resource naming, confirmable/non-confirmable use, blockwise transfer, observe relationships, multicast scope, and security context provisioning.",
        "Check message ID/token matching, retransmission count, blockwise options, DTLS/OSCORE context, proxy behavior, and device sleep schedule.",
        "CoAP: RFC 7252 — https://www.rfc-editor.org/rfc/rfc7252 ; OSCORE: RFC 8613 — https://www.rfc-editor.org/rfc/rfc8613"
    ),
    "AMQP" to chapter(
        "AMQP is a message-oriented middleware protocol for reliable transfer between messaging clients and brokers. AMQP 1.0 defines a peer-to-peer protocol with links, sessions, and connections.",
        "AMQP 1.0 uses a binary frame format with performatives such as Open, Begin, Attach, Flow, Transfer, Disposition, Detach, End, and Close.",
        "Peers establish a connection, create sessions, attach sender/receiver links, exchange flow control, transfer messages, settle delivery outcomes, and close resources.",
        "Use TLS, SASL authentication, authorization on nodes/addresses, and broker isolation. Delivery settlement is distinct from message trust or application authorization.",
        "Link credit and settlement mode govern backpressure and delivery semantics. Message size, persistence, broker storage, and consumer concurrency affect throughput.",
        "Document address model, link credit, durability, settlement, transactions, dead-letter behavior, and broker interoperability. Specify whether AMQP 1.0 or a different AMQP version is used.",
        "Check SASL/TLS, frame negotiation, link attach, flow credit, unsettled deliveries, settlement outcome, and broker resource limits.",
        "OASIS AMQP 1.0: https://docs.oasis-open.org/amqp/core/v1.0/amqp-core-complete-v1.0.pdf ; ISO/IEC 19464"
    ),
    "mDNS" to chapter(
        "Multicast DNS enables name resolution on a local link without conventional unicast DNS infrastructure. It is commonly used for zero-configuration discovery.",
        "DNS-format messages are sent to the IPv4 multicast group 224.0.0.251 or IPv6 ff02::fb, typically UDP port 5353, with link-local scope and special query behavior.",
        "Devices announce records and answer queries on the local link. Known-answer suppression, probing, and conflict resolution reduce redundant traffic and duplicate names.",
        "mDNS is not an authenticated global naming system. Treat discovered services as untrusted, isolate guest networks, and avoid exposing sensitive service metadata.",
        "Multicast traffic can become noisy on busy segments. Sleep proxies, cache-flush records, TTL behavior, and Wi-Fi multicast handling affect discovery reliability.",
        "Keep discovery within intended link boundaries, document service types, and avoid routing mDNS indiscriminately between VLANs.",
        "Check multicast delivery, firewall rules, IPv4/IPv6 group membership, duplicate-name conflicts, and whether devices are on the same link.",
        "mDNS: RFC 6762 — https://www.rfc-editor.org/rfc/rfc6762"
    ),
    "DNS-SD" to chapter(
        "DNS-Based Service Discovery advertises and discovers services using DNS records. It can operate with unicast DNS or mDNS and is often paired with mDNS on local links.",
        "PTR records enumerate service instances; SRV identifies target host and port; TXT carries service metadata; A/AAAA resolves the target host.",
        "A client queries a service type, obtains instance names, resolves SRV/TXT information, then resolves the target host address before connecting.",
        "Discovery records are not proof that a service is trustworthy. Validate service identity and authorization at the application layer and avoid publishing secrets in TXT records.",
        "Multiple query steps can add latency; caching and multicast suppression improve efficiency. Large service populations increase multicast traffic.",
        "Choose service types consistently, publish accurate SRV/TXT data, and define whether discovery should cross network boundaries.",
        "Check PTR enumeration, SRV target/port, TXT syntax, host A/AAAA, TTL, and multicast/unicast DNS mode.",
        "DNS-SD: RFC 6763 — https://www.rfc-editor.org/rfc/rfc6763"
    ),
    "DNS over TLS (DoT)" to chapter(
        "DoT carries DNS messages over a TLS-protected connection, commonly between a stub or forwarder and a recursive resolver. It protects the DNS transport channel from passive observation.",
        "DNS messages retain their normal wire format and are prefixed with a two-byte message length over the TLS stream. The well-known service port is TCP 853.",
        "A client authenticates the resolver's TLS identity, opens a persistent connection, sends length-prefixed DNS messages, and receives corresponding responses.",
        "Correct certificate and resolver-name validation are essential. DoT does not authenticate DNS data origin; DNSSEC validation addresses a different trust property.",
        "Connection reuse reduces handshake overhead. TCP head-of-line behavior and resolver distance influence latency; persistent connections need lifecycle management.",
        "Plan resolver discovery, certificate validation, fallback policy, privacy expectations, and network access to TCP 853. Avoid silently downgrading without an explicit policy.",
        "Check TLS handshake, SNI/authentication name, certificate chain, TCP 853 reachability, and DNS response independently.",
        "DoT: RFC 7858 — https://www.rfc-editor.org/rfc/rfc7858 ; usage profiles: RFC 8310 — https://www.rfc-editor.org/rfc/rfc8310"
    ),
    "DNS over QUIC (DoQ)" to chapter(
        "DoQ maps DNS queries to dedicated QUIC connections to provide transport confidentiality and reliable message delivery with QUIC stream behavior.",
        "DNS messages are length-prefixed on QUIC streams. The mapping uses QUIC connection and stream semantics rather than the classic UDP/TCP DNS framing model.",
        "The client establishes QUIC with authenticated server identity, opens a stream for a DNS exchange, sends one query, and receives the response under the DoQ mapping rules.",
        "QUIC protects the transport, but DNSSEC remains separate for data-origin authentication. Validate resolver identity and protect application handling of DNS answers.",
        "QUIC can reduce connection setup latency and avoids TCP's cross-stream head-of-line blocking. UDP reachability and resolver implementation support remain deployment constraints.",
        "Confirm DoQ client/resolver compatibility, authenticated resolver naming, ALPN/service discovery as applicable, and fallback policy.",
        "Check UDP path, QUIC handshake, server identity, stream errors, DNS message framing, and resolver policy.",
        "DoQ: RFC 9250 — https://www.rfc-editor.org/rfc/rfc9250 ; QUIC: RFC 9000 — https://www.rfc-editor.org/rfc/rfc9000"
    ),
    "DNSSEC" to chapter(
        "DNSSEC adds origin authentication and integrity protection to DNS data using digital signatures and a chain of trust. It does not encrypt DNS queries or hide requested names.",
        "DNSKEY publishes public keys, RRSIG signs RRsets, DS links child keys to parent zones, and NSEC/NSEC3 provide authenticated denial of existence.",
        "A validator follows delegation from a trust anchor, validates DS/DNSKEY relationships, verifies RRset signatures, and returns secure, insecure, or bogus results.",
        "Protect signing keys, maintain rollover procedures, and monitor signature expiry. DNSSEC can fail closed for bogus data; validation errors can make names unavailable.",
        "Signatures increase response size and may require TCP fallback or EDNS support. Validation adds CPU cost, while caching can reduce repeated verification.",
        "Plan KSK/ZSK or algorithm policy, DS publication, key rollover, signature validity, negative answers, and registrar coordination.",
        "Check chain of trust, DS mismatch, expired RRSIG, clock skew, unsupported algorithms, truncation, and resolver validation state.",
        "DNSSEC: RFC 4033 — https://www.rfc-editor.org/rfc/rfc4033 ; records: RFC 4034 — https://www.rfc-editor.org/rfc/rfc4034 ; protocol changes: RFC 4035 — https://www.rfc-editor.org/rfc/rfc4035"
    ),
    "Trojan Proxy" to chapter(
        "Trojan is an application-layer proxy design that uses a TLS-protected connection and an authentication token to distinguish authorized proxy sessions. It is a project protocol, not the Trojan malware family.",
        "The commonly documented design places an authentication value and target-address request within a TLS connection, followed by relayed stream data. Exact behavior depends on the implementation and version.",
        "A client establishes TLS to a server, presents its configured authentication material, requests a destination, and then exchanges proxied stream bytes through the server.",
        "Security depends on TLS certificate validation, secret protection, server hardening, authorization, and safe logging. A TLS-looking handshake alone does not prove a deployment is trustworthy.",
        "The TLS and proxy layers add handshake and relay overhead. Performance depends on RTT, encryption, server load, congestion, and connection multiplexing support.",
        "Treat this as a protocol-family reference only. Review official project documentation, maintenance status, platform policy, and threat model before any deployment decision.",
        "Separate TLS failure from proxy authentication or destination-connect failure. Verify server identity and application logs; do not infer that a reachable port means the service is healthy.",
        "Official project: https://github.com/trojan-gfw/trojan ; TLS 1.3: RFC 8446 — https://www.rfc-editor.org/rfc/rfc8446"
    ),
    "VLESS" to chapter(
        "VLESS is a lightweight proxy protocol in the Project V ecosystem. It defines user identification and request metadata while relying on an external transport/security layer for protection.",
        "The protocol request contains a version, user identifier, optional addons, command, destination port/address, and subsequent payload. Implementations and transport combinations determine wire behavior.",
        "A client establishes the selected transport, sends protocol identity and destination metadata, and relays application data through the server after authorization.",
        "VLESS itself should not be treated as encryption. Security depends on the transport, authenticated server identity, key/certificate management, access control, and current implementation.",
        "Transport choice dominates latency and overhead. Multiplexing, congestion behavior, server processing, and relay distance affect observed performance.",
        "This entry is an educational protocol overview, not a tunnel setup guide. Verify project documentation, implementation compatibility, and local network policy before evaluating a deployment.",
        "Check the outer transport and TLS/security layer first, then protocol version, identity authorization, destination metadata, and server-side relay logs.",
        "Project V documentation: https://www.v2fly.org/ ; protocol implementation references: https://github.com/v2fly/v2ray-core"
    ),
    "VMess" to chapter(
        "VMess is a proxy protocol family historically associated with V2Ray. It defines client identity and request framing and has evolved across implementations.",
        "VMess uses versioned request/response framing with identity-derived authentication and command/address metadata. Exact cryptographic and framing behavior is implementation-version dependent.",
        "A client and server establish the configured transport, authenticate protocol messages, negotiate or derive session parameters, and relay application traffic.",
        "Do not assume all VMess implementations or legacy modes provide equivalent security. Prefer current maintained documentation, authenticated transport, and protected credential storage.",
        "Encryption and framing overhead, multiplexing, transport choice, and implementation age affect performance. Legacy compatibility can increase complexity and risk.",
        "Document exact implementation/version and transport separately. Treat protocol compatibility as a tested property rather than relying on generic labels.",
        "Check version compatibility, client identity, clock and key material, outer transport, and server logs; distinguish protocol rejection from transport failure.",
        "Project V documentation: https://www.v2fly.org/ ; source: https://github.com/v2fly/v2ray-core"
    ),
    "Shadowsocks" to chapter(
        "Shadowsocks is a secure proxy protocol designed around encrypted proxy traffic. It uses a method and secret shared between client and server, with protocol behavior defined by the maintained project specification.",
        "The commonly used AEAD protocol carries encrypted payloads with nonce-based authenticated encryption. Address-type fields identify IPv4, IPv6, or domain destinations within the protected stream.",
        "A client establishes a transport to the server, encrypts destination metadata and application data, and the server decrypts and relays the stream.",
        "Use maintained AEAD methods and protect secrets. Legacy stream-cipher methods are obsolete; server identity and transport security must be evaluated separately.",
        "Cipher choice, packet framing, relay RTT, and server capacity affect throughput. TCP relay behavior can inherit stream-level head-of-line effects.",
        "Use only current official specifications and implementations. Separate proxy protocol from optional plugin transports and follow the network owner's rules.",
        "Check method compatibility, secret encoding, nonce/framing, server reachability, DNS behavior, and plugin/transport compatibility.",
        "Official specification: https://shadowsocks.org/doc/ ; project source: https://github.com/shadowsocks"
    ),
    "ShadowsocksR" to chapter(
        "ShadowsocksR is a derivative protocol and implementation ecosystem related to Shadowsocks, with additional protocol and obfuscation components. It is not an IETF standard.",
        "Wire behavior depends on the particular SSR implementation, protocol plugin, obfuscation plugin, cipher, and version; there is no single universally authoritative interoperable wire profile.",
        "A client and server must agree on the same implementation-specific options before the encrypted/obfuscated relay can be interpreted.",
        "Security claims must be assessed per implementation and cryptographic method. Obfuscation is not a substitute for encryption, authentication, or a sound threat model.",
        "Extra framing and transformations add overhead and can complicate debugging. Compatibility is sensitive to exact client/server/plugin versions.",
        "This entry is informational only. Prefer maintained, reviewed specifications and do not assume SSR has standards-body certification or uniform interoperability.",
        "Compare client/server versions and plugin settings at a high level, then inspect transport and authentication logs without weakening security checks.",
        "Project documentation and source vary by maintained fork; verify the exact repository and revision. Shadowsocks reference: https://shadowsocks.org/doc/"
    ),
    "TUIC" to chapter(
        "TUIC is a proxy protocol implementation built on QUIC transport. It uses QUIC streams and datagrams to carry proxy sessions and can benefit from QUIC connection properties.",
        "The application protocol defines authentication and proxy command framing over QUIC streams/datagrams. Exact fields and supported versions must be checked against the official project specification.",
        "A client establishes a QUIC connection, authenticates to the server, requests a target, and carries stream or datagram traffic within the negotiated session.",
        "Security relies on QUIC/TLS authentication, correct certificate validation, secret handling, and server authorization. Protocol-specific authentication does not replace secure endpoint identity.",
        "QUIC multiplexing and datagram support can reduce some transport overhead, but UDP availability, congestion control, and server CPU remain important.",
        "Treat as project-specific protocol documentation. Verify release compatibility, official project status, transport assumptions, and network policy; this app does not configure or run tunnels.",
        "Check QUIC handshake and certificate state before inspecting application authentication, stream/datagram mode, and server compatibility.",
        "Official project: https://github.com/EAimTY/tuic ; QUIC: RFC 9000 — https://www.rfc-editor.org/rfc/rfc9000"
    ),
    "Hysteria" to chapter(
        "Hysteria is a proxy protocol project built over QUIC, designed for high-throughput and loss-tolerant transport scenarios. Its behavior is version-specific.",
        "The protocol uses QUIC transport with project-defined authentication and stream/datagram framing. Congestion-control and protocol extensions must be read from the exact major-version documentation.",
        "A client authenticates over QUIC, requests proxy services, and transfers data using the protocol's supported stream or datagram mechanisms.",
        "Use authenticated TLS identity, strong credentials, maintained releases, and server access controls. Congestion-control tuning must not be mistaken for a security control.",
        "QUIC provides transport recovery and congestion control; performance depends on path loss, RTT, bandwidth, implementation, and configured congestion algorithm.",
        "This is a reference page only. Verify exact Hysteria version and official docs; do not assume settings or behavior from older releases apply to current releases.",
        "Check QUIC connectivity, TLS identity, version compatibility, authentication, UDP path quality, and server resource metrics.",
        "Official documentation: https://v2.hysteria.network/ ; QUIC: RFC 9000 — https://www.rfc-editor.org/rfc/rfc9000"
    ),
    "SSH" to chapter(
        "SSH is a secure remote-login and secure-channel protocol suite. It separates transport security, user authentication, and connection multiplexing into distinct protocol layers.",
        "SSH transport packets contain packet length, padding length, payload, padding, and integrity protection. Key exchange, service negotiation, authentication, and channel messages have separate message types.",
        "Peers negotiate algorithms and exchange host keys, authenticate the server, authenticate the user, open channels, and carry shell, command, forwarding, or subsystem traffic.",
        "Verify host keys, use modern algorithms, protect private keys, restrict accounts and forwarding, and apply least privilege. Password or key authentication is only as safe as server policy.",
        "Connection multiplexing amortizes handshakes. Cipher choice, compression policy, RTT, channel window, and server limits influence interactive and file-transfer performance.",
        "Maintain host-key inventory, key rotation, authorized-key policy, MFA where available, audit logs, and restricted forwarding. Separate SFTP subsystem access from shell access.",
        "Check host-key mismatch, key exchange compatibility, authentication logs, account shell/subsystem policy, channel limits, and network reachability.",
        "SSH architecture: RFC 4251 — https://www.rfc-editor.org/rfc/rfc4251 ; transport: RFC 4253 — https://www.rfc-editor.org/rfc/rfc4253 ; user authentication: RFC 4252 — https://www.rfc-editor.org/rfc/rfc4252"
    ),
    "RDP" to chapter(
        "Remote Desktop Protocol provides remote interactive presentation and device/resource redirection for supported desktop systems. It combines session establishment, graphics, input, and virtual channels.",
        "RDP uses negotiation and security exchange followed by MCS/GCC channel setup and virtual-channel data. Modern implementations can use TLS and Network Level Authentication.",
        "The client negotiates security and capabilities, authenticates, establishes channels, then exchanges display updates, input events, and optional device redirection.",
        "Require Network Level Authentication where supported, strong identity controls, TLS, patching, MFA, and restricted network exposure. Avoid direct public exposure of remote desktop services.",
        "Graphics codec, display resolution, RTT, bandwidth, channel redirection, and host CPU/GPU affect responsiveness.",
        "Use gateways or managed remote-access architecture, limit clipboard/drive/device redirection, audit sessions, and enforce account lockout and patch policy.",
        "Check authentication/NLA, TLS, gateway policy, licensing, session host resources, UDP/TCP transport, and device redirection restrictions.",
        "Microsoft protocol specifications: https://learn.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpbcgr/ ; security guidance: https://learn.microsoft.com/en-us/windows-server/remote/remote-desktop-services/"
    ),
    "NTP Security (NTS)" to chapter(
        "Network Time Security adds cryptographic authentication and key establishment to NTP. It separates a TLS-based key establishment exchange from authenticated NTP extension fields.",
        "NTS-KE negotiates protocol parameters over TLS; NTP packets then carry cookies and authenticated extension fields that bind requests and responses to the established security context.",
        "A client authenticates the NTS-KE server, obtains cookies and algorithm parameters, then uses cookies to authenticate NTP exchanges without a TLS handshake for every time query.",
        "Validate the NTS-KE certificate and server identity, protect cookies, and maintain secure key/algorithm policy. NTS authenticates time exchange but does not guarantee source accuracy.",
        "Cookie reuse and stateless server processing reduce per-query state. TLS setup, cookie renewal, network path quality, and NTP filtering still affect accuracy.",
        "Deploy redundant authenticated time sources, monitor offset and root dispersion, and ensure NTS-KE plus NTP traffic can reach the intended services.",
        "Check NTS-KE TLS validation, cookie expiry/acceptance, extension-field handling, time-source selection, and UDP 123 reachability.",
        "NTS: RFC 8915 — https://www.rfc-editor.org/rfc/rfc8915 ; NTPv4: RFC 5905 — https://www.rfc-editor.org/rfc/rfc5905"
    )
)
