package com.HG.Forge.Ts.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ToolsScreen(modifier: Modifier = Modifier) {
    var selectedTool by remember { mutableIntStateOf(0) }
    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedTool) {
            listOf("DNS & Protocols", "Repositories", "Security", "Diagnostics").forEachIndexed { index, title ->
                Tab(
                    selected = selectedTool == index,
                    onClick = { selectedTool = index },
                    text = { Text(title) }
                )
            }
        }
        when (selectedTool) {
            0 -> DnsProtocolsScreen(modifier = Modifier.weight(1f))
            1 -> RepositoryExplorerScreen(modifier = Modifier.weight(1f))
            2 -> SecurityPrivacyScreen(modifier = Modifier.weight(1f))
            else -> DiagnosticsScreen(modifier = Modifier.weight(1f))
        }
    }
}
