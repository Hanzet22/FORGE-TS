package com.HG.Forge.Ts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CoreNetworkingScreen(modifier: Modifier = Modifier, viewModel: CoreNetworkingViewModel = viewModel()) {
    var selectedTool by remember { mutableIntStateOf(0) }
    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("Core Networking", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Practical network utilities for everyday diagnostics.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ScrollableTabRow(selectedTabIndex = selectedTool) {
            listOf("CIDR", "VLSM", "IP Plan", "My IP", "Host").forEachIndexed { index, label ->
                Tab(selected = selectedTool == index, onClick = { selectedTool = index }, text = { Text(label) })
            }
        }
        when (selectedTool) {
            0 -> CidrTool(viewModel)
            1 -> VlsmPlannerScreen()
            2 -> NetworkIpAllocationScreen()
            3 -> PublicIpTool(viewModel)
            else -> HostTool(viewModel)
        }
    }
}

@Composable
private fun CidrTool(viewModel: CoreNetworkingViewModel) {
    var input by remember { mutableStateOf("192.168.1.10/24") }
    var binary by remember { mutableStateOf<String?>(null) }
    var binaryError by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("IPv4 & IPv6 subnet calculator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Address / prefix") }, placeholder = { Text("2001:db8::1/64") }, singleLine = true)
        Button(onClick = {
            viewModel.calculateCidr(input)
            try {
                binary = com.HG.Forge.Ts.data.network.CidrCalculator.toBinary(input.substringBefore("/"))
                binaryError = null
            } catch (error: Exception) {
                binary = null
                binaryError = error.message ?: "Invalid address"
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Calculate network") }
        viewModel.cidrError?.let { ErrorText(it) }
        binaryError?.let { ErrorText(it) }
        viewModel.cidrResult?.let { result ->
            ResultCard {
                Text(result.addressFamily, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                ResultRow("Address", result.address)
                ResultRow("Prefix", "/${result.prefix}")
                ResultRow("Network address", result.network)
                ResultRow("Last address / broadcast", result.lastAddress)
                result.subnetMask?.let { ResultRow("Subnet mask", it) }
                result.broadcastAddress?.let { ResultRow("Broadcast address", it) }
                result.firstUsableAddress?.let { ResultRow("First usable host", it) }
                result.lastUsableAddress?.let { ResultRow("Last usable host", it) }
                result.usableHostCount?.let { ResultRow("Usable host count", it) }
                ResultRow("Total addresses", result.addressCount)
                if (result.addressFamily == "IPv6") {
                    Text("IPv6 has no broadcast address. Usable host counts are not calculated using IPv4 network/broadcast subtraction.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                binary?.let { ResultRow("Binary address", it) }
            }
        }
        Text("IPv4 /31 is treated as a two-address point-to-point subnet (RFC 3021); /32 is one host route. IPv6 does not use broadcast addressing.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Calculations and binary conversion run locally on this device. No address is uploaded.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PublicIpTool(viewModel: CoreNetworkingViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Public IP address", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Fetches the public address visible to an external service. This is not your device's private LAN address.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = { viewModel.loadPublicIp() }, enabled = !viewModel.isLoadingIp, modifier = Modifier.fillMaxWidth()) {
            if (viewModel.isLoadingIp) CircularProgressIndicator(strokeWidth = 2.dp) else Text("Check public IP")
        }
        viewModel.publicIpError?.let { ErrorText(it) }
        viewModel.publicIp?.let { result ->
            ResultCard {
                Text("Public IP", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(result.address, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Source: ${result.source}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("Your public IP is sent to the lookup provider as part of the request. No location history is stored by FORGE-TS.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HostTool(viewModel: CoreNetworkingViewModel) {
    var host by remember { mutableStateOf("example.com") }
    var port by remember { mutableStateOf("443") }
    var checkTls by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Authorized host check", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(value = host, onValueChange = { host = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Hostname or IP address") }, singleLine = true)
        OutlinedTextField(value = port, onValueChange = { port = it.filter(Char::isDigit).take(5) }, modifier = Modifier.fillMaxWidth(), label = { Text("TCP port") }, singleLine = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checkTls, onCheckedChange = { checkTls = it })
            Text("Verify TLS certificate and hostname")
        }
        Button(onClick = { viewModel.checkHost(host, port, checkTls) }, enabled = !viewModel.isCheckingHost, modifier = Modifier.fillMaxWidth()) {
            if (viewModel.isCheckingHost) CircularProgressIndicator(strokeWidth = 2.dp) else Text("Check host")
        }
        viewModel.hostError?.let { ErrorText(it) }
        viewModel.hostResult?.let { result ->
            ResultCard {
                Text(result.host, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ResultRow("Resolved addresses", result.addresses.joinToString("\n"))
                ResultRow("TCP port ${result.port}", if (result.tcpReachable) "Reachable" else "Not reachable")
                result.tlsValid?.let { ResultRow("TLS verification", if (it) "Valid" else "Failed") }
                Text(result.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("Only check systems you own or have permission to inspect. This tool performs one DNS lookup and one TCP/TLS check; it does not scan ports or exploit services.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ResultCard(content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
}
