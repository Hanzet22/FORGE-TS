package com.HG.Forge.Ts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.HG.Forge.Ts.data.simulation.LinuxUserlandFoundationReport
import com.HG.Forge.Ts.data.simulation.LinuxUserlandFoundationSupervisor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LinuxUserlandFoundationPanel() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val supervisor = remember(context) { LinuxUserlandFoundationSupervisor(context) }
    var report by remember { mutableStateOf<LinuxUserlandFoundationReport?>(null) }
    var preparing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Linux Userland • Phase 8 Foundation", style = MaterialTheme.typography.titleMedium)
            Text(
                "Prepares private workspace folders for a future XFCE4 environment. This step does not install or launch Linux.",
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                onClick = {
                    preparing = true
                    error = null
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            runCatching { supervisor.prepare() }
                        }
                        result.onSuccess { report = it }
                            .onFailure { error = it.message ?: "Linux workspace preparation failed." }
                        preparing = false
                    }
                },
                enabled = !preparing
            ) {
                Text(if (preparing) "Preparing…" else "Prepare Linux workspace")
            }
            error?.let { Text("Preparation error: $it", color = MaterialTheme.colorScheme.error) }
            report?.let { current ->
                Text("Workspace status: ${current.status}", style = MaterialTheme.typography.titleSmall)
                Text("Private root: ${current.rootPath}", style = MaterialTheme.typography.bodySmall)
                Text(
                    "Directories: rootfs=${current.rootfsDirectoryReady}, home=${current.homeDirectoryReady}, tmp=${current.temporaryDirectoryReady}, logs=${current.logDirectoryReady}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Available app storage: ${current.availableStorageBytes / 1_073_741_824} GiB",
                    style = MaterialTheme.typography.bodySmall
                )
                current.notes.forEach { note ->
                    Text("• $note", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
