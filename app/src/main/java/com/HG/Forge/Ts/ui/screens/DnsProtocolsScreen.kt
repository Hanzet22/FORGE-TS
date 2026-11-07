package com.HG.Forge.Ts.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.HG.Forge.Ts.data.network.DnsAnswer
import com.HG.Forge.Ts.data.network.DnsResolver
import com.HG.Forge.Ts.data.preferences.AppPreferencesRepository

private val dnsTypes = listOf("A","AAAA","CNAME","MX","TXT","NS","SRV","CAA","SOA","PTR","HTTPS","SVCB","DS","DNSKEY","TLSA","SSHFP","RRSIG","NSEC")
private data class Guide(val name:String,val group:String,val overview:String,val how:String,val pros:String,val cons:String,val risks:String,val refs:String)
private val guides = listOf(
Guide("DNS","Core","Maps names to records through a distributed hierarchy.","Query a fully qualified domain and record type with a resolver; edit authoritative records at your DNS provider, not here.","Distributed, cached and widely supported.","Caches and propagation can delay changes.","Resolver sees queried names; wrong records can break web or mail.","RFC 1034, 1035, 9499"),
Guide("DHCP / DHCPv6","Core","Automatically supplies network configuration to clients.","An administrator configures a trusted server and correctly scoped address pool.","Reduces manual work and address conflicts.","Depends on server and pool availability.","Rogue DHCP can redirect gateway or DNS settings; avoid competing servers.","RFC 2131, RFC 8415"),
Guide("NAT / CGNAT","Core","Translates addresses between network realms; CGNAT shares provider-side IPv4 space.","Configure only on managed routers with documented address and firewall policy.","Conserves IPv4 addresses and enables address sharing.","Can complicate inbound services, tracing and some applications.","NAT is not encryption or a firewall; shared public IPs can complicate attribution.","RFC 3022, RFC 6598"),
Guide("TCP","Transport","Reliable, ordered, connection-oriented byte-stream transport.","Applications open connections to authorized services and handle timeouts.","Ordering, retransmission and congestion control.","Connection setup and retransmissions add overhead.","Unbounded connections or exposed services can exhaust resources.","RFC 9293"),
Guide("UDP","Transport","Datagram transport without built-in delivery or ordering guarantees.","Use where the application implements any needed reliability and congestion control.","Low overhead and message-oriented.","No built-in delivery, ordering or retransmission.","Spoofing, amplification and congestion risks require application/network controls.","RFC 768"),
Guide("TCP/IP model","Core","Practical layered model of the Internet protocol suite.","Use link, internet, transport and application layers to organize troubleshooting.","Practical and widely used.","Layer boundaries are a teaching abstraction.","Misdiagnosis occurs when encapsulation or middleboxes are ignored.","RFC 1122"),
Guide("OSI model","Core","Seven-layer reference model for describing network functions.","Map symptoms to physical, data-link, network, transport, session, presentation and application functions.","Useful shared troubleshooting vocabulary.","Not a literal implementation map for every stack.","Treating layers as rigid real-world boundaries can mislead diagnosis.","ITU-T X.200"),
Guide("SSH","Remote access","Encrypted remote login and file transfer protocol.","Connect only to authorized hosts; verify host keys and protect private keys.","Encrypted sessions and key authentication.","Requires key, account and patch management.","Stolen keys or ignored host-key warnings can expose systems.","RFC 4251"),
Guide("SSH over WebSocket (SSHWS)","Remote access","Deployment pattern carrying SSH through a WebSocket gateway; not a separate SSH standard.","Use only an approved gateway; secure WebSocket/TLS and SSH layers independently.","Useful where an authorized service explicitly offers WebSocket transport.","Extra gateway dependency and troubleshooting complexity.","Not a magic bypass or anonymity layer; weak gateway/TLS exposes access.","RFC 4251, RFC 6455"),
Guide("HTTP","Application","Web transfer protocol without transport encryption by itself.","Use HTTPS for accounts and sensitive data; plain HTTP is for controlled labs or redirects.","Simple and ubiquitous.","No confidentiality or integrity without a secure layer.","Credentials and content may be observed or modified.","RFC 9110"),
Guide("HTTPS / TLS","Application","HTTP protected by TLS; HTTP/3 uses QUIC with TLS 1.3.","Use valid certificates, hostname verification and maintained TLS settings.","Confidentiality, integrity and server authentication.","Certificate lifecycle and configuration add work.","Expired/misissued certificates or disabled verification undermine protection.","RFC 8446, RFC 9114"),
Guide("DNS over HTTPS (DoH)","DNS privacy","Carries DNS messages inside HTTPS requests to a selected resolver.","Choose a resolver with a privacy policy you accept; use DNSSEC validation separately.","Encrypts transport between client and resolver.","Resolver still sees queries; DoH alone does not authenticate DNS data.","Resolver logging and metadata remain; encrypted does not mean anonymous.","RFC 8484, RFC 9076"),
Guide("DNSSEC","DNS security","Adds origin authentication and integrity validation to DNS data.","Use a validating resolver; DS/DNSKEY/RRSIG records alone do not prove successful validation.","Helps detect forged or modified DNS data.","Signing and key rollover add complexity; queries are not encrypted.","Broken trust chains can cause lookup failures; DNSSEC does not hide names.","RFC 9364, RFC 4033-4035"),
Guide("Trojan Proxy","Proxy","TLS-based proxy protocol with authenticated requests; distinct from Trojan horse malware.","Use maintained compatible software only on networks where proxy use is permitted; validate TLS certificates.","Defined authenticated request format over TLS.","Project-specific compatibility; not a general VPN.","TLS-like appearance is not invisibility or anonymity; endpoint can observe metadata and unencrypted traffic.","Trojan-GFW protocol documentation"),
Guide("VLESS","Proxy","Lightweight proxy protocol in Project X ecosystem.","Use a maintained implementation and separately protected transport; verify endpoint identity.","Lightweight and flexible transport choices.","Security depends on transport and implementation.","Do not assume VLESS alone encrypts traffic.","Project X VLESS docs"),
Guide("VMess","Proxy","Proxy protocol used in V2Ray / Project X ecosystem.","Follow exact implementation/version guidance and keep system time accurate.","Established ecosystem and multiple implementations.","Compatibility varies by version.","Outdated clients and configuration errors weaken reliability/security.","Project X VMess docs"),
Guide("Shadowsocks","Proxy","Encrypted proxy protocol for TCP and UDP relay.","Use maintained implementations, unique credentials and approved endpoints.","Simple encrypted proxy design.","Not a full routed VPN; protection depends on configuration.","Credential reuse or untrusted server exposes metadata and potentially traffic beyond the encrypted hop.","Shadowsocks protocol specification"),
Guide("ShadowsocksR (SSR)","Proxy","Shadowsocks-derived legacy protocol family.","Check maintenance and compatibility; do not treat obfuscation as encryption.","Some legacy clients support extra options.","Fragmented support and maintenance.","Legacy software and weak cryptography create risk; obfuscation is not privacy.","Implementation-specific SSR docs"),
Guide("TUIC","Proxy","Proxy protocol for TCP/UDP relay commonly using QUIC.","Use compatible maintained clients and follow project authentication guidance.","Multiplexing and QUIC transport.","Project-specific compatibility and network dependency.","Endpoint sees metadata; weak credentials or exposed servers create risk.","TUIC project specification"),
Guide("Hysteria","Proxy","QUIC-based proxy family supporting TCP and UDP relay.","Follow the exact major-version documentation and use authorized endpoints.","QUIC transport and TCP/UDP relay.","Behavior varies by version and network conditions.","Do not equate congestion/obfuscation features with anonymity.","Hysteria protocol docs"),
Guide("QUIC","Transport","Secure multiplexed transport over UDP integrating TLS 1.3.","Applications such as HTTP/3 negotiate it; permit UDP only according to policy.","Stream multiplexing, connection migration and reduced cross-stream blocking.","UDP filtering and middleboxes can affect connectivity.","Outdated implementations or incorrect TLS validation undermine security.","RFC 9000, RFC 9001"),
Guide("WireGuard","VPN","VPN protocol carrying IP packets through authenticated encrypted UDP tunnels.","Administrators define peers, public keys and allowed IP prefixes; protect private keys.","Compact design and modern cryptography.","Peer/key distribution may be manual; UDP can be blocked.","Leaked keys or overly broad routes expose networks; VPN endpoint still must be trusted.","WireGuard official protocol docs"),
Guide("OpenVPN","VPN","VPN software using TLS-based control-channel security and encrypted data channels.","Use supported versions, certificates and administrator-provided configuration.","Mature TCP/UDP deployment options.","Can require more configuration and resources.","Weak certificates, exposed management or obsolete ciphers compromise deployment.","OpenVPN official docs"),
Guide("IPsec","VPN","Suite providing security services at IP layer, commonly negotiated with IKE.","Configure peer identity, security policies and permitted networks carefully.","Standardized network-layer protection.","Policy negotiation and NAT traversal can be complex.","Weak credentials or permissive policies expose traffic or disrupt connectivity.","RFC 4301, RFC 7296"),
Guide("L2TP","Tunneling","Layer 2 tunneling protocol carrying PPP frames.","Use an appropriate security layer such as IPsec when confidentiality/integrity are required.","Carries PPP sessions over IP networks.","L2TP alone does not strongly encrypt traffic.","Using it without security exposes traffic; legacy deployments have compatibility risks.","RFC 2661, RFC 3193"),
Guide("NTP","Time","Synchronizes clocks across networked systems.","Configure trusted time sources and authenticated modes where supported.","Improves logs, certificates and distributed system behavior.","Accuracy depends on source and network conditions.","Bad time sources disrupt logs, authentication and scheduling.","RFC 5905"),
Guide("SMTP","Email","Transfers email between mail systems and submission clients.","Use authenticated submission with TLS; mail routing uses MX records.","Interoperable and widely deployed.","Spam reputation and delivery policy are complex.","Open relays and weak accounts enable abuse; TLS is not end-to-end encryption.","RFC 5321, RFC 6409"),
Guide("IMAP","Email","Synchronizes mailbox folders and messages on a server.","Use secure IMAPS/STARTTLS and provider authentication.","Multi-device mailbox synchronization.","Needs server availability and storage.","Plaintext sessions or stolen credentials expose mail.","RFC 9051"),
Guide("POP3","Email","Retrieves messages, often with download-oriented behavior.","Use POP3S/STARTTLS and check server-retention settings.","Simple retrieval model.","Less suited to multi-device sync than IMAP.","Plaintext access and weak passwords expose mail.","RFC 1939"),
Guide("SMB","File services","Network file and printer sharing, common in Windows networks.","Enable only on trusted networks, use current versions and restrict share permissions.","Integrated sharing and access controls.","Permissions and interoperability need careful management.","Internet exposure and outdated SMB versions increase attack surface.","Microsoft SMB documentation"),
Guide("RDP","Remote desktop","Interactive remote desktop access to supported systems.","Use secured gateway/VPN, network-level authentication and restricted admin access.","Full graphical remote administration.","Bandwidth and access management overhead.","Internet exposure, weak credentials or unpatched services can compromise systems.","Microsoft RDP documentation")
)

