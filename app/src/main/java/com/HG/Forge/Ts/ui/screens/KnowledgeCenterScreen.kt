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
    val example: String
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
    )
)

@Composable
fun KnowledgeCenterScreen(modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    var selectedSection by remember { mutableStateOf("All") }
    var expandedId by remember { mutableStateOf<String?>(null) }
    val sections = listOf("All", "MikroTik", "OS Commands", "SSH & TLS")
    val filtered = knowledgeEntries.filter { entry ->
        (selectedSection == "All" || entry.section == selectedSection) &&
            (query.isBlank() || listOf(entry.title, entry.summary, entry.explanation, entry.example)
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
                    "MikroTik, operating-system, SSH and TLS learning references.",
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
                        Text(entry.explanation, style = MaterialTheme.typography.bodyMedium)
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
                        Text(
                            "Reference only • No commands are executed",
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
