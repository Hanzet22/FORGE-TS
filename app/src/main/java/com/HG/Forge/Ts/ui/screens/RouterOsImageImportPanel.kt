package com.HG.Forge.Ts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AssistChip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class ChrVersionEntry(
    val version: String,
    val channel: String,
    val releaseDate: String,
    val description: String
)

private val chrVersions = listOf(
    ChrVersionEntry("7.25.1", "Stable", "2026-11-03", "Latest stable release"),
    ChrVersionEntry("7.24.6", "Long-term", "2026-11-02", "Current long-term release"),
    ChrVersionEntry("7.26beta5", "Development", "2026-11-02", "Development preview"),
    ChrVersionEntry("7.25rc3", "Testing", "2026-10-21", "Release candidate"),
    ChrVersionEntry("6.49.23", "Legacy", "2026-10-09", "RouterOS 6 long-term line")
)

@Composable
fun RouterOsImageImportPanel() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("MikroTik RouterOS CHR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Version catalog • CHR runtime and image integration are not released yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            chrVersions.forEach { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text("RouterOS ${entry.version}", fontWeight = FontWeight.SemiBold)
                            Text("${entry.channel} • ${entry.releaseDate}", style = MaterialTheme.typography.labelSmall)
                            Text(entry.description, style = MaterialTheme.typography.bodySmall)
                        }
                        AssistChip(onClick = {}, enabled = false, label = { Text("Coming Soon") })
                    }
                }
            }
            Text(
                "Version references follow MikroTik's published changelog. Every entry is informational only; download, import, installation and runtime controls are not available.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
