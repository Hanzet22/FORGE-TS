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
import com.HG.Forge.Ts.data.simulation.ProotDistribution
import com.HG.Forge.Ts.data.simulation.RuntimeEnvironmentId
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
    var installing by remember { mutableStateOf<ProotDistribution?>(null) }
    var commandOutput by remember { mutableStateOf("") }

    LaunchedEffect(manager) {
        val result = manager.initialize()
        message = result.message
        snapshot = manager.inspect()
        managerEvent = manager.lastManagerEvent()
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Hybrid Runtime Manager", style = MaterialTheme.typography.titleLarge)
            Text("Persistent registry, conservative process recovery, and five isolated workspaces. Recovery never auto-starts an environment.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            snapshot.environments.forEach { environment ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(environment.title, style = MaterialTheme.typography.titleMedium)
                        Text("Lifecycle: ${environment.lifecycle}", style = MaterialTheme.typography.labelMedium)
                        Text(if (environment.workspaceReady) "Workspace: prepared and verified" else "Workspace: not prepared / not verified", style = MaterialTheme.typography.bodySmall)
                        Text(environment.notes.joinToString(" "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                scope.launch {
                                    val result = manager.prepareWorkspace(environment.id)
                                    message = result.message; snapshot = manager.inspect(); managerEvent = manager.lastManagerEvent()
                                }
                            }) { Text(if (environment.workspaceReady) "Refresh workspace" else "Prepare workspace") }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    val result = manager.requestStart(environment.id)
                                    message = result.message; snapshot = manager.inspect()
                                }
                            }, enabled = environment.id != RuntimeEnvironmentId.XFCE_PROOT &&
                                environment.lifecycle != RuntimeLifecycleState.UNAVAILABLE &&
                                environment.lifecycle != RuntimeLifecycleState.RECOVERY_REQUIRED) { Text("Start") }
                        }
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PRoot distro instances", style = MaterialTheme.typography.titleMedium)
                    Text("Pinned ARM64 rootfs archives for Debian, Ubuntu, and Alpine are installed into each distro's existing private workspace. Runtime availability still depends on the device PRoot probe.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    snapshot.prootInstances.forEach { instance ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(instance.distribution.name, style = MaterialTheme.typography.titleSmall)
                            Text("Lifecycle: ${instance.lifecycle}", style = MaterialTheme.typography.bodySmall)
                            Text(if (instance.workspaceReady) "Workspace prepared" else "Workspace not prepared", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        val result = manager.prepareProotInstance(instance.distribution)
                                        message = result.message; snapshot = manager.inspect(); managerEvent = manager.lastManagerEvent()
                                    }
                                }) { Text(if (instance.workspaceReady) "Refresh" else "Prepare") }
                                Button(
                                    enabled = installing == null &&
                                        instance.lifecycle != RuntimeLifecycleState.RUNNING &&
                                        instance.lifecycle != RuntimeLifecycleState.STARTING &&
                                        instance.lifecycle != RuntimeLifecycleState.RECOVERY_REQUIRED,
                                    onClick = {
                                        installing = instance.distribution
                                        scope.launch {
                                            try {
                                                val result = manager.installProotRootfs(instance.distribution)
                                                message = result.message
                                            } finally {
                                                installing = null
                                                snapshot = manager.inspect()
                                                managerEvent = manager.lastManagerEvent()
                                            }
                                        }
                                    }
                                ) { Text(if (installing == instance.distribution) "Installing…" else "Install rootfs") }
                                OutlinedButton(
                                    enabled = instance.lifecycle == RuntimeLifecycleState.READY ||
                                        instance.lifecycle == RuntimeLifecycleState.STOPPED ||
                                        instance.lifecycle == RuntimeLifecycleState.FAILED,
                                    onClick = {
                                        scope.launch {
                                            val result = manager.requestStart(RuntimeEnvironmentId.XFCE_PROOT, instance.distribution)
                                            message = result.message; snapshot = manager.inspect()
                                        }
                                    }
                                ) { Text("Start") }
                                OutlinedButton(
                                    enabled = instance.lifecycle == RuntimeLifecycleState.RUNNING,
                                    onClick = {
                                        scope.launch {
                                            val result = manager.requestStop(RuntimeEnvironmentId.XFCE_PROOT, instance.distribution)
                                            message = result.message; snapshot = manager.inspect()
                                            managerEvent = manager.lastManagerEvent()
                                        }
                                    }
                                ) { Text("Stop") }
                                OutlinedButton(
                                    enabled = instance.lifecycle == RuntimeLifecycleState.RUNNING,
                                    onClick = {
                                        scope.launch {
                                            val result = manager.executeProotCommand(
                                                instance.distribution,
                                                listOf("/bin/sh", "-lc", "id; uname -a; cat /etc/os-release")
                                            )
                                            commandOutput = result.output.ifBlank { result.message }
                                            message = result.message
                                            snapshot = manager.inspect()
                                        }
                                    }
                                ) { Text("Diagnostics") }
                            }
                        }
                    }
                    if (commandOutput.isNotBlank()) {
                        Text("Guest command output", style = MaterialTheme.typography.titleSmall)
                        Text(commandOutput, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            ResourceSummary(snapshot)
            snapshot.foundation?.let { foundation ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Runtime capability gate", style = MaterialTheme.typography.titleSmall)
                        Text("PRoot ptrace: ${foundation.capabilities.prootPtraceProbe.status}", style = MaterialTheme.typography.bodySmall)
                        Text("KVM API: ${foundation.capabilities.kvmApiProbe.status} • VM creation: ${foundation.capabilities.kvmVmCreationProbe.status}", style = MaterialTheme.typography.bodySmall)
                        foundation.capabilities.executionAdapters.forEach { (environment, observation) ->
                            Text("$environment adapter: ${observation.status}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("Unknown means not probed; it is not treated as available.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Manager event", style = MaterialTheme.typography.titleSmall)
                    Text(message, style = MaterialTheme.typography.bodySmall)
                    Text(managerEvent, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedButton(onClick = {
                scope.launch {
                    snapshot = manager.inspect(); managerEvent = manager.lastManagerEvent()
                    message = "Device resources and persistent registry refreshed."
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Refresh manager status") }
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
    if (value < 1024L) return "$value B"
    val units = listOf("KB", "MB", "GB", "TB")
    var amount = value.toDouble()
    var index = -1
    do { amount /= 1024.0; index++ } while (amount >= 1024.0 && index < units.lastIndex)
    return String.format(java.util.Locale.US, "%.1f %s", amount, units[index])
}
