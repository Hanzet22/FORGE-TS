package com.HG.Forge.Ts.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private data class NetworkSnapshot(
    val connected: Boolean,
    val validated: Boolean,
    val transports: String
)

private fun readNetworkSnapshot(context: Context): NetworkSnapshot {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = manager.activeNetwork ?: return NetworkSnapshot(false, false, "No active network")
    val capabilities = manager.getNetworkCapabilities(network)
        ?: return NetworkSnapshot(false, false, "Network capabilities unavailable")
    val transports = buildList {
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add("Wi-Fi")
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add("Cellular")
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add("Ethernet")
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add("VPN")
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) add("Bluetooth")
    }.joinToString(" + ").ifBlank { "Other transport" }
    return NetworkSnapshot(
        connected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
        validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
        transports = transports
    )
}

@Composable
fun DiagnosticsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val snapshot = remember { mutableStateOf(readNetworkSnapshot(context)) }
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Network Diagnostics", style = MaterialTheme.typography.headlineMedium)
        Text(
            "A local snapshot of Android's active network capabilities. This screen does not send diagnostic probes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Transport", style = MaterialTheme.typography.labelMedium)
                Text(snapshot.value.transports, style = MaterialTheme.typography.titleLarge)
                Text(
                    if (snapshot.value.connected) "Internet capability: available" else "Internet capability: unavailable",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    if (snapshot.value.validated) "Internet validation: Android reports validated connectivity"
                    else "Internet validation: not validated",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Button(onClick = { snapshot.value = readNetworkSnapshot(context) }) {
            Text("Refresh network status")
        }
        Text(
            "For a specific authorized endpoint, use Network → Host Checker. It performs one DNS lookup and one selected TCP/TLS check; it is not a scanner.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
