package com.HG.Forge.Ts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class PrivacyProvider(val name: String, val data: String, val purpose: String)

private val providers = listOf(
    PrivacyProvider("Google Public DNS", "Queried domain and DNS record type", "DNS-over-HTTPS lookups"),
    PrivacyProvider("GitHub", "Repository search terms and normal API request metadata", "Public repository search"),
    PrivacyProvider("api.ipify.org", "The connection's public IP address", "What Is My IP lookup"),
    PrivacyProvider("User-selected host", "Hostname or IP and selected port", "Single-host connectivity check")
)

@Composable
fun SecurityPrivacyScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Security & Privacy", style = MaterialTheme.typography.headlineMedium)
        Text(
            "A transparent overview of what FORGE-TS sends, what it keeps local and the boundaries of its tools.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text("External service disclosures", style = MaterialTheme.typography.titleMedium)
        providers.forEach { provider ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(provider.name, style = MaterialTheme.typography.titleSmall)
                    Text("Data: ${provider.data}", style = MaterialTheme.typography.bodyMedium)
                    Text("Purpose: ${provider.purpose}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text("Local-first boundaries", style = MaterialTheme.typography.titleMedium)
        listOf(
            "CIDR calculations run locally on the device.",
            "Knowledge Center and protocol reference content are bundled and searchable offline.",
            "DNS query history is not stored by FORGE-TS.",
            "Repository Explorer reads public GitHub data only and does not ask for a personal access token.",
            "Host Checker performs a single selected-host check. Use it only on systems you own or are authorized to inspect.",
            "OS command examples are displayed as reference text; the app does not execute them.",
            "No Supabase secret/service-role key or provider secret may be embedded in the APK."
        ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        Text(
            "Network providers may process requests under their own privacy policies and retention practices. FORGE-TS does not control those external services.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
