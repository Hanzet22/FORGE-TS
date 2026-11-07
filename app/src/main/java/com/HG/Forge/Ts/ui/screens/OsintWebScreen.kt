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

@Composable
fun OsintWebScreen(
    modifier: Modifier = Modifier,
    viewModel: OsintWebViewModel = viewModel()
) {
    var domain by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("OSINT Web", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Collect public domain intelligence using passive DNS-over-HTTPS lookups. This first integration establishes the native OSINT workflow; additional Web-Check capabilities will be ported in separate modules.",
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
            onClick = { viewModel.inspect(domain) },
            enabled = domain.isNotBlank() && !viewModel.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Inspect domain")
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

        Text(
            "Scope: passive public DNS metadata only. Queries are sent to Google Public DNS; DNS records can reveal infrastructure details and may be logged by the resolver. DNSSEC AD is resolver-reported metadata, not a complete independent validation report. FORGE-TS does not save this report. Inspect only domains you own or are authorized to assess.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Reference project: Web-Check by Alicia Sykes (MIT). This initial native implementation reuses FORGE-TS DNS infrastructure and does not bundle the Web-Check web application.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
