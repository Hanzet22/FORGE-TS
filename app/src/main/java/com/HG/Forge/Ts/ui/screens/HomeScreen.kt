package com.HG.Forge.Ts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class ToolItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val destinationTab: Int,
    val toolsSubtab: Int = 0
)

private val tools = listOf(
    ToolItem("CIDR Calculator", "IPv4 and IPv6 subnet planning", Icons.Outlined.Calculate, 2),
    ToolItem("MikroTik Guide", "RouterOS learning references", Icons.Outlined.Router, 1),
    ToolItem("OS Commands", "Command reference library", Icons.Outlined.Terminal, 1),
    ToolItem("Host Checker", "Checks for authorized hosts", Icons.Outlined.Hub, 2),
    ToolItem("What Is My IP", "Public network information", Icons.Outlined.Language, 2),
    ToolItem("DNS Lists", "DNS records and resolver references", Icons.Outlined.Dns, 3, 0),
    ToolItem("SSH & TLS", "Secure protocol references", Icons.Outlined.Security, 3, 0),
    ToolItem("Repositories", "Explore public source repositories", Icons.Outlined.Source, 3, 1)
)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onToolSelected: (destinationTab: Int, toolsSubtab: Int) -> Unit = { _, _ -> }
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("FORGE-TS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("NETWORK & SYSTEMS TOOLKIT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.padding(20.dp)) {
                    Text("YOUR SYSTEMS WORKSPACE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(8.dp))
                    Text("Tools for your network workflow.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(6.dp))
                    Text("Utilities, system references and diagnostics in one place.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("CORE TOOLS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("8 MODULES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().height(640.dp),
                contentPadding = PaddingValues(0.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {
                items(tools) { tool ->
                    Card(
                        onClick = { onToolSelected(tool.destinationTab, tool.toolsSubtab) },
                        modifier = Modifier.fillMaxWidth().height(151.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Box(Modifier.background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)).padding(9.dp)) {
                                Icon(tool.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            Text(tool.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(tool.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
