package com.HG.Forge.Ts.ui.screens

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.HG.Forge.Ts.R
import com.HG.Forge.Ts.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember(context) { AppPreferencesRepository(context) }
    val scope = rememberCoroutineScope()
    val defaultType by repository.defaultDnsRecordType.collectAsState(initial = "A")
    var showAboutApps by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        AccountManagementPanel()
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
        Text("Crash recovery & diagnostics", style = MaterialTheme.typography.titleMedium)
        Text(
            "FORGE-TS automatically uploads technical crash reports and INFO, WARNING, ERROR and DEBUG diagnostic events to the FORGE-TS Supabase backend when an internet connection is available. Uploads are queued and retried after temporary network or server errors. Reports include app/Android versions, an installation identifier, event labels and technical stack traces; they do not intentionally include message contents or passwords. A local copy is retained until the backend confirms receipt. You can also share a local report from the recovery screen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = { showAboutApps = true }) {
            Text("About Apps")
        }
        Text("FORGE-TS 0.1.0", style = MaterialTheme.typography.labelMedium)
    }

    if (showAboutApps) {
        AlertDialog(
            onDismissRequest = { showAboutApps = false },
            title = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(
                        painter = painterResource(R.mipmap.forge_ts_logo),
                        contentDescription = "FORGE-TS app logo",
                        modifier = Modifier.size(44.dp)
                    )
                    Text("About FORGE-TS", style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("FORGE-TS • Version 0.1.0")
                    AndroidView(
                        factory = { viewContext ->
                            VideoView(viewContext).apply {
                                setVideoURI(Uri.parse("android.resource://${viewContext.packageName}/${R.raw.about_apps}"))
                                setOnPreparedListener { player ->
                                    player.isLooping = false
                                    start()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showAboutApps = false }) {
                    Text("Close")
                }
            }
        )
    }
}
