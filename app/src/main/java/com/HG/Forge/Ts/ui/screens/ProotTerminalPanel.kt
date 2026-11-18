package com.HG.Forge.Ts.ui.screens

import com.HG.Forge.Ts.ui.localization.forgeTextFor
import com.HG.Forge.Ts.ui.localization.LocalForgeLanguage
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
private data class OfficialAlpineMirror(val countryCode: String, val location: String, val host: String, val url: String)
private data class OfficialAlpineMirrorCatalog(val capturedAt: String, val mirrors: List<OfficialAlpineMirror>)

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

private fun loadOfficialAlpineMirrors(context: android.content.Context): OfficialAlpineMirrorCatalog {
    val json = JSONObject(context.assets.open("proot-runtime/alpine-mirrors.json").bufferedReader().use { it.readText() })
    val mirrors = json.getJSONArray("mirrors")
    val entries = buildList {
        for (index in 0 until mirrors.length()) {
            val item = mirrors.getJSONObject(index)
            val url = item.optString("url")
            if (url.startsWith("https://") && url.endsWith("/alpine")) {
                add(OfficialAlpineMirror(
                    countryCode = item.optString("countryCode"),
                    location = item.optString("countryOrLocation"),
                    host = item.optString("host"),
                    url = url
                ))
            }
        }
    }
    return OfficialAlpineMirrorCatalog(json.optString("capturedAt"), entries)
}

