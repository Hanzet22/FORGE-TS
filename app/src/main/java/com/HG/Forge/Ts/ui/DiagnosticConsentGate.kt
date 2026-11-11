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
                        "You can optionally allow automatic upload of technical diagnostics to the FORGE-TS service " +
                        "when the app opens and when important warnings or errors occur. " +
                        "Uploaded data may include app and Android versions, event labels, installation pseudonymous ID, " +
                        "and technical exception traces. It does not include passwords, terminal input/output, message " +
                        "contents, or network payloads. Diagnostics are used for reliability and troubleshooting. " +
                        "You can change this choice later in Settings."
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
