package com.HG.Forge.Ts.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.HG.Forge.Ts.data.simulation.RouterOsImageImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RouterOsImageImportPanel() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val importer = remember(context) { RouterOsImageImporter(context) }
    var expectedHash by remember { mutableStateOf("") }
    var importing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("No RouterOS image has been staged.") }
    var verifiedPath by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importing = true
            message = "Copying and verifying SHA-256…"
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching { importer.importRawImage(uri, expectedHash) }
                }
                result.onSuccess {
                    verifiedPath = it.filePath
                    message = "SHA-256 verified • ${it.sizeBytes / 1_048_576} MiB staged."
                }.onFailure {
                    verifiedPath = null
                    message = it.message ?: "Image import failed."
                }
                importing = false
            }
        }
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("RouterOS CHR • Phase 7", style = MaterialTheme.typography.titleMedium)
            Text(
                "Select an official MikroTik CHR ARM64 RAW image and enter its SHA-256 from the official download source. The app verifies and stages the file privately; it does not boot it yet.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = expectedHash,
                onValueChange = { expectedHash = it.filter(Char::isLetterOrDigit).take(64) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Expected SHA-256 (64 hex characters)") },
                singleLine = true
            )
            Button(
                onClick = { picker.launch(arrayOf("*/*")) },
                enabled = !importing && expectedHash.matches(Regex("^[a-fA-F0-9]{64}$"))
            ) { Text(if (importing) "Verifying…" else "Choose CHR ARM64 RAW image") }
            Text(message, style = MaterialTheme.typography.bodySmall)
            verifiedPath?.let { Text("Private image: $it", style = MaterialTheme.typography.labelSmall) }
            Text(
                "Verification proves the imported bytes match the supplied hash; it does not prove the hash source is authentic, CHR boot compatibility, or KVM availability.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
