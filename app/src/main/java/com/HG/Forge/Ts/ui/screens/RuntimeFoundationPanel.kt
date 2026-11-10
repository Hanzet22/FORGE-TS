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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.HG.Forge.Ts.data.simulation.RuntimeFoundationReport
import com.HG.Forge.Ts.data.simulation.RuntimeFoundationSupervisor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RuntimeFoundationPanel() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val supervisor = remember(context) { RuntimeFoundationSupervisor(context) }
    var report by remember { mutableStateOf<RuntimeFoundationReport?>(null) }
    var inspecting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Runtime Foundation • Phase 6", style = MaterialTheme.typography.titleMedium)
            Text(
                "Checks Android compatibility and prepares app-private runtime folders. No VM, guest, image download, or network is started.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        inspecting = true
                        error = null
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching { supervisor.inspect() }
                            }
                            result.onSuccess { report = it }
                                .onFailure { error = it.message ?: "Runtime inspection failed." }
                            inspecting = false
                        }
                    },
                    enabled = !inspecting
                ) { Text(if (inspecting) "Inspecting…" else "Inspect device") }
                OutlinedButton(onClick = { report = null; error = null }, enabled = !inspecting) {
                    Text("Reset view")
                }
            }
            error?.let { Text("Inspection error: $it", color = MaterialTheme.colorScheme.error) }
            report?.let { current ->
                Text("Foundation status: ${current.status}", style = MaterialTheme.typography.titleSmall)
                Text("Android API: ${current.apiLevel} • ABI: ${current.supportedAbis.joinToString()}")
                Text("CPU cores: ${current.availableProcessors}")
                Text("RAM available / total: ${current.availableMemoryBytes / 1_048_576} / ${current.totalMemoryBytes / 1_048_576} MiB")
                Text("Storage available / total: ${current.availableStorageBytes / 1_073_741_824} / ${current.totalStorageBytes / 1_073_741_824} GiB")
                Text("/dev/kvm visible: ${current.kvmNodeVisible} • readable flag: ${current.kvmNodeReadable} • writable flag: ${current.kvmNodeWritable}")
                Text("Process architecture: ${current.capabilities.processArchitecture}")
                Text("KVM node: ${current.capabilities.kvmNode.status} — ${current.capabilities.kvmNode.detail}", style = MaterialTheme.typography.bodySmall)
                Text("KVM read: ${current.capabilities.kvmReadPermission.status} • write: ${current.capabilities.kvmWritePermission.status}", style = MaterialTheme.typography.bodySmall)
                Text("KVM API probe: ${current.capabilities.kvmApiProbe.status} — ${current.capabilities.kvmApiProbe.detail}", style = MaterialTheme.typography.bodySmall)
                Text("KVM VM creation: ${current.capabilities.kvmVmCreationProbe.status} — ${current.capabilities.kvmVmCreationProbe.detail}", style = MaterialTheme.typography.bodySmall)
                Text("PRoot ptrace: ${current.capabilities.prootPtraceProbe.status} — ${current.capabilities.prootPtraceProbe.detail}", style = MaterialTheme.typography.bodySmall)
                Text("Execution adapters:", style = MaterialTheme.typography.labelMedium)
                current.capabilities.executionAdapters.forEach { (environment, observation) ->
                    Text("• $environment: ${observation.status} — ${observation.detail}", style = MaterialTheme.typography.bodySmall)
                }
                Text("Private runtime root: ${current.runtimeRoot}")
                Text("Directories: images=${current.imageDirectoryReady}, instances=${current.instanceDirectoryReady}, logs=${current.logDirectoryReady}")
                current.notes.forEach { note ->
                    Text("• $note", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
