package com.HG.Forge.Ts.ui
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.HG.Forge.Ts.ui.screens.HomeScreen
import com.HG.Forge.Ts.ui.screens.SettingsScreen
@Composable
fun ForgeApp() {
 var selectedTab by remember { mutableIntStateOf(0) }
 Scaffold(bottomBar = {
  NavigationBar {
   NavigationBarItem(selectedTab == 0, { selectedTab = 0 }, { Icon(Icons.Outlined.Home, "Home") }, label = { Text("Home") })
   NavigationBarItem(selectedTab == 1, { selectedTab = 1 }, { Icon(Icons.Outlined.Settings, "Settings") }, label = { Text("Settings") })
  }
 }) { padding ->
  if (selectedTab == 0) HomeScreen(Modifier.padding(padding)) else SettingsScreen(Modifier.padding(padding))
 }
}