@Composable
fun DnsProtocolsScreen(modifier: Modifier = Modifier, viewModel: DnsProtocolsViewModel = viewModel()) {
    val context = LocalContext.current
    val preferences = remember(context) { AppPreferencesRepository(context) }
    val defaultType by preferences.defaultDnsRecordType.collectAsState(initial = "A")
    var domain by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("A") }
    var resolver by remember { mutableStateOf(DnsResolver.CLOUDFLARE) }
    var section by remember { mutableIntStateOf(0) }
    var expanded by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(defaultType) { if (dnsTypes.contains(defaultType)) selectedType = defaultType }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("DNS & Protocols", style = MaterialTheme.typography.headlineMedium)
        Text("Live DNS record inspection and practical protocol guidance.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(section == 0, { section = 0 }, label = { Text("DNS Lookup") })
            FilterChip(section == 1, { section = 1 }, label = { Text("Protocol Library") })
            FilterChip(section == 2, { section = 2 }, label = { Text("Protocol Center") })
            FilterChip(section == 3, { section = 3 }, label = { Text("Application Protocols") })
        }
        if (section == 0) {
            OutlinedTextField(domain, { domain = it }, Modifier.fillMaxWidth(), label = { Text("Domain name") }, placeholder = { Text("example.com") }, singleLine = true)
            Text("Record type", style = MaterialTheme.typography.titleSmall)
            dnsTypes.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    row.forEach { type -> FilterChip(selectedType == type, { selectedType = type }, label = { Text(type) }) }
                }
            }
            Text("Public resolver", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DnsResolver.values().forEach { item -> FilterChip(resolver == item, { resolver = item }, label = { Text(item.label) }) }
            }
            Button(onClick = { viewModel.lookup(domain, selectedType, resolver) }, enabled = domain.isNotBlank() && !viewModel.isLoading, modifier = Modifier.fillMaxWidth()) { Text("Resolve DNS") }
            if (viewModel.isLoading) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { CircularProgressIndicator(); Text("Querying ${resolver.label} over HTTPS…") }
            viewModel.error?.let { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Text(it, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer) } }
            viewModel.result?.let { result ->
                Card {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${result.domain} · ${result.recordType}", style = MaterialTheme.typography.titleMedium)
                        Text("Resolver: ${result.provider} · ${result.elapsedMs} ms", style = MaterialTheme.typography.bodySmall)
                        Text(when(result.status) {
                            0 -> if(result.answers.isEmpty()) "NOERROR · No answer records." else "NOERROR · ${result.answers.size} answer(s)."
                            1 -> "FORMERR · Malformed query."
                            2 -> "SERVFAIL · Resolver failed."
                            3 -> "NXDOMAIN · Name does not exist."
                            4 -> "NOTIMP · Operation not implemented."
                            5 -> "REFUSED · Resolver refused the query."
                            else -> "DNS status: ${result.status}"
                        })
                        Text(if(result.dnssecAuthenticated) "DNSSEC: resolver reports AD (Authenticated Data)." else "DNSSEC: resolver did not assert AD; this does not prove the domain is unsigned.", style = MaterialTheme.typography.bodySmall)
                        if(result.checkingDisabled) Text("CD flag: checking disabled by query.", style = MaterialTheme.typography.bodySmall)
                        DnsAnswerSection("Answer", result.answers)
                        DnsAnswerSection("Authority", result.authority)
                        DnsAnswerSection("Additional", result.additional)
                    }
                }
            }
            Text("Privacy: the selected resolver receives the domain and record type. DoH encrypts transport to that resolver but does not make queries anonymous. DNSSEC validation is separate. FORGE-TS does not save lookup history.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (section == 1) {
            Text("Select a protocol to read its purpose, usage, pros, cons, risks and references. This is educational guidance; the app does not establish tunnels or run protocol clients.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            guides.forEach { guide ->
                val open = expanded == guide.name
                Card(onClick = { expanded = if(open) null else guide.name }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(guide.group.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(guide.name, style = MaterialTheme.typography.titleMedium)
                        Text(guide.overview, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if(open) {
                            Detail("Cara menggunakan", guide.how)
                            Detail("Kelebihan", guide.pros)
                            Detail("Kekurangan", guide.cons)
                            Detail("Risiko", guide.risks)
                            Detail("Referensi", guide.refs)
                        } else Text("Tap untuk membuka detail", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        } else if (section == 2) {
            ProtocolCenterScreen()
        } else {
            ApplicationProtocolCenterScreen()
        }
    }
}

@Composable
private fun DnsAnswerSection(title:String, answers:List<DnsAnswer>) {
    if(answers.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleSmall)
    answers.forEach { answer ->
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(answer.data, style = MaterialTheme.typography.bodyLarge)
                Text("${answer.type} · TTL ${answer.ttl}s · ${answer.name}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun Detail(label:String, value:String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
