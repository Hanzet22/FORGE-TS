package com.HG.Forge.Ts.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.HG.Forge.Ts.data.simulation.HybridRuntimeManager
import com.HG.Forge.Ts.data.simulation.ProotDistribution
import com.HG.Forge.Ts.data.simulation.ProotMirrorProfileStore
import com.HG.Forge.Ts.data.simulation.RuntimeEnvironmentId
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.connectbot.terminal.Terminal
import org.connectbot.terminal.TerminalEmulatorFactory
import java.security.SecureRandom

private data class MirrorCountryOption(val code: String, val name: String)

private fun loadMirrorCountryOptions(context: android.content.Context): List<MirrorCountryOption> {
    val json = context.assets.open("dns-catalog/country-index.json").bufferedReader().use { it.readText() }
    val countries = JSONObject(json).getJSONArray("countries")
    return buildList {
        for (index in 0 until countries.length()) {
            val item = countries.getJSONObject(index)
            val code = item.optString("code")
            val name = item.optString("name")
            if (code.matches(Regex("[A-Z]{2}")) && name.isNotBlank()) add(MirrorCountryOption(code, name))
        }
    }
}

@Composable
fun ProotTerminalPanel() {
    val context = LocalContext.current
    val manager = remember(context) { HybridRuntimeManager(context) }
    val mirrorProfiles = remember(context) { ProotMirrorProfileStore(context) }
    val mirrorCountries = remember(context) { runCatching { loadMirrorCountryOptions(context) }.getOrElse { emptyList() } }
    val scope = rememberCoroutineScope()
    var distribution by remember { mutableStateOf(ProotDistribution.DEBIAN) }
    var lifecycle by remember { mutableStateOf("Loading runtime registry…") }
    var status by remember { mutableStateOf("Native PTY terminal is ready.") }
    var busy by remember { mutableStateOf(false) }
    var vncPassword by remember { mutableStateOf("") }
    var vncReady by remember { mutableStateOf(false) }
    var showBuiltInVnc by remember { mutableStateOf(false) }
    var showMirrorCountryPicker by remember { mutableStateOf(false) }
    var mirrorCountryQuery by remember { mutableStateOf("") }
    var selectedMirrorCountry by remember { mutableStateOf<MirrorCountryOption?>(null) }
    var mirrorUrl by remember(distribution) { mutableStateOf("") }

    val terminalEmulator = remember(manager, distribution) {
        TerminalEmulatorFactory.create(
            initialRows = 24,
            initialCols = 80,
            onKeyboardInput = { bytes ->
                scope.launch { manager.writeProotTerminalBytes(distribution, bytes) }
            },
            onResize = { dimensions ->
                scope.launch {
                    manager.resizeProotTerminal(
                        distribution,
                        dimensions.rows,
                        dimensions.columns
                    )
                }
            }
        )
    }

    fun refreshState() {
        val instance = manager.inspect().prootInstances.firstOrNull {
            it.distribution == distribution
        }
        lifecycle = instance?.lifecycle?.name ?: "UNKNOWN"
    }

    LaunchedEffect(manager) {
        val result = manager.initialize()
        lifecycle = if (result.accepted) "Registry ready" else result.message
        refreshState()
    }

    LaunchedEffect(distribution) {
        terminalEmulator.clearScreen()
        vncPassword = ""
        vncReady = false
        mirrorUrl = selectedMirrorCountry?.let { mirrorProfiles.get(distribution, it.code) }.orEmpty()
        refreshState()
    }

    LaunchedEffect(distribution, lifecycle) {
        if (lifecycle == "RUNNING") {
            while (true) {
                val result = manager.readProotTerminalOutput(distribution)
                if (result.rawOutput.isNotEmpty()) {
                    terminalEmulator.writeInput(result.rawOutput)
                }
                if (result.message != "Native PTY output read.") {
                    status = result.message
                }
                if (!result.accepted) {
                    lifecycle = "EXITED"
                    status = result.message
                    break
                }
            }
        }
    }

    Card {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("PRoot Terminal", style = MaterialTheme.typography.titleLarge)
            Text(
                "Native PTY + VT emulator • ANSI colors, cursor, scrollback, keyboard input and terminal resize.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ProotDistribution.entries.forEach { item ->
                    FilterChip(
                        selected = distribution == item,
                        onClick = { distribution = item },
                        label = { Text(item.name) },
                        enabled = !busy
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showMirrorCountryPicker = true }, enabled = !busy) {
                    Text(selectedMirrorCountry?.let { "${it.name} mirror profile" } ?: "Choose country profile")
                }
                selectedMirrorCountry?.let { country ->
                    TextButton(onClick = {
                        mirrorUrl = mirrorProfiles.get(distribution, country.code).orEmpty()
                        status = if (mirrorUrl.isBlank()) "No saved ${distribution.name} URL for ${country.name}. Enter an official HTTPS mirror URL." else "Loaded ${country.name} HTTPS mirror profile."
                    }, enabled = !busy) { Text("Load saved") }
                }
            }
            OutlinedTextField(
                value = mirrorUrl,
                onValueChange = { mirrorUrl = it.take(300) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("HTTPS package mirror") },
                placeholder = { Text(when (distribution) {
                    ProotDistribution.DEBIAN -> "https://mirror.example/debian"
                    ProotDistribution.UBUNTU -> "https://mirror.example/ubuntu-ports"
                    ProotDistribution.ALPINE -> "https://mirror.example/alpine"
                }) },
                supportingText = { Text("Enter an official HTTPS URL. You can save it to one of ${mirrorCountries.size} country profiles; package signatures remain enabled.") },
                singleLine = true
            )
            selectedMirrorCountry?.let { country ->
                OutlinedButton(
                    onClick = {
                        runCatching { mirrorProfiles.save(distribution, country.code, mirrorUrl) }
                            .onSuccess { status = "Saved HTTPS mirror profile for ${country.name} (${distribution.name})." }
                            .onFailure { status = it.message ?: "Could not save this mirror profile." }
                    },
                    enabled = !busy && mirrorUrl.isNotBlank()
                ) { Text("Save ${country.name} profile") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val officialMirror = when (distribution) {
                            ProotDistribution.DEBIAN -> "https://deb.debian.org/debian"
                            ProotDistribution.UBUNTU -> "https://ports.ubuntu.com/ubuntu-ports"
                            ProotDistribution.ALPINE -> "https://dl-cdn.alpinelinux.org/alpine"
                        }
                        mirrorUrl = officialMirror
                        scope.launch {
                            busy = true
                            try { status = manager.configureProotPackageMirror(distribution, officialMirror).message }
                            finally { busy = false }
                        }
                    },
                    enabled = !busy && lifecycle != "RUNNING"
                ) { Text("Use official HTTPS mirror") }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                val result = manager.configureProotPackageMirror(distribution, mirrorUrl)
                                status = result.message
                                val country = selectedMirrorCountry
                                if (result.accepted && country != null) {
                                    runCatching { mirrorProfiles.save(distribution, country.code, mirrorUrl) }
                                        .onFailure { status = "${result.message} Profile save failed: ${it.message ?: "storage unavailable"}" }
                                        .onSuccess { status = "${result.message} Saved for ${country.name}." }
                                }
                            } finally { busy = false }
                        }
                    },
                    enabled = !busy && lifecycle != "RUNNING" && mirrorUrl.isNotBlank()
                ) { Text("Apply mirror") }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                status = manager.configureProotPackageMirror(distribution, null).message
                                mirrorUrl = ""
                            } finally { busy = false }
                        }
                    },
                    enabled = !busy && lifecycle != "RUNNING"
                ) { Text("Restore default") }
            }
            Text("Lifecycle: $lifecycle", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            status = "Preparing ${distribution.name} rootfs: downloading, verifying SHA-256 and extracting…"
                            try {
                                val result = manager.installProotRootfs(distribution)
                                status = result.message
                                refreshState()
                                terminalEmulator.clearScreen()
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy && lifecycle !in setOf("RUNNING", "STARTING", "RECOVERY_REQUIRED")
                ) { Text(if (lifecycle == "READY" || lifecycle == "STOPPED") "Repair RootFS" else "Install RootFS") }
                OutlinedButton(
                    onClick = { refreshState() },
                    enabled = !busy
                ) { Text("Refresh") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            status = "Preparing XFCE4 bootstrap…"
                            try {
                                val result = manager.prepareProotDesktop(distribution) { stage ->
                                    status = stage
                                }
                                status = result.message
                            } finally {
                                busy = false
                                refreshState()
                            }
                        }
                    },
                    enabled = !busy && lifecycle == "RUNNING"
                ) { Text("Prepare / Repair XFCE4") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            status = "Preparing TigerVNC server…"
                            try {
                                val result = manager.prepareProotVncServer(distribution) { stage -> status = stage }
                                status = result.message
                            } finally {
                                busy = false
                                refreshState()
                            }
                        }
                    },
                    enabled = !busy && lifecycle == "RUNNING"
                ) { Text("Install / Repair VNC Viewer") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
                        val random = SecureRandom()
                        val password = buildString { repeat(8) { append(alphabet[random.nextInt(alphabet.length)]) } }
                        scope.launch {
                            busy = true
                            status = "Starting localhost-only TigerVNC and XFCE4…"
                            try {
                                val result = manager.startProotDesktop(distribution, password)
                                status = result.message
                                vncReady = result.accepted
                                vncPassword = if (result.accepted) password else ""
                            } finally {
                                busy = false
                                refreshState()
                            }
                        }
                    },
                    enabled = !busy && lifecycle == "RUNNING"
                ) { Text("Start Desktop") }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                val result = manager.stopProotDesktop(distribution)
                                status = result.message
                                vncReady = false
                                vncPassword = ""
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy && lifecycle == "RUNNING"
                ) { Text("Stop Desktop") }
            }
            if (vncReady) {
                Text("VNC server: loopback:5901 · built-in viewer: localhost:6080", style = MaterialTheme.typography.labelMedium)
                Text("Temporary VNC password: $vncPassword", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard?.setPrimaryClip(ClipData.newPlainText("FORGE-TS VNC password", vncPassword))
                            status = "Temporary VNC password copied to clipboard."
                        }
                    ) { Text("Copy Password") }
                    Button(onClick = { showBuiltInVnc = true }) { Text("Open built-in viewer") }
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("vnc://127.0.0.1:5901")))
                            }.onFailure {
                                status = "No VNC viewer is registered. Install a compatible viewer such as AVNC, then open the endpoint again."
                            }
                        }
                    ) { Text("External viewer") }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                val result = manager.requestStart(
                                    RuntimeEnvironmentId.XFCE_PROOT,
                                    distribution
                                )
                                status = result.message
                                refreshState()
                                val dimensions = terminalEmulator.dimensions
                                manager.resizeProotTerminal(
                                    distribution,
                                    dimensions.rows,
                                    dimensions.columns
                                )
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy && lifecycle != "RUNNING"
                ) { Text("Start") }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                val result = manager.requestStop(
                                    RuntimeEnvironmentId.XFCE_PROOT,
                                    distribution
                                )
                                status = result.message
                                refreshState()
                                terminalEmulator.clearScreen()
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy && lifecycle == "RUNNING"
                ) { Text("Stop") }
            }
            Text(
                status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Terminal(
                terminalEmulator = terminalEmulator,
                modifier = Modifier.fillMaxWidth().height(360.dp),
                initialFontSize = 10.sp,
                backgroundColor = Color(0xFF080B10),
                foregroundColor = Color(0xFFE6EDF3),
                keyboardEnabled = lifecycle == "RUNNING" && !busy,
                showSoftKeyboard = lifecycle == "RUNNING" && !busy
            )
        }
    }

    if (showBuiltInVnc) ProotVncViewerDialog(onDismiss = { showBuiltInVnc = false })
    if (showMirrorCountryPicker) {
        val filteredCountries = mirrorCountries.filter {
            mirrorCountryQuery.isBlank() || it.name.contains(mirrorCountryQuery, ignoreCase = true) || it.code.contains(mirrorCountryQuery, ignoreCase = true)
        }
        AlertDialog(
            onDismissRequest = { showMirrorCountryPicker = false },
            title = { Text("Choose a country profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This profile saves a mirror URL you provide. Country selection does not imply a public mirror is hosted there.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = mirrorCountryQuery,
                        onValueChange = { mirrorCountryQuery = it.take(50) },
                        label = { Text("Search ${mirrorCountries.size} countries") },
                        singleLine = true
                    )
                    LazyColumn(modifier = Modifier.height(320.dp)) {
                        item(key = "no-country-profile") {
                            TextButton(
                                onClick = {
                                    selectedMirrorCountry = null
                                    mirrorCountryQuery = ""
                                    showMirrorCountryPicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Manual URL (no country profile)", modifier = Modifier.fillMaxWidth()) }
                        }
                        items(filteredCountries, key = { it.code }) { country ->
                            TextButton(
                                onClick = {
                                    selectedMirrorCountry = country
                                    mirrorUrl = mirrorProfiles.get(distribution, country.code).orEmpty()
                                    status = if (mirrorUrl.isBlank()) "No saved URL for ${country.name}. Enter a public HTTPS mirror URL." else "Loaded ${country.name} mirror profile."
                                    mirrorCountryQuery = ""
                                    showMirrorCountryPicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("${country.name} (${country.code})", modifier = Modifier.fillMaxWidth()) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMirrorCountryPicker = false }) { Text("Close") } }
        )
    }
}
