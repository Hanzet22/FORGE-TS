package com.HG.Forge.Ts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class KnowledgeEntry(
    val id: String,
    val section: String,
    val title: String,
    val summary: String,
    val explanation: String,
    val example: String,
    val definition: String = summary,
    val function: String = explanation,
    val advantages: List<String> = emptyList(),
    val limitations: List<String> = emptyList(),
    val environments: List<String> = emptyList(),
    val tutorial: List<String> = emptyList(),
    val troubleshooting: List<String> = emptyList(),
    val standards: List<String> = emptyList()
)

private val knowledgeEntries = listOf(
    KnowledgeEntry(
        "mt-basics", "MikroTik", "RouterOS orientation",
        "Understand the RouterOS workspace and configuration model.",
        "RouterOS manages router interfaces, addressing, routing, services and firewall policy. WinBox and WebFig are graphical management options; the terminal is a command-line interface. Start by identifying the device and reviewing its current state before changing configuration.",
        "/system resource print\n/system identity print"
    ),
    KnowledgeEntry(
        "mt-interface", "MikroTik", "Interface inventory",
        "Read interface names, types and link status.",
        "An interface is a physical or logical network connection. Confirm interface names and link state before using an interface in a lesson or network diagram. Interface names vary by device and configuration.",
        "/interface print"
    ),
    KnowledgeEntry(
        "mt-address", "MikroTik", "IP addressing",
        "Learn how addresses are assigned to router interfaces.",
        "An IP address identifies a Layer 3 interface within a network. Pair the address with the correct prefix length and verify that the selected interface belongs to the intended network. This lesson is conceptual; it does not apply configuration changes.",
        "/ip address print"
    ),
    KnowledgeEntry(
        "mt-route", "MikroTik", "Routing table",
        "Read connected and learned routes.",
        "The routing table helps explain how a router selects a next hop for a destination. A default route is used when no more-specific route matches. Check route status and gateway reachability when diagnosing connectivity.",
        "/ip route print"
    ),
    KnowledgeEntry(
        "mt-dhcp", "MikroTik", "DHCP overview",
        "Understand address pools, servers and leases.",
        "DHCP can provide clients with IP addresses and related network settings. A DHCP server, address pool and interface must agree with the intended LAN design. Review existing leases and server state before making changes.",
        "/ip dhcp-server print\n/ip dhcp-server lease print"
    ),
    KnowledgeEntry(
        "mt-firewall", "MikroTik", "Firewall inspection",
        "Read filter rules and understand rule order.",
        "Firewall filter rules are evaluated in order. A rule can accept, drop or reject traffic depending on its chain and match conditions. This reference is read-only: review existing policy and use a controlled lab for any configuration practice.",
        "/ip firewall filter print"
    ),
    KnowledgeEntry(
        "os-ipconfig", "OS Commands", "Windows: ipconfig",
        "Inspect Windows network interface configuration.",
        "Displays IP address, subnet mask and default gateway information for network adapters. Useful as a first step when a Windows device cannot reach the local network.",
        "ipconfig /all"
    ),
    KnowledgeEntry(
        "os-ipaddr", "OS Commands", "Linux: ip address",
        "Inspect Linux interfaces and assigned addresses.",
        "Shows network interfaces and their assigned IPv4 or IPv6 addresses. The command is observational and does not change network configuration.",
        "ip address show"
    ),
    KnowledgeEntry(
        "os-route", "OS Commands", "Route table",
        "Inspect the routes used by the operating system.",
        "Use the Windows or Linux form to review destination prefixes, gateways and interface selection. A missing or incorrect default route can prevent access beyond the local network.",
        "Windows: route print\nLinux: ip route show"
    ),
    KnowledgeEntry(
        "os-dns", "OS Commands", "DNS lookup",
        "Query DNS records to troubleshoot name resolution.",
        "nslookup is available on Windows and many Linux distributions. It can query a hostname against the configured resolver or a resolver you specify. Avoid sending private internal hostnames to public resolvers.",
        "nslookup example.com"
    ),
    KnowledgeEntry(
        "os-ping", "OS Commands", "Ping",
        "Check basic reachability and round-trip time.",
        "Ping sends diagnostic echo requests to a destination. Some networks block echo requests, so no reply does not always mean the host is offline. Only test systems and networks you are permitted to diagnose.",
        "Windows: ping example.com\nLinux: ping -c 4 example.com"
    ),
    KnowledgeEntry(
        "os-trace", "OS Commands", "Trace a network path",
        "Observe the hops toward a destination.",
        "Traceroute tools show the path that packets may take. Intermediate routers may not respond, and paths can vary. Use this for troubleshooting on networks where you have permission.",
        "Windows: tracert example.com\nLinux: traceroute example.com"
    ),
    KnowledgeEntry(
        "os-system", "OS Commands", "System information",
        "Identify the operating system and kernel version.",
        "System information helps when checking compatibility, documenting a lab machine or reporting a technical issue.",
        "Windows: systeminfo\nLinux: uname -a"
    ),
    KnowledgeEntry(
        "ssh-basics", "SSH & TLS", "SSH fundamentals",
        "Understand encrypted remote administration and host identity.",
        "Secure Shell (SSH) provides an encrypted channel between an SSH client and server. Server host keys help the client recognize the server on later connections. Verify a host-key change through a trusted administrator or another trusted channel before accepting it; do not blindly bypass host-key warnings.",
        "ssh -V\nssh -G example.com"
    ),
    KnowledgeEntry(
        "ssh-keys", "SSH & TLS", "SSH keys and authentication",
        "Learn the role of public and private keys.",
        "An SSH key pair contains a private key that must remain secret and a public key that can be installed on an account or server. A passphrase can protect a private key at rest. Never paste private-key contents into chat, logs or support requests.",
        "ssh-keygen -lf ~/.ssh/id_ed25519.pub"
    ),
    KnowledgeEntry(
        "tls-basics", "SSH & TLS", "TLS and HTTPS",
        "Understand encryption, server authentication and integrity.",
        "Transport Layer Security (TLS) protects data in transit. During a TLS handshake, peers negotiate cryptographic parameters and the server presents a certificate. A valid certificate helps bind a server identity to a public key; encryption alone does not prove that the intended service is trustworthy.",
        "HTTPS uses TLS to protect HTTP connections.\nDefault HTTPS port: 443"
    ),
    KnowledgeEntry(
        "tls-certificates", "SSH & TLS", "Certificate inspection",
        "Read certificate identity, validity and trust information.",
        "Check the certificate subject or subject alternative names, validity dates, issuer and verification result. A certificate can be expired, issued for another hostname or chain to an untrusted authority. Never treat disabling certificate verification as a fix.",
        "openssl version\nopenssl s_client -connect example.com:443 -servername example.com -brief"
    ),
    KnowledgeEntry("ip-v4", "Addressing", "IPv4 addressing", "32-bit addresses, dotted-decimal notation and subnet boundaries.", "IPv4 uses 32-bit addresses, commonly written as four decimal octets. A prefix length identifies the network portion. Private ranges are intended for internal networks and are not globally routed.", "Example: 192.168.10.25/24"),
    KnowledgeEntry("ip-v6", "Addressing", "IPv6 addressing", "128-bit addresses and hexadecimal notation.", "IPv6 uses 128-bit addresses written as hexadecimal groups separated by colons. Double-colon compression can replace one consecutive run of zero groups. IPv6 has no broadcast address; multicast and anycast serve related delivery roles.", "Example: 2001:db8:10::25/64"),
    KnowledgeEntry("ip-cidr", "Addressing", "CIDR and prefix length", "Classless network notation such as /24 or /64.", "CIDR expresses the network prefix length directly and supports flexible subnet sizes and route aggregation. A smaller IPv4 prefix number means a larger address block.", "192.168.10.0/24 = 256 total IPv4 addresses"),
    KnowledgeEntry("ip-vlsm", "Addressing", "VLSM planning", "Use different subnet sizes within one address block.", "Variable Length Subnet Masking allocates a subnet sized for each segment rather than splitting every segment into equal blocks. Sort requirements by largest host need, round each block to a power of two, and respect subnet boundaries.", "Example requirements: Office 50 hosts; Lab 25; Management 10"),
    KnowledgeEntry("ip-binary", "Addressing", "Binary address notation", "Understand how IP addresses map to bits.", "Each IPv4 octet is represented by eight bits. IPv6 contains 128 bits, commonly grouped into eight 16-bit hexadecimal groups. Prefix lengths count the network bits from the left.", "IPv4: 192.168.1.10 = 11000000.10101000.00000001.00001010"),
    KnowledgeEntry("net-pan", "Network Types", "PAN — Personal Area Network", "A short-range network around one person.", "A PAN connects personal devices over a small range. Bluetooth is a common PAN technology, but not every PAN device requires an independently routed IP address. Addressing depends on the technology and gateway design.", "Example: phone connected to a personal wearable"),
    KnowledgeEntry("net-lan", "Network Types", "LAN — Local Area Network", "Connect devices within a limited local area.", "A LAN commonly connects workstations, printers, servers and network infrastructure in a home, school or office. Its IP plan should account for clients, gateways, infrastructure and future growth.", "Example: 192.168.10.0/24"),
    KnowledgeEntry("net-wlan", "Network Types", "WLAN — Wireless LAN", "A LAN using wireless access, commonly Wi-Fi.", "A WLAN connects wireless clients through access points. Guest, staff and infrastructure traffic may be separated into different SSIDs and VLANs according to the network policy. Wireless does not mean the network has a separate IP protocol.", "Example: staff Wi-Fi and guest Wi-Fi mapped to separate VLANs"),
    KnowledgeEntry("net-vlan", "Network Types", "VLAN — Virtual LAN", "Logical Layer 2 segmentation over shared switching infrastructure.", "A VLAN creates a separate logical broadcast domain on a bridged network. A routed VLAN commonly uses its own IP subnet and default gateway. IEEE 802.1Q defines VLAN tagging for Ethernet links.", "Example: VLAN 10 — Administration; VLAN 20 — Laboratory"),
    KnowledgeEntry("net-ipplan", "Network Types", "IP allocation for PAN, LAN, WLAN and VLAN", "Size each segment from real device and growth requirements.", "There is no universal IP quota for a network type. Estimate clients, infrastructure addresses, reservations and expected growth. A routed VLAN generally receives a dedicated subnet; a WLAN may share or separate a subnet depending on policy. Typical IPv6 LAN segments use /64.", "Required IPv4 capacity = clients + reserved addresses + growth allowance"),
    KnowledgeEntry("proto-dhcp", "Core Protocols", "DHCP and DHCPv6", "Automatically provide network configuration to clients.", "DHCPv4 can assign IPv4 addresses and options such as subnet mask, default gateway and DNS servers. DHCPv6 provides IPv6 configuration services; IPv6 hosts may also use SLAAC. Avoid running competing DHCP servers on the same segment.", "DHCPv4: UDP ports 67 (server) and 68 (client)"),
    KnowledgeEntry("proto-nat", "Core Protocols", "NAT — Network Address Translation", "Translate addresses between network realms.", "NAT changes address information as packets cross network boundaries. Common deployments translate private IPv4 addresses to a public address and maintain mapping state. NAT is not encryption and does not replace a firewall.", "Common term: source NAT / destination NAT"),
    KnowledgeEntry("proto-cgnat", "Core Protocols", "CGNAT — Carrier-Grade NAT", "Provider-operated address sharing for multiple subscribers.", "CGNAT performs address translation inside an internet service provider network, often sharing public IPv4 addresses among customers. The shared address block 100.64.0.0/10 is reserved for this purpose by RFC 6598 and is distinct from RFC 1918 private ranges.", "Shared address space: 100.64.0.0/10"),
    KnowledgeEntry("proto-tcp", "Core Protocols", "TCP", "Reliable, ordered, connection-oriented transport.", "Transmission Control Protocol establishes a connection and provides ordered byte-stream delivery, retransmission and congestion control. Applications use TCP when these transport properties are useful.", "Defined by RFC 9293"),
    KnowledgeEntry("proto-udp", "Core Protocols", "UDP", "Lightweight datagram transport without delivery guarantees.", "User Datagram Protocol sends independent datagrams with minimal transport mechanisms. UDP itself does not guarantee delivery, ordering or retransmission; applications may add their own reliability and congestion handling.", "Defined by RFC 768"),
    KnowledgeEntry("proto-tcpip", "Core Protocols", "TCP/IP model", "A practical model for the Internet protocol suite.", "TCP/IP refers to a family of interoperating protocols, not one single protocol. A common teaching model groups functions into link, internet, transport and application layers.", "Examples: Ethernet/Wi-Fi; IP; TCP/UDP; DNS/HTTP"),
    KnowledgeEntry("proto-osi", "Core Protocols", "OSI seven-layer model", "A reference model for describing network functions.", "The OSI model describes seven conceptual layers: Physical, Data Link, Network, Transport, Session, Presentation and Application. It is a learning and troubleshooting model, not a direct one-to-one implementation map for every TCP/IP protocol.", "Layer 1 → 7: Physical, Data Link, Network, Transport, Session, Presentation, Application"),
    KnowledgeEntry("proto-sshws", "Remote Access", "SSH over WebSocket (SSHWS)", "A deployment pattern that carries SSH through a WebSocket connection.", "SSHWS is commonly used as shorthand for SSH carried inside a WebSocket transport or gateway. It is not a separate IETF SSH standard. The security properties depend on the SSH endpoint, WebSocket gateway, TLS configuration and authentication policy.", "Conceptual path: SSH client → WebSocket/TLS gateway → SSH server"),
    KnowledgeEntry("proto-trojan", "Proxy Protocols", "Trojan proxy protocol", "A TLS-based proxy protocol with an authenticated request format.", "The Trojan proxy protocol performs a TLS handshake and then sends an authenticated destination request before relaying traffic. It is designed to make the proxy connection resemble ordinary TLS-protected web traffic, but traffic appearance is not proof that a service is safe or indistinguishable in every network. This protocol is distinct from Trojan horse malware.", "Project reference: trojan-gfw protocol specification"),
    KnowledgeEntry("proto-vless", "Proxy Protocols", "VLESS", "A lightweight proxy protocol in the Project X ecosystem.", "VLESS is a stateless proxy protocol that uses UUID-based client authentication. It does not itself provide the same built-in encryption model as a complete secure transport; deployments rely on a protected transport such as TLS or another supported security layer.", "Project reference: Project X VLESS documentation"),
    KnowledgeEntry("proto-vmess", "Proxy Protocols", "VMess", "An encrypted transport protocol in the Project X ecosystem.", "VMess is used by compatible clients and servers in the V2Ray/Xray ecosystem. Its protocol and compatibility are implementation-specific; system time synchronization is an important operational dependency described by Project X documentation.", "Project reference: Project X VMess documentation"),
    KnowledgeEntry("proto-ss", "Proxy Protocols", "Shadowsocks", "An encrypted proxy protocol for TCP and UDP traffic.", "Shadowsocks uses authenticated encryption with a shared secret to protect proxy traffic. Shadowsocks is a proxy design rather than a general-purpose routed VPN. Keep credentials private and use maintained implementations.", "Project reference: Shadowsocks protocol specifications"),
    KnowledgeEntry("proto-ssr", "Proxy Protocols", "ShadowsocksR (SSR)", "A Shadowsocks-derived protocol family.", "ShadowsocksR is a derivative/legacy protocol family with implementation-specific protocol and obfuscation options. It is not an IETF standard, and client compatibility and security properties vary by implementation.", "Reference: compatible-client SSR documentation"),
    KnowledgeEntry("proto-tuic", "Proxy Protocols", "TUIC", "A proxy protocol designed to relay TCP and UDP traffic.", "TUIC uses multiplexed encrypted streams and is commonly deployed over QUIC. Its project specification describes authentication, TCP relay and UDP relay mechanisms. It is a project protocol, not an IETF standard.", "Project reference: tuic-protocol specification"),
    KnowledgeEntry("proto-hysteria", "Proxy Protocols", "Hysteria", "A TCP/UDP proxy protocol built on QUIC.", "Hysteria 2 uses QUIC and its unreliable datagram extension to relay TCP and UDP traffic. The project publishes a protocol specification; behavior and compatibility should be checked against the specific version.", "Project reference: Hysteria 2 protocol specification"),
    KnowledgeEntry("proto-quic", "Transport & Tunneling", "QUIC", "Secure multiplexed transport carried over UDP.", "QUIC provides streams, flow control, congestion control and connection migration. It integrates TLS 1.3 for handshake and packet protection. HTTP/3 uses QUIC as its transport.", "Defined by RFC 9000 and RFC 9001"),
    KnowledgeEntry("proto-wireguard", "VPN & Tunneling", "WireGuard", "A modern VPN tunnel that encapsulates IP packets over UDP.", "WireGuard uses public-key-based peer authentication and a compact cryptographic protocol. It creates a virtual network interface and uses allowed IP prefixes for cryptokey routing. Protect private keys and review peer permissions.", "Official reference: wireguard.com/protocol"),
    KnowledgeEntry("proto-openvpn", "VPN & Tunneling", "OpenVPN", "VPN software supporting secure tunnels over TCP or UDP.", "OpenVPN uses TLS-based control-channel security and can transport network traffic through a tunnel. Configuration choices affect performance and security; use official documentation and current supported versions.", "Official reference: OpenVPN protocol documentation"),
    KnowledgeEntry("proto-ipsec", "VPN & Tunneling", "IPsec", "A suite for protecting IP-layer communications.", "IPsec provides security services such as confidentiality, integrity and peer authentication at the IP layer. ESP is commonly used for encryption and integrity; IKE negotiates security associations in many deployments.", "Architecture: RFC 4301"),
    KnowledgeEntry("proto-l2tp", "VPN & Tunneling", "L2TP", "A Layer 2 tunneling protocol that carries PPP frames.", "L2TP tunnels PPP traffic across an IP network. L2TP by itself does not provide strong encryption; L2TP/IPsec combines tunneling with IPsec security.", "L2TP: RFC 2661; securing L2TP with IPsec: RFC 3193")
)


