package com.HG.Forge.Ts.ui

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.HG.Forge.Ts.R
import com.HG.Forge.Ts.ui.screens.HomeScreen
import com.HG.Forge.Ts.ui.screens.KnowledgeCenterScreen
import com.HG.Forge.Ts.ui.screens.CoreNetworkingScreen
import com.HG.Forge.Ts.ui.screens.ToolsScreen
import com.HG.Forge.Ts.ui.screens.SimulationScreen
import com.HG.Forge.Ts.ui.screens.SettingsScreen

@Composable
fun ForgeApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedToolsSubtab by remember { mutableIntStateOf(0) }
    var showIntro by rememberSaveable { mutableStateOf(true) }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") }, label = { Text("Home") })
                    NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Outlined.MenuBook, contentDescription = "Learn") }, label = { Text("Learn") })
                    NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Outlined.Calculate, contentDescription = "Network") }, label = { Text("Network") })
                    NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Icon(Icons.Outlined.Science, contentDescription = "Simulate") }, label = { Text("Simulate") })
                    NavigationBarItem(selected = selectedTab == 4, onClick = { selectedTab = 4 }, icon = { Icon(Icons.Outlined.Build, contentDescription = "Tools") }, label = { Text("Tools") })
                    NavigationBarItem(selected = selectedTab == 5, onClick = { selectedTab = 5 }, icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }, label = { Text("Settings") })
                }
            }
        ) { padding ->
            when (selectedTab) {
                0 -> HomeScreen(
                    modifier = Modifier.padding(padding),
                    onToolSelected = { destinationTab, toolsSubtab ->
                        selectedToolsSubtab = toolsSubtab
                        selectedTab = destinationTab
                    }
                )
                1 -> KnowledgeCenterScreen(Modifier.padding(padding))
                2 -> CoreNetworkingScreen(Modifier.padding(padding))
                3 -> SimulationScreen(Modifier.padding(padding))
                4 -> ToolsScreen(modifier = Modifier.padding(padding), initialSelectedTool = selectedToolsSubtab)
                else -> SettingsScreen(Modifier.padding(padding))
            }
        }

        if (showIntro) {
            AndroidView(
                factory = { context ->
                    VideoView(context).apply {
                        setBackgroundColor(android.graphics.Color.BLACK)
                        setVideoURI(Uri.parse("android.resource://${context.packageName}/${R.raw.intro_video}"))
                        setOnPreparedListener { player ->
                            player.isLooping = false
                            start()
                        }
                        setOnCompletionListener { showIntro = false }
                        setOnErrorListener { _, _, _ ->
                            showIntro = false
                            true
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            TextButton(
                onClick = { showIntro = false },
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
            ) {
                Text("Skip intro")
            }
        }
    }
}
