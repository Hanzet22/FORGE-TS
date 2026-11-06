package com.HG.Forge.Ts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.HG.Forge.Ts.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = androidx.compose.runtime.remember(context) { AppPreferencesRepository(context) }
    val scope = rememberCoroutineScope()
    val defaultType by repository.defaultDnsRecordType.collectAsState(initial = "A")

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text("Local preferences are stored on this device.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Default DNS record type", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("A", "AAAA", "MX", "TXT").forEach { type ->
                FilterChip(
                    selected = defaultType == type,
                    onClick = { scope.launch { repository.setDefaultDnsRecordType(type) } },
                    label = { Text(type) }
                )
            }
        }
        Text("The selected record type is saved locally using Android DataStore.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("FORGE-TS 0.1.0", style = MaterialTheme.typography.labelMedium)
    }
}
