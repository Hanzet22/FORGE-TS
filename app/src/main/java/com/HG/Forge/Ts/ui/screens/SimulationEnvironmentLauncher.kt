package com.HG.Forge.Ts.ui.screens

import com.HG.Forge.Ts.ui.localization.forgeTextFor
import com.HG.Forge.Ts.ui.localization.LocalForgeLanguage
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
    val language = LocalForgeLanguage.current.language
    val manager = remember(context) { HybridRuntimeManager(context) }
    val scope = rememberCoroutineScope()
    var snapshot by remember(manager) { mutableStateOf(manager.inspect()) }
    var message by remember { mutableStateOf("Loading persistent runtime registry…") }
    var managerEvent by remember { mutableStateOf("Reading local event record…") }
    var installing by remember { mutableStateOf<ProotDistribution?>(null) }
    var commandOutput by remember { mutableStateOf("") }
    val refreshedManagerMessage = forgeTextFor("Device resources and persistent registry refreshed.", language)

    LaunchedEffect(manager) {
        val result = manager.initialize()
        message = result.message
        snapshot = manager.inspect()
        managerEvent = manager.lastManagerEvent()
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(forgeTextFor("Hybrid Runtime Manager", language), style = MaterialTheme.typography.titleLarge)
            Text(forgeTextFor("Persistent registry and five isolated workspaces. Recovery never auto-starts an environment.", language),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(forgeTextFor("What works on this build", language), style = MaterialTheme.typography.titleSmall)
                    Text(forgeTextFor("PRoot provides Debian, Ubuntu and Alpine ARM64 rootfs instances. XFCE4 can run through the loopback-only TigerVNC server and the in-app noVNC viewer after their packages are installed.", language), style = MaterialTheme.typography.bodySmall)
                    Text(forgeTextFor("PRoot Terminal can store custom HTTPS mirror profiles for 132 country labels. You provide each URL; this build does not include a curated 90-country endpoint directory. Embedded X11, Ubuntu/Linux native desktop, and VMStation engines are not available in this Android build.", language), style = MaterialTheme.typography.bodySmall)
                }
            }
            snapshot.environments.forEach { environment ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(environment.title, style = MaterialTheme.typography.titleMedium)
                        Text(forgeTextFor("Lifecycle: ${environment.lifecycle}", language), style = MaterialTheme.typography.labelMedium)
                        Text(forgeTextFor(if (environment.workspaceReady) "Workspace: prepared and verified" else "Workspace: not prepared / not verified", language), style = MaterialTheme.typography.bodySmall)
                        Text(environment.notes.joinToString(" "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                scope.launch {
                                    val result = manager.prepareWorkspace(environment.id)
                                    message = result.message; snapshot = manager.inspect(); managerEvent = manager.lastManagerEvent()
                                }
                            }) { Text(forgeTextFor(if (environment.workspaceReady) "Refresh workspace" else "Prepare workspace", language)) }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    val result = manager.requestStart(environment.id)
                                    message = result.message; snapshot = manager.inspect()
                                }
                            }, enabled = environment.id != RuntimeEnvironmentId.XFCE_PROOT &&
                                environment.lifecycle != RuntimeLifecycleState.UNAVAILABLE &&
                                environment.lifecycle != RuntimeLifecycleState.RECOVERY_REQUIRED) { Text(forgeTextFor("Start", language)) }
                        }
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(forgeTextFor("PRoot distro instances", language), style = MaterialTheme.typography.titleMedium)
                    Text(forgeTextFor("Pinned ARM64 rootfs archives for Debian, Ubuntu, and Alpine are installed into each distro's existing private workspace. Runtime availability still depends on the device PRoot probe.", language),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    snapshot.prootInstances.forEach { instance ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(instance.distribution.name, style = MaterialTheme.typography.titleSmall)
                            Text(forgeTextFor("Lifecycle: ${instance.lifecycle}", language), style = MaterialTheme.typography.bodySmall)
                            Text(forgeTextFor(if (instance.workspaceReady) "Workspace prepared" else "Workspace not prepared", language), style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        val result = manager.prepareProotInstance(instance.distribution)
                                        message = result.message; snapshot = manager.inspect(); managerEvent = manager.lastManagerEvent()
                                    }
                                }) { Text(forgeTextFor(if (instance.workspaceReady) "Refresh" else "Prepare", language)) }
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
                                ) { Text(if (installing == instance.distribution) forgeTextFor("Installing…", language) else forgeTextFor("Install rootfs", language)) }
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
                                ) { Text(forgeTextFor("Start", language)) }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    enabled = instance.lifecycle == RuntimeLifecycleState.RUNNING,
                                    onClick = {
                                        scope.launch {
                                            val result = manager.requestStop(RuntimeEnvironmentId.XFCE_PROOT, instance.distribution)
                                            message = result.message; snapshot = manager.inspect()
                                            managerEvent = manager.lastManagerEvent()
                                        }
                                    }
                                ) { Text(forgeTextFor("Stop", language)) }
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
                                ) { Text(forgeTextFor("Diagnostics", language)) }
                            }
                        }
                    }
                    if (commandOutput.isNotBlank()) {
                        Text(forgeTextFor("Guest command output", language), style = MaterialTheme.typography.titleSmall)
                        Text(commandOutput, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            ResourceSummary(snapshot, language)
            snapshot.foundation?.let { foundation ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(forgeTextFor("Runtime capability gate", language), style = MaterialTheme.typography.titleSmall)
                        Text(forgeTextFor("PRoot ptrace: ${foundation.capabilities.prootPtraceProbe.status}", language), style = MaterialTheme.typography.bodySmall)
                        Text(forgeTextFor("KVM API: ${foundation.capabilities.kvmApiProbe.status} • VM creation: ${foundation.capabilities.kvmVmCreationProbe.status}", language), style = MaterialTheme.typography.bodySmall)
                        foundation.capabilities.executionAdapters.forEach { (environment, observation) ->
                            Text(forgeTextFor("$environment adapter: ${observation.status}", language), style = MaterialTheme.typography.bodySmall)
                        }
                        Text(forgeTextFor("Unknown means not probed; it is not treated as available.", language), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(forgeTextFor("Manager event", language), style = MaterialTheme.typography.titleSmall)
                    Text(message, style = MaterialTheme.typography.bodySmall)
                    Text(managerEvent, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedButton(onClick = {
                scope.launch {
                    snapshot = manager.inspect(); managerEvent = manager.lastManagerEvent()
                    message = refreshedManagerMessage
                }
            }, modifier = Modifier.fillMaxWidth()) { Text(forgeTextFor("Refresh manager status", language)) }
        }
    }
}

@Composable
private fun ResourceSummary(snapshot: HybridRuntimeSnapshot, language: com.HG.Forge.Ts.ui.localization.ForgeLanguage) {
    val resources = snapshot.resources
    Card {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(forgeTextFor("Device resource monitor", language), style = MaterialTheme.typography.titleMedium)
            Text(forgeTextFor("Available RAM: ${formatBytes(resources.availableMemoryBytes)} / ${formatBytes(resources.totalMemoryBytes)}", language))
            Text(forgeTextFor("Available storage: ${formatBytes(resources.availableStorageBytes)} / ${formatBytes(resources.totalStorageBytes)}", language))
            Text(forgeTextFor("Available processors: ${resources.availableProcessors}", language))
            Text(forgeTextFor("Device-wide readings; not resources reserved for FORGE-TS.", language), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