private data class LessonGuidance(
    val definition: String,
    val function: String,
    val advantages: List<String>,
    val limitations: List<String>,
    val environments: List<String>,
    val tutorial: List<String>,
    val troubleshooting: List<String>,
    val standards: List<String>
)

private fun guidanceFor(entry: KnowledgeEntry): LessonGuidance {
    val commonTutorial = listOf(
        "Read the definition and identify the problem this technology solves.",
        "Trace the example from input to output; note each device or protocol involved.",
        "Change one variable at a time and predict what should happen.",
        "Compare the result with the expected behavior, then explain why."
    )
    val commonTroubleshooting = listOf(
        "Confirm the configuration and assumptions before changing anything.",
        "Separate name-resolution, addressing, routing, transport and application symptoms.",
        "Record the exact symptom and the last known-good state.",
        "Change one item at a time and verify the result in an authorized lab."
    )
    val advantages = when (entry.section) {
        "Addressing" -> listOf("Makes network boundaries and address allocation explicit.", "Supports structured planning and easier troubleshooting.")
        "Core Protocols" -> listOf("Defines interoperable behavior between different systems.", "Can automate or standardize a recurring network function.")
        "SSH & TLS" -> listOf("Protects data in transit when configured and validated correctly.", "Provides a standard model for secure communication.")
        "Network Types" -> listOf("Helps organize devices and traffic by scope or purpose.", "Can improve manageability when the design is documented.")
        "MikroTik", "OS Commands" -> listOf("Provides direct visibility into configuration or system state.", "Useful as a repeatable first diagnostic step.")
        "VPN & Tunneling", "Transport & Tunneling", "Proxy Protocols", "Remote Access" -> listOf("Can connect or relay traffic across different network environments.", "Supports specific connectivity and deployment requirements.")
        else -> listOf("Provides a shared model for understanding network behavior.", "Makes common designs and failure modes easier to explain.")
    }
    val limitations = when (entry.section) {
        "Addressing" -> listOf("Incorrect prefixes or overlapping ranges can break communication.", "A mathematically valid plan can still conflict with an organization's existing routes.")
        "Core Protocols" -> listOf("A protocol does not guarantee that every surrounding device is configured correctly.", "Compatibility, security and performance depend on the implementation and environment.")
        "SSH & TLS" -> listOf("Encryption does not make an untrusted endpoint trustworthy.", "Certificate, key and identity checks still matter.")
        "MikroTik", "OS Commands" -> listOf("Output can differ by operating-system version and device configuration.", "A diagnostic command does not prove that every network layer is healthy.")
        "VPN & Tunneling", "Transport & Tunneling", "Proxy Protocols", "Remote Access" -> listOf("Adds operational complexity and may affect latency or compatibility.", "Security depends on endpoint identity, configuration and maintained software.")
        else -> listOf("The concept solves a specific problem, not every connectivity problem.", "Real deployments need validation against topology, policy and vendor behavior.")
    }
    val environments = when (entry.section) {
        "Addressing" -> listOf("IP planning for home, school, office and routed networks.", "Use documentation-only example ranges while learning.")
        "MikroTik" -> listOf("A router you administer or a controlled RouterOS lab.", "Read-only review before any approved configuration change.")
        "OS Commands" -> listOf("A device and network you own or are explicitly allowed to troubleshoot.", "Use local examples when practicing command syntax.")
        "SSH & TLS" -> listOf("Maintained services where endpoint identity and encrypted transport are required.", "Use fictional certificates and host keys in this learning module.")
        "Network Types" -> listOf("Campus, school, home, enterprise and managed wireless networks.", "Plan segmentation before deploying it.")
        "Core Protocols" -> listOf("Networks and applications that implement the relevant protocol standard.", "Use local simulations to understand protocol state.")
        else -> listOf("A documented, authorized environment that supports the relevant protocol.", "Use the local simulation for first-time practice.")
    }
    val tutorial = when (entry.id) {
        "ip-cidr", "ip-vlsm" -> listOf("Choose a documentation-only IPv4 network, such as 192.0.2.0/24.", "Select a prefix and calculate the total address block.", "Identify network and broadcast addresses for ordinary subnet sizes.", "Check that planned subnets do not overlap and fit inside the parent block.")
        "os-dns" -> listOf("Start with the fictional hostname portal.example.com.", "Check whether a local cache entry exists.", "Follow the resolver to the authoritative example record.", "Compare the returned record type and TTL with the expected answer.")
        "proto-dhcp" -> listOf("Begin with a fictional client that has no lease.", "Observe Discover and Offer in the local sequence.", "Observe Request and Acknowledge, then inspect the lease.", "Reset the scenario and explain what changes when the pool is empty.")
        "proto-tcp", "proto-udp" -> listOf("Choose a fictional application message.", "Compare connection setup and delivery expectations.", "Advance through the local event timeline.", "Explain what the application must do if a datagram is missing.")
        "mt-route" -> listOf("Read the fictional destination address.", "List every route whose prefix matches the destination.", "Select the most specific matching prefix.", "Explain the selected next hop or why no route exists.")
        "tls-basics" -> listOf("Identify the fictional hostname the client expects.", "Review the simulated certificate identity and validity.", "Follow the TLS handshake stages in order.", "Explain why a failed identity check must not be bypassed.")
        "ssh-basics", "ssh-keys" -> listOf("Review the fictional server identity shown by the lesson.", "Compare its host-key fingerprint with the trusted example.", "Choose the simulated authentication outcome.", "Explain the difference between server identity and user authorization.")
        else -> commonTutorial
    }
    val troubleshooting = when (entry.id) {
        "os-dns" -> listOf("Hostname typo or malformed query.", "Missing or incorrect record in the authoritative example zone.", "Stale cached answer or expired TTL.", "Wrong resolver selected; separate DNS failure from general connectivity.")
        "ip-cidr", "ip-vlsm" -> listOf("Wrong prefix length or subnet mask.", "Network and broadcast boundary counted as ordinary hosts.", "Overlapping subnet allocations.", "Address or default gateway outside the intended subnet.")
        "proto-dhcp" -> listOf("No offer: inspect the simulated pool and server state.", "Pool exhausted: compare available leases with requested clients.", "Wrong scope: compare subnet, gateway and DNS options.", "VLAN or relay mismatch: check whether the client and DHCP scope belong together.")
        "proto-tcp", "proto-udp" -> listOf("Connection refused and timeout are different symptoms.", "Check the fictional destination port and transport selection.", "A missing UDP datagram is not automatically retransmitted by UDP itself.", "Transport success does not prove the application is healthy.")
        "mt-route" -> listOf("No matching route.", "A less-specific route selected when a more-specific route was expected.", "Incorrect next hop in the fictional table.", "A route exists but the destination service is still unavailable.")
        "tls-basics", "tls-certificates" -> listOf("Hostname does not match the certificate identity.", "Certificate is outside its validity period.", "Issuer is not trusted by the example trust store.", "Do not disable verification to hide a validation failure.")
        "ssh-basics", "ssh-keys" -> listOf("Host-key mismatch: stop and verify identity through a trusted channel.", "Authentication failure is different from authorization failure.", "Timeout, refusal and no-route symptoms indicate different layers.", "Never share private keys or bypass host-key warnings.")
        else -> commonTroubleshooting
    }
    val standards = when (entry.id) {
        "os-dns" -> listOf("RFC 1034 — https://www.rfc-editor.org/rfc/rfc1034", "RFC 1035 — https://www.rfc-editor.org/rfc/rfc1035")
        "ip-v4" -> listOf("RFC 791 — https://www.rfc-editor.org/rfc/rfc791", "RFC 1918 — https://www.rfc-editor.org/rfc/rfc1918")
        "ip-v6" -> listOf("RFC 8200 — https://www.rfc-editor.org/rfc/rfc8200", "RFC 4291 — https://www.rfc-editor.org/rfc/rfc4291")
        "ip-cidr", "ip-vlsm" -> listOf("RFC 4632 — https://www.rfc-editor.org/rfc/rfc4632")
        "proto-dhcp" -> listOf("RFC 2131 — https://www.rfc-editor.org/rfc/rfc2131", "RFC 8415 — https://www.rfc-editor.org/rfc/rfc8415")
        "proto-tcp" -> listOf("RFC 9293 — https://www.rfc-editor.org/rfc/rfc9293")
        "proto-udp" -> listOf("RFC 768 — https://www.rfc-editor.org/rfc/rfc768")
        "tls-basics", "tls-certificates" -> listOf("RFC 9846 — https://www.rfc-editor.org/rfc/rfc9846")
        "ssh-basics", "ssh-keys" -> listOf("RFC 4251 — https://www.rfc-editor.org/rfc/rfc4251")
        "proto-quic" -> listOf("RFC 9000 — https://www.rfc-editor.org/rfc/rfc9000", "RFC 9001 — https://www.rfc-editor.org/rfc/rfc9001")
        "proto-osi" -> listOf("ITU-T X.200 — https://www.itu.int/rec/T-REC-X.200")
        "net-vlan" -> listOf("IEEE 802.1Q — https://standards.ieee.org/standard/802_1Q-2022.html")
        else -> listOf("Use the linked official project documentation and verify the protocol version before applying it.")
    }
    return LessonGuidance(
        definition = entry.summary,
        function = entry.explanation,
        advantages = advantages,
        limitations = limitations,
        environments = environments,
        tutorial = tutorial,
        troubleshooting = troubleshooting,
        standards = standards
    )
}

