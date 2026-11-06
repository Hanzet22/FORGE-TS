package com.HG.Forge.Ts.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Row
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
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            FilterChip(
                selected = selectedTool == 0,
                onClick = { selectedTool = 0 },
                label = { Text("DNS & Protocols") }
            )
            FilterChip(
                selected = selectedTool == 1,
                onClick = { selectedTool = 1 },
                label = { Text("Repositories") },
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        if (selectedTool == 0) {
            DnsProtocolsScreen(modifier = Modifier.weight(1f))
        } else {
            RepositoryExplorerScreen(modifier = Modifier.weight(1f))
        }
    }
}
