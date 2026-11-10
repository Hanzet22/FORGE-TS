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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private enum class SimulationEnvironment(
    val title: String,
    val summary: String,
    val status: String,
    val availability: String
) {
    XFCE_NATIVE(
        "XFCE4 Desktop Native",
        "Standalone XFCE4 desktop managed by the FORGE-TS Linux runtime.",
        "Foundation prepared",
        "The private Linux workspace foundation is available below. Desktop installation and display lifecycle are not implemented yet."
    ),
    XFCE_PROOT(
        "XFCE4 PRoot",
        "XFCE4 inside a user-selected Linux distribution: Alpine, Ubuntu, or Debian.",
        "Planned",
        "Distribution downloads, rootfs verification, safe extraction, PRoot setup, and desktop display integration are still pending."
    ),
    MIKROTIK(
        "MikroTik RouterOS",
        "RouterOS CHR guest environment for network simulation.",
        "Coming soon",
        "CHR execution remains gated on licensing clarification, compatible virtualization, and guest-isolation evidence."
    ),
    LINUX_NATIVE(
        "Linux Ubuntu/Debian Desktop Native",
        "A separately provisioned Ubuntu or Debian desktop userland managed by FORGE-TS.",
        "Planned",
        "This will have its own distribution provisioning and lifecycle; it is not the PRoot environment."
    ),
    VM_WORKSTATION(
        "VM WStation",
        "A general-purpose virtual-machine workstation for compatible guest systems.",
        "Planned",
        "The VM supervisor and supported guest/backend matrix have not been implemented or validated."
    )
}

@Composable
fun SimulationEnvironmentLauncher() {
    var selected by remember { mutableStateOf<SimulationEnvironment?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Environment Launcher", style = MaterialTheme.typography.titleLarge)
            Text(
                "Choose one of the five independent simulation environments. Entries that are not ready show their current implementation status.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SimulationEnvironment.entries.forEach { environment ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(environment.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            environment.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (environment == SimulationEnvironment.MIKROTIK) {
                                OutlinedButton(onClick = { selected = environment }, enabled = false) {
                                    Text(environment.status)
                                }
                            } else {
                                Button(onClick = { selected = environment }) {
                                    Text("Open")
                                }
                                Text(
                                    environment.status,
                                    modifier = Modifier.padding(top = 10.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            selected?.let { environment ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(environment.title, style = MaterialTheme.typography.titleSmall)
                        Text(environment.availability, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
