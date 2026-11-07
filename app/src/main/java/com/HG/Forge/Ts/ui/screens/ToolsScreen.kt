package com.HG.Forge.Ts.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun ToolsScreen(modifier: Modifier = Modifier, initialSelectedTool: Int = 0) {
    var selectedTool by remember(initialSelectedTool) { mutableIntStateOf(initialSelectedTool.coerceIn(0, 4)) }
    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedTool) {
            listOf("DNS & Protocols", "Repositories", "Security", "Diagnostics", "OSINT Web").forEachIndexed { index, title ->
                Tab(selected = selectedTool == index, onClick = { selectedTool = index }, text = { Text(title) })
            }
        }
        when (selectedTool) {
            0 -> DnsProtocolsScreen(modifier = Modifier.weight(1f))
            1 -> RepositoryExplorerScreen(modifier = Modifier.weight(1f))
            2 -> SecurityPrivacyScreen(modifier = Modifier.weight(1f))
            3 -> DiagnosticsScreen(modifier = Modifier.weight(1f))
            else -> OsintWebScreen(modifier = Modifier.weight(1f))
        }
    }
}
