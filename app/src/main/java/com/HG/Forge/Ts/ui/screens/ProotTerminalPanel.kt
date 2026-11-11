package com.HG.Forge.Ts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.HG.Forge.Ts.data.simulation.HybridRuntimeManager
import com.HG.Forge.Ts.data.simulation.ProotDistribution
import com.HG.Forge.Ts.data.simulation.RuntimeEnvironmentId
import kotlinx.coroutines.launch

@Composable
fun ProotTerminalPanel() {
    val context = LocalContext.current
    val manager = remember(context) { HybridRuntimeManager(context) }
    val scope = rememberCoroutineScope()
    var distribution by remember { mutableStateOf(ProotDistribution.DEBIAN) }
    var lifecycle by remember { mutableStateOf("Loading runtime registry…") }
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("Select an installed PRoot instance, then start its shell.") }
    var busy by remember { mutableStateOf(false) }

    fun refreshState() {
        val instance = manager.inspect().prootInstances.firstOrNull { it.distribution == distribution }
        lifecycle = instance?.lifecycle?.name ?: "UNKNOWN"
    }

    LaunchedEffect(manager) {
        val result = manager.initialize()
        lifecycle = if (result.accepted) "Registry ready" else result.message
        refreshState()
    }

    Card {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("PRoot Interactive Shell", style = MaterialTheme.typography.titleLarge)
            Text("Pipe-backed live shell session. This is interactive command I/O, not yet a full PTY: terminal job control, resize signals, and TTY-dependent applications are not supported.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ProotDistribution.entries.forEach { item ->
                    androidx.compose.material3.FilterChip(
                        selected = distribution == item,
                        onClick = { distribution = item; refreshState() },
                        label = { Text(item.name) },
                        enabled = !busy
                    )
                }
            }
            Text("Lifecycle: $lifecycle", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            busy = true
                            val result = manager.requestStart(RuntimeEnvironmentId.XFCE_PROOT, distribution)
                            output = result.message
                            refreshState()
                            busy = false
                        }
                    },
                    enabled = !busy
                ) { Text("Start shell") }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            val result = manager.requestStop(RuntimeEnvironmentId.XFCE_PROOT, distribution)
                            output = result.message
                            refreshState()
                            busy = false
                        }
                    },
                    enabled = !busy
                ) { Text("Stop") }
                OutlinedButton(onClick = { refreshState() }, enabled = !busy) { Text("Refresh") }
            }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.take(16384) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Shell input") },
                placeholder = { Text("whoami && uname -a") },
                minLines = 1,
                maxLines = 3
            )
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        val result = manager.writeProotTerminalInput(distribution, input + "\n")
                        output = result.message
                        if (result.accepted) input = ""
                        busy = false
                    }
                },
                enabled = !busy && input.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Send to shell") }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        busy = true
                        val result = manager.readProotTerminalOutput(distribution)
                        output = if (result.output.isNotEmpty()) output + result.output else output + "\n" + result.message
                        refreshState()
                        busy = false
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Read output") }
            Text(
                output,
                modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp, max = 260.dp)
                    .verticalScroll(rememberScrollState()).padding(10.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            )
        }
    }
}
