package com.HG.Forge.Ts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private val dnsTypes = listOf("A", "AAAA", "CNAME", "MX", "TXT", "NS", "SRV", "CAA")

private data class ProtocolEntry(
    val name: String,
    val port: String,
    val transport: String,
    val purpose: String
)

private val protocolEntries = listOf(
    ProtocolEntry("DNS", "53", "UDP / TCP", "Translates domain names into network records."),
    ProtocolEntry("DHCP", "67 / 68", "UDP", "Assigns network configuration to clients."),
    ProtocolEntry("HTTP", "80", "TCP", "Web traffic without transport encryption."),
    ProtocolEntry("HTTPS", "443", "TCP", "HTTP protected by TLS; HTTP/3 uses QUIC over UDP."),
    ProtocolEntry("SSH", "22", "TCP", "Encrypted remote administration and file transfer."),
    ProtocolEntry("NTP", "123", "UDP", "Synchronizes system clocks."),
    ProtocolEntry("SMTP", "25", "TCP", "Mail transfer between mail servers."),
    ProtocolEntry("IMAP", "143", "TCP", "Accesses and synchronizes mailbox contents."),
    ProtocolEntry("POP3", "110", "TCP", "Retrieves email from a mail server."),
    ProtocolEntry("SMB", "445", "TCP", "File and printer sharing in Windows networks."),
    ProtocolEntry("RDP", "3389", "TCP / UDP", "Remote desktop transport."),
    ProtocolEntry("WireGuard", "51820*", "UDP", "Common default port; deployments can configure another port.")
)

@Composable
fun DnsProtocolsScreen(
    modifier: Modifier = Modifier,
    viewModel: DnsProtocolsViewModel = viewModel()
) {
    var domain by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("A") }
    var selectedSection by remember { mutableStateOf(0) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("DNS & Protocols", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Resolve common DNS records and browse an offline protocol reference.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selectedSection == 0, onClick = { selectedSection = 0 }, label = { Text("DNS Lookup") })
            FilterChip(selected = selectedSection == 1, onClick = { selectedSection = 1 }, label = { Text("Protocols") })
        }

        if (selectedSection == 0) {
            OutlinedTextField(
                value = domain,
                onValueChange = { domain = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Domain name") },
                placeholder = { Text("example.com") },
                singleLine = true
            )
            Text("Record type", style = MaterialTheme.typography.titleSmall)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                dnsTypes.chunked(4).forEach { group ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        group.forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type) }
                            )
                        }
                    }
                }
            }
            Button(
                onClick = { viewModel.lookup(domain, selectedType) },
                enabled = domain.isNotBlank() && !viewModel.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Resolve DNS") }

            if (viewModel.isLoading) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator()
                    Text("Querying DNS over HTTPS…")
                }
            }
            viewModel.error?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(it, modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            viewModel.result?.let { result ->
                Card {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${result.domain} · ${result.recordType}", style = MaterialTheme.typography.titleMedium)
                        Text("Provider: ${result.provider}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            when (result.status) {
                                0 -> if (result.answers.isEmpty()) "No records returned for this query." else "${result.answers.size} answer(s) returned."
                                3 -> "NXDOMAIN: the queried name does not exist."
                                else -> "DNS response status: ${result.status}"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        result.answers.forEach { answer ->
                            Spacer(Modifier.height(4.dp))
                            Text(answer.data, style = MaterialTheme.typography.bodyLarge)
                            Text("${answer.type} · TTL ${answer.ttl}s · ${answer.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Text(
                "Privacy: the domain and record type are sent to Cloudflare DNS over HTTPS. The lookup is not stored by FORGE-TS.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            protocolEntries.forEach { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(entry.name, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(4.dp))
                            Text("${entry.port} · ${entry.transport}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(entry.purpose, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Text(
                "*WireGuard commonly uses UDP 51820 by default, but its port is configurable. Port references are educational and do not indicate that a service is active.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
