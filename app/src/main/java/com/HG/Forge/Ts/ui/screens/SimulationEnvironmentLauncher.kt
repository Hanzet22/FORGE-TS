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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.HG.Forge.Ts.data.simulation.HybridRuntimeManager
import com.HG.Forge.Ts.data.simulation.HybridRuntimeSnapshot
import com.HG.Forge.Ts.data.simulation.RuntimeLifecycleState
import kotlinx.coroutines.launch

@Composable
fun SimulationEnvironmentLauncher() {
    val context = LocalContext.current
    val manager = remember(context) { HybridRuntimeManager(context) }
    val scope = rememberCoroutineScope()
    var snapshot by remember(manager) { mutableStateOf(manager.inspect()) }
    var message by remember { mutableStateOf("Loading persistent runtime registry…") }
    var managerEvent by remember { mutableStateOf("Reading local event record…") }

    LaunchedEffect(manager) {
        val result = manager.initialize()
        message = result.message
        snapshot = manager.inspect()
        managerEvent = manager.lastManagerEvent()
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Hybrid Runtime Manager", style = MaterialTheme.typography.titleLarge)
            Text(
                "Persistent registry, conservative process recovery, and five isolated workspaces. Recovery never auto-starts an environment.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            snapshot.environments.forEach { environment ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(environment.title, style = MaterialTheme.typography.titleMedium)
                        Text("Lifecycle: ${environment.lifecycle}", style = MaterialTheme.typography.labelMedium)
                        Text(
                            if (environment.workspaceReady) "Workspace: prepared and verified" else "Workspace: not prepared / not verified",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(environment.notes.joinToString(" "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                scope.launch {
                                    val result = manager.prepareWorkspace(environment.id)
                                    message = result.message
                                    snapshot = manager.inspect()
                                    managerEvent = manager.lastManagerEvent()
                                }
                            }) { Text(if (environment.workspaceReady) "Refresh workspace" else "Prepare workspace") }
                            OutlinedButton(
                                onClick = {
                                    message = manager.requestStart(environment.id).message
                                    snapshot = manager.inspect()
                                },
                                enabled = environment.lifecycle != RuntimeLifecycleState.UNAVAILABLE &&
                                    environment.lifecycle != RuntimeLifecycleState.RECOVERY_REQUIRED
                            ) { Text("Start") }
                            if (environment.lifecycle == RuntimeLifecycleState.RECOVERY_REQUIRED) {
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        val result = manager.transitionForFutureAdapter(
                                            environment.id, RuntimeLifecycleState.STOPPED
                                        )
                                        message = if (result.accepted) {
                                            "Previous session marked stopped. Only confirm this after verifying no environment process is still running."
                                        } else result.message
                                        snapshot = manager.inspect()
                                        managerEvent = manager.lastManagerEvent()
                                    }
                                }) { Text("Confirm stopped") }
                            }
                        }
                    }
                }
            }
            ResourceSummary(snapshot)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Manager event", style = MaterialTheme.typography.titleSmall)
                    Text(message, style = MaterialTheme.typography.bodySmall)
                    Text(managerEvent, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        snapshot = manager.inspect()
                        managerEvent = manager.lastManagerEvent()
                        message = "Device resources and persistent registry refreshed."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Refresh manager status") }
        }
    }
}

@Composable
private fun ResourceSummary(snapshot: HybridRuntimeSnapshot) {
    val resources = snapshot.resources
    Card {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Device resource monitor", style = MaterialTheme.typography.titleMedium)
            Text("Available RAM: ${formatBytes(resources.availableMemoryBytes)} / ${formatBytes(resources.totalMemoryBytes)}")
            Text("Available storage: ${formatBytes(resources.availableStorageBytes)} / ${formatBytes(resources.totalStorageBytes)}")
            Text("Available processors: ${resources.availableProcessors}")
            Text("Device-wide readings; not resources reserved for FORGE-TS.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatBytes(value: Long): String {
    if (value < 1024L) return "${value} B"
    val units = listOf("KB", "MB", "GB", "TB")
    var amount = value.toDouble()
    var index = -1
    do { amount /= 1024.0; index++ } while (amount >= 1024.0 && index < units.lastIndex)
    return String.format(java.util.Locale.US, "%.1f %s", amount, units[index])
}
