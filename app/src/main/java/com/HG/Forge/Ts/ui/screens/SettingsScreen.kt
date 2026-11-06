package com.HG.Forge.Ts.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable
fun SettingsScreen(modifier:Modifier=Modifier) {
 Column(modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Text("Settings",style=MaterialTheme.typography.headlineMedium)
  Text("Preferences, offline data and app information will be configured here.",style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
  Text("FORGE-TS 0.1.0",style=MaterialTheme.typography.labelMedium)
 }
}
