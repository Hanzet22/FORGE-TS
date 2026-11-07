package com.HG.Forge.Ts.ui.screens

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.HG.Forge.Ts.data.network.VlsmCalculator
import com.HG.Forge.Ts.data.network.VlsmRequest

@Composable
fun VlsmPlannerScreen(modifier: Modifier = Modifier) {
    var base by remember { mutableStateOf("192.168.10.0/24") }
    var requirements by remember { mutableStateOf("Office,50\nLaboratory,25\nWLAN,20\nManagement,10") }
    var allocations by remember { mutableStateOf(emptyList<com.HG.Forge.Ts.data.network.VlsmAllocation>()) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("VLSM subnet planner", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Allocate different subnet sizes from one IPv4 network. The planner sorts requests from largest to smallest and aligns each subnet on its boundary.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(base, { base = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Base IPv4 network / prefix") }, singleLine = true)
        OutlinedTextField(
            requirements, { requirements = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Subnet name, required hosts (one per line)") },
            minLines = 4
        )
        Button(
            onClick = {
                try {
                    val parsed = requirements.lines().filter { it.isNotBlank() }.mapIndexed { index, line ->
                        val parts = line.split(",")
                        require(parts.size == 2) { "Line ${index + 1}: use Name,HostCount" }
                        VlsmRequest(parts[0].trim(), parts[1].trim().toIntOrNull()
                            ?: throw IllegalArgumentException("Line ${index + 1}: host count must be a number"))
                    }
                    allocations = VlsmCalculator.allocate(base, parsed)
                    error = null
                } catch (e: Exception) {
                    allocations = emptyList()
                    error = e.message ?: "Unable to allocate subnets"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Allocate VLSM subnets") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        allocations.forEach { item ->
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(item.cidr, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    PlannerRow("Network", item.network)
                    PlannerRow("First host", item.firstHost)
                    PlannerRow("Last host", item.lastHost)
                    PlannerRow("Broadcast", item.broadcast)
                    PlannerRow("Requested / available", "${item.requestedHosts} / ${item.availableHosts}")
                }
            }
        }
        Text("IPv4 VLSM reserves network and broadcast addresses for ordinary subnets. /31 point-to-point links are not allocated by this classroom planner. All calculations stay on-device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun NetworkIpAllocationScreen(modifier: Modifier = Modifier) {
    val types = listOf("PAN", "LAN", "WLAN", "VLAN")
    var selected by remember { mutableStateOf("LAN") }
    var devices by remember { mutableStateOf("30") }
    var reserve by remember { mutableStateOf("5") }
    var growth by remember { mutableStateOf("20") }

    val deviceCount = devices.toIntOrNull()?.coerceAtLeast(0)
    val reserveCount = reserve.toIntOrNull()?.coerceAtLeast(0)
    val growthPercent = growth.toIntOrNull()?.coerceIn(0, 500)
    val required = if (deviceCount != null && reserveCount != null && growthPercent != null) {
        deviceCount + reserveCount + ((deviceCount.toLong() * growthPercent + 99) / 100).toInt()
    } else null
    val prefix = required?.let { count ->
        if (count <= 0) null else {
            val blockBits = (count + 2 - 1).coerceAtLeast(4).let { value -> 32 - Integer.numberOfLeadingZeros(value - 1) }
            (32 - blockBits).coerceIn(0, 30)
        }
    }
    val addressCount = prefix?.let { 1L shl (32 - it) }
    val usable = prefix?.let { (1L shl (32 - it)) - 2 }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Network IP allocation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("PAN, LAN, WLAN and VLAN do not have fixed IP quotas. Estimate each segment from its actual device count, reserved addresses and growth.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            types.forEach { type ->
                FilterChip(selected = selected == type, onClick = { selected = type }, label = { Text(type) })
            }
        }
        Text(networkTypeDescription(selected), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(devices, { devices = it.filter(Char::isDigit).take(7) }, modifier = Modifier.fillMaxWidth(), label = { Text("Expected client devices") }, singleLine = true)
        OutlinedTextField(reserve, { reserve = it.filter(Char::isDigit).take(7) }, modifier = Modifier.fillMaxWidth(), label = { Text("Reserved IPs (gateway, printer, AP, etc.)") }, singleLine = true)
        OutlinedTextField(growth, { growth = it.filter(Char::isDigit).take(3) }, modifier = Modifier.fillMaxWidth(), label = { Text("Growth allowance (%)") }, singleLine = true)
        if (required != null && prefix != null && addressCount != null && usable != null) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$selected IPv4 estimate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("/$prefix", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    PlannerRow("Capacity target", "$required addresses")
                    PlannerRow("Total block", "$addressCount addresses")
                    PlannerRow("Usable hosts", "$usable")
                    PlannerRow("Example subnet mask", maskForPrefix(prefix))
                }
            }
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("IPv6 planning note", fontWeight = FontWeight.SemiBold)
                    Text("For a typical IPv6 LAN or VLAN segment, /64 is the conventional subnet size and supports SLAAC. Do not size an IPv6 subnet by subtracting network and broadcast addresses; IPv6 has no broadcast address.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            Text("Enter valid non-negative numbers to calculate an estimate.", color = MaterialTheme.colorScheme.error)
        }
        Text("Planning estimate only. A PAN may use link-local or non-IP personal-area technologies; a VLAN is a logical Layer 2 segment and typically receives its own Layer 3 subnet when routed. Actual allocation must follow the network design and existing address plan.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PlannerRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

private fun networkTypeDescription(type: String): String = when (type) {
    "PAN" -> "Personal Area Network: a short-range network around an individual. IP assignment depends on the devices and technology involved; not every PAN device needs a separately routed IPv4 address."
    "LAN" -> "Local Area Network: a local wired segment. Plan addresses for clients, gateways, printers, servers and infrastructure, with room for growth."
    "WLAN" -> "Wireless LAN: Wi-Fi clients and access-point management. Separate guest, staff and infrastructure networks when policy or scale requires it."
    else -> "Virtual LAN: a logical Layer 2 segment. A routed VLAN commonly maps to its own IP subnet and default gateway; multiple VLANs should not share one subnet by default."
}

private fun maskForPrefix(prefix: Int): String {
    val mask = if (prefix == 0) 0 else (-1 shl (32 - prefix))
    return listOf(24, 16, 8, 0).joinToString(".") { shift -> ((mask ushr shift) and 255).toString() }
}
