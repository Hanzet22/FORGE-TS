package com.HG.Forge.Ts.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.HG.Forge.Ts.data.sync.ForgeDiagnosticsConsent

@Composable
fun DiagnosticConsentGate() {
    val context = LocalContext.current
    var showDialog by remember {
        mutableStateOf(!ForgeDiagnosticsConsent.hasDecision(context))
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Diagnostics & privacy") },
            text = {
                Text(
                    "FORGE-TS always keeps a limited technical log on this device. " +
                        "If you allow remote diagnostics, the app automatically uploads one bounded report when it " +
                        "moves to the background, and retries later if the network is unavailable. Reports can include " +
                        "Info, Warning, Error, Crash and Critical Error events; app and Android versions; device " +
                        "brand/model, RAM, display resolution, CPU chipset and architecture; event labels; and a " +
                        "pseudonymous installation ID. Technical exception traces are bounded. It does not collect " +
                        "IMEI or serial numbers, location, passwords, terminal input/output, message contents, files, " +
                        "or network payloads. Remote upload is optional and can be turned off in Settings at any time."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    ForgeDiagnosticsConsent.setEnabled(context, true)
                    showDialog = false
                }) {
                    Text("Allow automatic diagnostics")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    ForgeDiagnosticsConsent.setEnabled(context, false)
                    showDialog = false
                }) {
                    Text("Keep on-device only")
                }
            }
        )
    }
}