@Composable
private fun LessonSection(title: String, lines: List<String>) {
    if (lines.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        lines.forEach { line ->
            Text("• " + line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SimulationPanel(entryId: String) {
    val dnsStages = listOf(
        "1 · Local cache: no matching entry is stored.",
        "2 · Recursive resolver: receives the fictional lookup request.",
        "3 · Root and TLD referrals: the resolver follows the example delegation path.",
        "4 · Authoritative answer: portal.example.com resolves to 192.0.2.25.",
        "5 · Cache result: the answer is stored for its displayed TTL."
    )
    var stage by remember(entryId) { mutableStateOf(0) }
    var prefix by remember(entryId) { mutableStateOf(24f) }
    var dhcpStage by remember(entryId) { mutableStateOf(0) }
    var transport by remember(entryId) { mutableStateOf("TCP") }
    var destination by remember(entryId) { mutableStateOf("192.0.2.77") }

    if (entryId !in setOf("os-dns", "ip-cidr", "ip-vlsm", "proto-dhcp", "proto-tcp", "proto-udp", "mt-route")) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text("LOCAL LEARNING LAB", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text("SIMULATED · No packets sent · No network inspected", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            when (entryId) {
                "os-dns" -> {
                    Text("DNS resolution walkthrough", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(dnsStages[stage], style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { stage = 0 }) { Text("Reset") }
                        Button(onClick = { stage = (stage + 1) % dnsStages.size }) { Text("Next step") }
                    }
                    Text("Fixture: portal.example.com → 192.0.2.25", style = MaterialTheme.typography.bodySmall)
                }
                "ip-cidr", "ip-vlsm" -> {
                    val p = prefix.toInt()
                    val total = 1 shl (32 - p)
                    Text("IPv4 prefix calculator", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Example network: 192.0.2.0/" + p, style = MaterialTheme.typography.bodyMedium)
                    Slider(value = prefix, onValueChange = { prefix = it }, valueRange = 24f..30f, steps = 5)
                    Text("Prefix /" + p + " · " + total + " total addresses · " + (total - 2) + " conventional usable host addresses", style = MaterialTheme.typography.bodyMedium)
                    Text("Educational fixture only. /31 and /32 have different use cases and are not included in this simple host-count exercise.", style = MaterialTheme.typography.bodySmall)
                }
                "proto-dhcp" -> {
                    val stages = listOf("DISCOVER · fictional client asks for configuration.", "OFFER · fictional server proposes 192.0.2.40/24.", "REQUEST · client indicates the offered configuration.", "ACKNOWLEDGE · server confirms the example lease.")
                    Text("DHCPv4 DORA walkthrough", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(stages[dhcpStage], style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { dhcpStage = 0 }) { Text("Reset") }
                        Button(onClick = { dhcpStage = (dhcpStage + 1) % stages.size }) { Text("Next step") }
                    }
                }
                "proto-tcp", "proto-udp" -> {
                    Text("Transport behavior comparison", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { transport = "TCP" }) { Text("TCP") }
                        Button(onClick = { transport = "UDP" }) { Text("UDP") }
                    }
                    if (transport == "TCP") {
                        Text("SYN → SYN-ACK → ACK → application bytes → acknowledgements/retransmission as needed.", style = MaterialTheme.typography.bodyMedium)
                        Text("TCP provides an ordered reliable byte stream.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("Datagram A → Datagram B → Datagram C. A missing datagram is not retransmitted by UDP itself.", style = MaterialTheme.typography.bodyMedium)
                        Text("UDP provides datagrams without delivery or ordering guarantees.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                "mt-route" -> {
                    Text("Longest-prefix route selection", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { destination = "192.0.2.77" }) { Text("Host A") }
                        Button(onClick = { destination = "198.51.100.9" }) { Text("Host B") }
                    }
                    Text("Destination: " + destination, style = MaterialTheme.typography.bodyMedium)
                    val route = if (destination == "192.0.2.77") "192.0.2.0/24 → Lab gateway (most specific match)" else "198.51.100.0/24 → Office gateway (most specific match)"
                    Text("Selected route: " + route, style = MaterialTheme.typography.bodyMedium)
                    Text("The simulator uses fixed documentation-only routes.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun KnowledgeCenterScreen(modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    var selectedSection by remember { mutableStateOf("All") }
    var expandedId by remember { mutableStateOf<String?>(null) }
    val sections = listOf("All", "MikroTik", "OS Commands", "SSH & TLS", "Addressing", "Network Types", "Core Protocols", "Remote Access", "Transport & Tunneling", "VPN & Tunneling", "Proxy Protocols")
    val filtered = knowledgeEntries.filter { entry ->
        (selectedSection == "All" || entry.section == selectedSection) &&
            (query.isBlank() || listOf(entry.title, entry.summary, entry.explanation, entry.example, entry.section)
                .any { it.contains(query, ignoreCase = true) })
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    "KNOWLEDGE CENTER",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Learn. Inspect. Understand.",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Offline references for IP addressing, network design, transport, VPN and proxy protocols.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search lessons and commands") },
                singleLine = true
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sections.forEach { section ->
                    FilterChip(
                        selected = selectedSection == section,
                        onClick = { selectedSection = section },
                        label = { Text(section) }
                    )
                }
            }
        }
        item {
            Text(
                "${filtered.size} REFERENCES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(filtered, key = { it.id }) { entry ->
            val expanded = expandedId == entry.id
            Card(
                onClick = { expandedId = if (expanded) null else entry.id },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        entry.section.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(entry.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (expanded) {
                        Spacer(Modifier.height(2.dp))
                        val guidance = guidanceFor(entry)
                        LessonSection("1 · WHAT IS IT?", listOf(guidance.definition))
                        LessonSection("2 · EXPLANATION", listOf(entry.explanation))
                        LessonSection("3 · FUNCTION", listOf(guidance.function))
                        LessonSection("4 · ADVANTAGES", guidance.advantages)
                        LessonSection("5 · LIMITATIONS & TRADE-OFFS", guidance.limitations)
                        LessonSection("6 · SUITABLE ENVIRONMENTS", guidance.environments)
                        LessonSection("7 · STEP-BY-STEP TUTORIAL", guidance.tutorial.mapIndexed { index, value -> (index + 1).toString() + ". " + value })
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                entry.example,
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        SimulationPanel(entry.id)
                        LessonSection("8 · BUGS & TROUBLESHOOTING", guidance.troubleshooting)
                        LessonSection("STANDARDS & FURTHER READING", guidance.standards)
                        Text(
                            "Reference examples are read-only. The learning lab uses synthetic local data only; no commands or network requests are executed.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "Tap to read",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        if (filtered.isEmpty()) {
            item {
                Text(
                    "No matching references. Try another keyword or category.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