@Composable
fun ProotTerminalPanel() {
    val context = LocalContext.current
    val language = LocalForgeLanguage.current.language
    val manager = remember(context) { HybridRuntimeManager(context) }
    val mirrorProfiles = remember(context) { ProotMirrorProfileStore(context) }
    val mirrorCountries = remember(context) { runCatching { loadMirrorCountryOptions(context) }.getOrElse { emptyList() } }
    val officialAlpineCatalog = remember(context) {
        runCatching { loadOfficialAlpineMirrors(context) }.getOrElse { OfficialAlpineMirrorCatalog("", emptyList()) }
    }
    val officialAlpineMirrors = officialAlpineCatalog.mirrors
    val scope = rememberCoroutineScope()
    var distribution by remember { mutableStateOf(ProotDistribution.DEBIAN) }
    var lifecycle by remember { mutableStateOf(forgeTextFor("Loading runtime registry…", language)) }
    var status by remember { mutableStateOf(forgeTextFor("Native PTY terminal is ready.", language)) }
    var busy by remember { mutableStateOf(false) }
    var vncPassword by remember { mutableStateOf("") }
    var vncReady by remember { mutableStateOf(false) }
    var showBuiltInVnc by remember { mutableStateOf(false) }
    var showMirrorCountryPicker by remember { mutableStateOf(false) }
    var showAlpineMirrorPicker by remember { mutableStateOf(false) }
    var mirrorCountryQuery by remember { mutableStateOf("") }
    var alpineMirrorQuery by remember { mutableStateOf("") }
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
        lifecycle = if (result.accepted) forgeTextFor("Registry ready", language) else forgeTextFor(result.message, language)
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
            Text(forgeTextFor("PRoot Terminal", language), style = MaterialTheme.typography.titleLarge)
            Text(forgeTextFor("Native PTY + VT emulator • ANSI colors, cursor, scrollback, keyboard input and terminal resize.", language),
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
                    Text(selectedMirrorCountry?.let { "${it.name} • ${forgeTextFor("mirror profile", language)}" } ?: forgeTextFor("Choose country profile", language))
                }
                selectedMirrorCountry?.let { country ->
                    TextButton(onClick = {
                        mirrorUrl = mirrorProfiles.get(distribution, country.code).orEmpty()
                        status = if (mirrorUrl.isBlank()) "${forgeTextFor("No saved mirror URL for", language)} ${distribution.name} / ${country.name}. ${forgeTextFor("Enter an official HTTPS mirror URL.", language)}" else "${forgeTextFor("Loaded", language)} ${country.name} HTTPS ${forgeTextFor("mirror profile", language)}."
                    }, enabled = !busy) { Text(forgeTextFor("Load saved", language)) }
                }
            }
            OutlinedTextField(
                value = mirrorUrl,
                onValueChange = { mirrorUrl = it.take(300) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(forgeTextFor("HTTPS package mirror", language)) },
                placeholder = { Text(when (distribution) {
                    ProotDistribution.DEBIAN -> "https://mirror.example/debian"
                    ProotDistribution.UBUNTU -> "https://mirror.example/ubuntu-ports"
                    ProotDistribution.ALPINE -> "https://mirror.example/alpine"
                }) },
                supportingText = { Text(forgeTextFor("Enter an official HTTPS URL. You can save it to one of ${mirrorCountries.size} country profiles; package signatures remain enabled.", language)) },
                singleLine = true
            )
            if (distribution == ProotDistribution.ALPINE) {
                OutlinedButton(onClick = { showAlpineMirrorPicker = true }, enabled = !busy && officialAlpineMirrors.isNotEmpty()) {
                    Text(forgeTextFor("Browse ${officialAlpineMirrors.size} official Alpine mirrors", language))
                }
                Text(forgeTextFor("Alpine mirror status snapshot: ${officialAlpineCatalog.capturedAt} · HTTPS · aarch64 v3.24 index reported OK by upstream.", language),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            selectedMirrorCountry?.let { country ->
                OutlinedButton(
                    onClick = {
                        runCatching { mirrorProfiles.save(distribution, country.code, mirrorUrl) }
                            .onSuccess { status = "${forgeTextFor("Saved HTTPS mirror profile for", language)} ${country.name} (${distribution.name})." }
                            .onFailure { status = it.message ?: forgeTextFor("Could not save this mirror profile.", language) }
                    },
                    enabled = !busy && mirrorUrl.isNotBlank()
                ) { Text(forgeTextFor("Save ${country.name} profile", language)) }
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
                ) { Text(forgeTextFor("Use official HTTPS mirror", language)) }
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
                ) { Text(forgeTextFor("Apply mirror", language)) }
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
                ) { Text(forgeTextFor("Restore default", language)) }
            }
            Text(forgeTextFor("Lifecycle: $lifecycle", language), style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            status = "${forgeTextFor("Preparing rootfs for", language)} ${distribution.name}: ${forgeTextFor("downloading, verifying SHA-256 and extracting…", language)}"
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
                ) { Text(forgeTextFor(if (lifecycle == "READY" || lifecycle == "STOPPED") "Repair RootFS" else "Install RootFS", language)) }
                OutlinedButton(
                    onClick = { refreshState() },
                    enabled = !busy
                ) { Text(forgeTextFor("Refresh", language)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            status = forgeTextFor("Preparing XFCE4 bootstrap…", language)
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
                ) { Text(forgeTextFor("Prepare / Repair XFCE4", language)) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            status = forgeTextFor("Preparing TigerVNC server…", language)
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
                ) { Text(forgeTextFor("Install / Repair VNC Viewer", language)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
                        val random = SecureRandom()
                        val password = buildString { repeat(8) { append(alphabet[random.nextInt(alphabet.length)]) } }
                        scope.launch {
                            busy = true
                            status = forgeTextFor("Starting localhost-only TigerVNC and XFCE4…", language)
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
                ) { Text(forgeTextFor("Start Desktop", language)) }
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
                ) { Text(forgeTextFor("Stop Desktop", language)) }
            }
            if (vncReady) {
                Text(forgeTextFor("VNC server: loopback:5901 · built-in viewer: localhost:6080", language), style = MaterialTheme.typography.labelMedium)
                Text(forgeTextFor("Temporary VNC password: $vncPassword", language), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard?.setPrimaryClip(ClipData.newPlainText("FORGE-TS VNC password", vncPassword))
                            status = forgeTextFor("Temporary VNC password copied to clipboard.", language)
                        }
                    ) { Text(forgeTextFor("Copy Password", language)) }
                    Button(onClick = { showBuiltInVnc = true }) { Text(forgeTextFor("Open built-in viewer", language)) }
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("vnc://127.0.0.1:5901")))
                            }.onFailure {
                                status = forgeTextFor("No VNC viewer is registered. Install a compatible viewer such as AVNC, then open the endpoint again.", language)
                            }
                        }
                    ) { Text(forgeTextFor("External viewer", language)) }
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
                ) { Text(forgeTextFor("Start", language)) }
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
                ) { Text(forgeTextFor("Stop", language)) }
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
            title = { Text(forgeTextFor("Choose a country profile", language)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(forgeTextFor("This profile saves a mirror URL you provide. Country selection does not imply a public mirror is hosted there.", language),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = mirrorCountryQuery,
                        onValueChange = { mirrorCountryQuery = it.take(50) },
                        label = { Text(forgeTextFor("Search ${mirrorCountries.size} countries", language)) },
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
                            ) { Text(forgeTextFor("Manual URL (no country profile)", language), modifier = Modifier.fillMaxWidth()) }
                        }
                        items(filteredCountries, key = { it.code }) { country ->
                            TextButton(
                                onClick = {
                                    selectedMirrorCountry = country
                                    mirrorUrl = mirrorProfiles.get(distribution, country.code).orEmpty()
                                    status = if (mirrorUrl.isBlank()) "${forgeTextFor("No saved mirror URL for", language)} ${country.name}. ${forgeTextFor("Enter an official HTTPS mirror URL.", language)}" else "${forgeTextFor("Loaded", language)} ${country.name} ${forgeTextFor("mirror profile", language)}."
                                    mirrorCountryQuery = ""
                                    showMirrorCountryPicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(forgeTextFor("${country.name} (${country.code})", language), modifier = Modifier.fillMaxWidth()) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMirrorCountryPicker = false }) { Text(forgeTextFor("Close", language)) } }
        )
    }
    if (showAlpineMirrorPicker) {
        val filteredMirrors = officialAlpineMirrors.filter {
            alpineMirrorQuery.isBlank() || it.location.contains(alpineMirrorQuery, ignoreCase = true) ||
                it.host.contains(alpineMirrorQuery, ignoreCase = true)
        }
        AlertDialog(
            onDismissRequest = { showAlpineMirrorPicker = false },
            title = { Text(forgeTextFor("Official Alpine HTTPS mirrors", language)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = alpineMirrorQuery,
                        onValueChange = { alpineMirrorQuery = it.take(80) },
                        label = { Text(forgeTextFor("Search location or host", language)) },
                        singleLine = true
                    )
                    LazyColumn(modifier = Modifier.height(360.dp)) {
                        items(filteredMirrors, key = { it.url }) { mirror ->
                            TextButton(
                                onClick = {
                                    mirrorUrl = mirror.url
                                    selectedMirrorCountry = mirrorCountries.firstOrNull { it.code == mirror.countryCode }
                                    status = "Selected official Alpine mirror ${mirror.host}; upstream reported aarch64 v3.24 index OK in the ${officialAlpineCatalog.capturedAt} snapshot."
                                    alpineMirrorQuery = ""
                                    showAlpineMirrorPicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(mirror.location, style = MaterialTheme.typography.labelLarge)
                                    Text(mirror.host, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAlpineMirrorPicker = false }) { Text(forgeTextFor("Close", language)) } }
        )
    }
}
