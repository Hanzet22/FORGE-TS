package com.HG.Forge.Ts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun OsintWebScreen(
    modifier: Modifier = Modifier,
    viewModel: OsintWebViewModel = viewModel()
) {
    var domain by remember { mutableStateOf("") }
    var includeCarbon by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("OSINT Web", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Inspect passive DNS, DNSSEC resolver signals, CAA policy, RDAP domain metadata, HTTP/TLS, HSTS, cookies, robots.txt, sitemaps, page metadata and technology indicators using native Android networking.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = domain,
            onValueChange = { domain = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Target domain") },
            placeholder = { Text("example.com") },
            singleLine = true
        )
        Button(
            onClick = { viewModel.inspect(domain, includeCarbon) },
            enabled = domain.isNotBlank() && !viewModel.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Run native inspection")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = includeCarbon, onCheckedChange = { includeCarbon = it })
            Column(modifier = Modifier.weight(1f)) {
                Text("Optional Website Carbon estimate", style = MaterialTheme.typography.bodyMedium)
                Text("When enabled, the page URL is sent to Website Carbon, a third-party estimator. Off by default.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (viewModel.isLoading) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator()
                Text("Collecting public DNS records…")
            }
        }

        viewModel.error?.let { message ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    message,
                    modifier = Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        if (viewModel.checks.isNotEmpty()) {
            Text("Domain report: ${viewModel.target}", style = MaterialTheme.typography.titleLarge)
            Text(
                "${viewModel.checks.count { it.result != null }} of ${viewModel.checks.size} DNS checks completed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            viewModel.checks.forEach { check ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(check.recordType, style = MaterialTheme.typography.titleMedium)
                        Text(check.queryName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (check.result != null) {
                            val result = check.result
                            Text(
                                when (result.status) {
                                    0 -> if (result.answers.isEmpty()) "NOERROR · No answer records" else "NOERROR · ${result.answers.size} answer(s)"
                                    2 -> "SERVFAIL · Resolver failed"
                                    3 -> "NXDOMAIN · Name does not exist"
                                    5 -> "REFUSED · Resolver refused query"
                                    else -> "DNS status: ${result.status}"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Google Public DNS · ${result.elapsedMs} ms · DNSSEC AD: ${result.dnssecAuthenticated}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (result.answers.isEmpty()) {
                                Text("No answer records returned.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                result.answers.forEach { answer ->
                                    Text(answer.data, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "TTL ${answer.ttl}s",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Text(
                                check.error ?: "Lookup unavailable",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        if (viewModel.dnsSecurityFindings.isNotEmpty()) {
            Text("DNS Security Intelligence", style = MaterialTheme.typography.headlineSmall)
            Text("Resolver-reported DNSSEC authentication and passive CAA policy interpretation. These are evidence signals, not a full independent DNSSEC chain validator.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            viewModel.dnsSecurityFindings.forEach { finding ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(finding.title, style = MaterialTheme.typography.titleMedium)
                        Text(finding.status, style = MaterialTheme.typography.bodyMedium)
                        finding.details.forEach { detail -> Text(detail, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
        if (viewModel.mailSecurityFindings.isNotEmpty()) {
            Text("Mail Security Intelligence", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Passive DNS checks for mail-routing and domain-level email security. DKIM checks use common selector guesses only.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            viewModel.mailSecurityFindings.forEach { finding ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(finding.title, style = MaterialTheme.typography.titleMedium)
                        Text(finding.status, style = MaterialTheme.typography.bodyMedium)
                        finding.details.forEach { detail ->
                            Text(detail, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Text("RDAP Domain Intelligence", style = MaterialTheme.typography.headlineSmall)
        viewModel.rdapError?.let { message ->
            Text("RDAP unavailable: $message", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        viewModel.rdapReport?.let { report ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(report.domain, style = MaterialTheme.typography.titleMedium)
                    Text("Registrar: ${report.registrar ?: "Not disclosed"}")
                    report.handle?.let { Text("RDAP handle: $it", style = MaterialTheme.typography.bodySmall) }
                    Text("Domain statuses: ${report.statuses.joinToString().ifBlank { "Not returned" }}", style = MaterialTheme.typography.bodySmall)
                    Text("Registration events:", style = MaterialTheme.typography.labelLarge)
                    report.events.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    Text("Nameservers: ${report.nameServers.joinToString().ifBlank { "Not returned" }}", style = MaterialTheme.typography.bodySmall)
                    Text("Source: ${report.source} · Public RDAP metadata only; registrant contact/vCard data is not collected.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Text("IP & ASN Network Intelligence", style = MaterialTheme.typography.headlineSmall)
        viewModel.ipNetworkError?.let { message ->
            Text("Network attribution unavailable: $message", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        viewModel.ipNetworkReport?.let { report ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(report.address, style = MaterialTheme.typography.titleMedium)
                    Text("IP version: ${report.ipVersion ?: "Not reported"}")
                    Text("Network: ${report.networkName ?: "Not reported"}")
                    Text("Handle: ${report.handle ?: "Not reported"}", style = MaterialTheme.typography.bodySmall)
                    Text("Range: ${report.startAddress ?: "?"} – ${report.endAddress ?: "?"}", style = MaterialTheme.typography.bodySmall)
                    Text("Country/region code: ${report.country ?: "Not reported"}", style = MaterialTheme.typography.bodySmall)
                    Text("Registry source: ${report.source}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("ASN is not guaranteed by every IP RDAP response. Network attribution is registry metadata, not proof of physical server location or ownership.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        viewModel.nativeError?.let { message ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text("Website inspection: $message", modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }

        if (includeCarbon) {
            Text("Website Carbon Intelligence", style = MaterialTheme.typography.headlineSmall)
            viewModel.carbonError?.let { Text("Estimate unavailable: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            viewModel.carbonReport?.let { report ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(report.rating ?: "Provider rating unavailable", style = MaterialTheme.typography.titleMedium)
                        report.gridCo2Grams?.let { Text("Estimated grid CO₂ per page view: ${"%.4f".format(it)} g") }
                        report.renewableCo2Grams?.let { Text("Estimated renewable-hosting CO₂: ${"%.4f".format(it)} g") }
                        report.bytes?.let { Text("Provider-measured transfer: $it bytes") }
                        report.cleanerThanPercent?.let { Text("Cleaner than: ${"%.1f".format(it)}% of tested pages") }
                        Text("Green hosting signal: ${report.greenHosting?.toString() ?: "Not reported"}")
                        Text("Provider: ${report.provider}. Estimates depend on the provider's model and assumptions; they are not direct energy measurements.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        viewModel.nativeReport?.let { report ->
            Text("Native Web-Check report", style = MaterialTheme.typography.headlineSmall)
            Text("Final URL: ${report.finalUrl}\nChecked: ${report.checkedAt}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            report.findings.forEach { finding ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(finding.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(finding.title, style = MaterialTheme.typography.titleMedium)
                        Text(finding.status, style = MaterialTheme.typography.bodyMedium)
                        finding.details.forEach { detail -> Text(detail, style = MaterialTheme.typography.bodySmall) }
                        Text("Source: ${finding.source}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Text(
            "Scope: passive public metadata only. DNS queries are sent to Google Public DNS; website requests go directly to the selected public domain. Local/private/link-local destinations are blocked, redirects are manually limited, response bodies are capped, and HTTPS downgrade redirects are rejected. FORGE-TS does not persist reports. Inspect only domains you own or are authorized to assess.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Reference project: Web-Check by Alicia Sykes (MIT). This is an independently implemented native Kotlin/Compose adaptation; it does not bundle the upstream web application or copy its runtime.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
