package com.HG.Forge.Ts.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.HG.Forge.Ts.data.simulation.LoopbackLabServer
import com.HG.Forge.Ts.data.simulation.RedisOfflineEngine
import com.HG.Forge.Ts.data.simulation.SupabaseOfflineEngine
import com.HG.Forge.Ts.ui.screens.RuntimeFoundationPanel
import com.HG.Forge.Ts.ui.screens.RouterOsImageImportPanel
import com.HG.Forge.Ts.ui.screens.LinuxUserlandFoundationPanel
import com.HG.Forge.Ts.ui.screens.SimulationEnvironmentLauncher

private enum class SimulationModule(val title: String) {
    REDIS("Redis"), SUPABASE("Supabase"), LOOPBACK("Loopback")
}

@Composable
fun SimulationScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var internetValidated by remember { mutableStateOf(false) }
    var selectedModule by remember { mutableStateOf(SimulationModule.REDIS) }
    var statusMessage by remember { mutableStateOf("Ready. Offline engines are available.") }
    var redisKey by remember { mutableStateOf("demo:welcome") }
    var redisValue by remember { mutableStateOf("FORGE-TS") }
    var redisTtl by remember { mutableStateOf("") }
    var redisOutput by remember { mutableStateOf("No command executed yet.") }
    var supabaseLabel by remember { mutableStateOf("lab-note") }
    var supabaseValue by remember { mutableStateOf("Hello from the local simulator") }
    var loopbackOutput by remember { mutableStateOf("Local server is stopped.") }
    var loopbackRunning by remember { mutableStateOf(false) }
    val loopbackServer = remember { LoopbackLabServer() }

    DisposableEffect(context) {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        fun readValidated(): Boolean {
            val network = manager.activeNetwork ?: return false
            val caps = manager.getNetworkCapabilities(network) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
        internetValidated = readValidated()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val caps = manager.getNetworkCapabilities(network)
                internetValidated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                internetValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
            override fun onLost(network: Network) {
                internetValidated = readValidated()
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        onDispose {
            manager.unregisterNetworkCallback(callback)
            loopbackServer.stop()
        }
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Simulation Center", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Hybrid local labs with automatic connectivity awareness.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SimulationEnvironmentLauncher()
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (internetValidated) "INTERNET AVAILABLE" else "INTERNET UNAVAILABLE", fontWeight = FontWeight.Bold)
                Text(
                    if (internetValidated) "Internet is validated. Simulation Center labs remain local; this screen does not start cloud requests."
                    else "Internet is unavailable. Local simulation remains available; no cloud request is made.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("Supabase Auth is managed by the shared app-level authentication gate. Simulation modules on this screen are local.", style = MaterialTheme.typography.labelSmall)
            }
        }
        RuntimeFoundationPanel()
        LinuxUserlandFoundationPanel()
        RouterOsImageImportPanel()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SimulationModule.entries.forEach { module ->
                FilterChip(selected = selectedModule == module, onClick = { selectedModule = module }, label = { Text(module.title) })
            }
        }
        when (selectedModule) {
            SimulationModule.REDIS -> {
                Text("Redis Lab", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("OFFLINE ENGINE • in-memory key/value store. Data is temporary and clears when the app process ends.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(redisKey, { redisKey = it }, Modifier.fillMaxWidth(), label = { Text("Key") }, singleLine = true)
                OutlinedTextField(redisValue, { redisValue = it }, Modifier.fillMaxWidth(), label = { Text("Value") })
                OutlinedTextField(redisTtl, { redisTtl = it.filter(Char::isDigit).take(5) }, Modifier.fillMaxWidth(), label = { Text("TTL seconds (optional, 1–86400)") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        try {
                            val ttl = redisTtl.toLongOrNull()
                            redisOutput = "SET ${redisKey}\n${RedisOfflineEngine.set(redisKey, redisValue, ttl)}"
                        } catch (e: Exception) { redisOutput = e.message ?: "SET failed." }
                    }) { Text("SET") }
                    OutlinedButton(onClick = { redisOutput = "GET ${redisKey}\n${RedisOfflineEngine.get(redisKey) ?: "(nil)"}" }) { Text("GET") }
                    OutlinedButton(onClick = { redisOutput = if (RedisOfflineEngine.delete(redisKey)) "DEL • 1 key removed" else "DEL • 0 keys removed" }) { Text("DEL") }
                }
                Button(onClick = { redisOutput = RedisOfflineEngine.snapshot().entries.joinToString("\n") { "${it.key} = ${it.value}" }.ifBlank { "(empty database)" } }, modifier = Modifier.fillMaxWidth()) { Text("Browse keys") }
                ResultPanel(redisOutput)
                Text("Online Redis operations are not exposed in Simulation Center. The local Redis simulator remains available.", style = MaterialTheme.typography.bodySmall)
            }
            SimulationModule.SUPABASE -> {
                Text("Supabase Lab", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("OFFLINE ENGINE • local sample table. Records are temporary and are not uploaded.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(supabaseLabel, { supabaseLabel = it }, Modifier.fillMaxWidth(), label = { Text("Record label") }, singleLine = true)
                OutlinedTextField(supabaseValue, { supabaseValue = it }, Modifier.fillMaxWidth(), label = { Text("Record value") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { try { val record = SupabaseOfflineEngine.insert(supabaseLabel, supabaseValue); statusMessage = "Inserted local row #${record.id}" } catch (e: Exception) { statusMessage = e.message ?: "Insert failed." } }) { Text("INSERT") }
                    OutlinedButton(onClick = { statusMessage = "Selected ${SupabaseOfflineEngine.select().size} local row(s)." }) { Text("SELECT") }
                    OutlinedButton(onClick = { SupabaseOfflineEngine.clearSession(); statusMessage = "Local sample table cleared." }) { Text("Clear") }
                }
                SupabaseOfflineEngine.select().forEach { record ->
                    Card {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("#${record.id} • ${record.label}", fontWeight = FontWeight.SemiBold)
                            Text(record.value, style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = { SupabaseOfflineEngine.delete(record.id); statusMessage = "Deleted local row #${record.id}" }) { Text("Delete row") }
                        }
                    }
                }
                ResultPanel(statusMessage)
                Text("Online Supabase table operations are not exposed in Simulation Center. This local sample table remains separate.", style = MaterialTheme.typography.bodySmall)
            }
            SimulationModule.LOOPBACK -> {
                Text("Loopback Lab", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("LOCAL DEVICE TEST • server binds only to 127.0.0.1 on an ephemeral port.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch {
                            loopbackOutput = "Starting local server..."
                            val result = withContext(Dispatchers.IO) {
                                runCatching { loopbackServer.start() }
                            }
                            result.onSuccess { port ->
                                loopbackRunning = true
                                loopbackOutput = "Listening on 127.0.0.1:$port\nGET /health"
                            }.onFailure { error ->
                                loopbackOutput = error.message ?: "Could not start localhost server."
                            }
                        }
                    }, enabled = !loopbackRunning) { Text("Start localhost") }
                    OutlinedButton(onClick = { loopbackServer.stop(); loopbackRunning = false; loopbackOutput = "Local server stopped." }, enabled = loopbackRunning) { Text("Stop") }
                }
                Button(onClick = {
                    scope.launch {
                        loopbackOutput = "Sending local GET /health..."
                        loopbackOutput = withContext(Dispatchers.IO) {
                            runCatching { loopbackServer.requestHealth() }
                                .getOrElse { it.message ?: "Local request failed." }
                        }
                    }
                }, enabled = loopbackRunning, modifier = Modifier.fillMaxWidth()) { Text("Send local GET /health") }
                ResultPanel(loopbackOutput)
                Text("Simulator mode: deterministic sample response, no socket opened.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { loopbackOutput = "SIMULATED • 127.0.0.1\nHTTP/1.1 200 OK\n{ \"status\": \"healthy\", \"source\": \"simulator\" }" }, modifier = Modifier.fillMaxWidth()) { Text("Run simulated response") }
            }
        }
        Text("IMPORTANT: Local Redis/Supabase simulation data is temporary and never auto-syncs to production. Cloud connectors are not exposed in Simulation Center. Privileged backend credentials remain server-side and never belong in the APK.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ResultPanel(value: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
        Text(value, Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodySmall)
    }
}
