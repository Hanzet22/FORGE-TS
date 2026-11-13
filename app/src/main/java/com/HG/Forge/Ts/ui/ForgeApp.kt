package com.HG.Forge.Ts.ui

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.HG.Forge.Ts.R
import com.HG.Forge.Ts.AuthDeepLinkBus
import com.HG.Forge.Ts.data.sync.ForgeDiagnostics
import com.HG.Forge.Ts.data.sync.CrashReportUploader
import com.HG.Forge.Ts.data.remote.ForgeAuthSessionState
import com.HG.Forge.Ts.data.remote.SupabaseAuthClient
import com.HG.Forge.Ts.data.network.HttpClientProvider
import com.HG.Forge.Ts.data.sync.ForgeCrashBackendConfig
import com.HG.Forge.Ts.ui.screens.AuthGateScreen
import com.HG.Forge.Ts.ui.screens.HomeScreen
import com.HG.Forge.Ts.ui.screens.KnowledgeCenterScreen
import com.HG.Forge.Ts.ui.screens.CoreNetworkingScreen
import com.HG.Forge.Ts.ui.screens.ToolsScreen
import com.HG.Forge.Ts.ui.screens.SimulationScreen
import com.HG.Forge.Ts.ui.screens.SettingsScreen

@Composable
fun ForgeApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var navigationTraceId by remember { mutableStateOf<String?>(null) }
    var selectedToolsSubtab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val authClient = remember(context) {
        SupabaseAuthClient(
            ForgeCrashBackendConfig.SUPABASE_URL,
            ForgeCrashBackendConfig.PUBLISHABLE_KEY,
            HttpClientProvider.client
        )
    }
    val authSession = ForgeAuthSessionState.session
    val passwordRecoveryPending = ForgeAuthSessionState.passwordRecoveryPending
    val nowEpochSeconds = System.currentTimeMillis() / 1000L
    val hasActiveSession = authSession?.expiresAtEpochSeconds?.let { it > nowEpochSeconds } == true
    val tabNames = listOf("home", "learn", "network", "simulate", "tools", "settings")
    fun navigateToTab(destination: Int) {
        val safeDestination = destination.coerceIn(0, 5)
        val traceId = ForgeDiagnostics.newTraceId()
        navigationTraceId = traceId
        ForgeDiagnostics.log(
            context,
            "INFO",
            "Navigation",
            "navigation.tab_selected.${tabNames[safeDestination]}",
            traceId = traceId
        )
        selectedTab = safeDestination
    }

    LaunchedEffect(authClient) {
        ForgeAuthSessionState.restore(context.applicationContext, authClient)
        AuthDeepLinkBus.callbacks.collect { callbackUrl ->
            val result = runCatching {
                withContext(Dispatchers.IO) { authClient.sessionFromCallback(callbackUrl) }
            }
            result.onSuccess {
                ForgeAuthSessionState.setSession(context, it)
                if (authClient.isPasswordRecoveryCallback(callbackUrl)) {
                    ForgeAuthSessionState.beginPasswordRecovery(context)
                }
            }.onFailure { Log.w("FORGE-TS Auth", "Supabase callback processing failed", it) }
        }
    }

    LaunchedEffect(authSession?.accessToken) {
        val accessToken = authSession?.accessToken ?: return@LaunchedEffect
        val upload = withContext(Dispatchers.IO) {
            runCatching { CrashReportUploader.uploadPending(context.applicationContext, accessToken) }
        }
        upload.onSuccess { status ->
            if (status != null) Log.i("FORGE-TS CrashSync", "Pending crash reports uploaded: $status")
        }.onFailure { error ->
            Log.w("FORGE-TS CrashSync", "Pending crash report upload deferred", error)
        }
    }

    LaunchedEffect(authSession?.accessToken, authSession?.expiresAtEpochSeconds) {
        val active = authSession ?: return@LaunchedEffect
        while (ForgeAuthSessionState.session?.accessToken == active.accessToken) {
            val secondsUntilRefresh = active.expiresAtEpochSeconds -
                System.currentTimeMillis() / 1000L - 45L
            if (secondsUntilRefresh > 0L) delay(secondsUntilRefresh * 1000L)

            val current = ForgeAuthSessionState.session ?: break
            val refreshed = runCatching {
                withContext(Dispatchers.IO) { authClient.validSession(current) }
            }
            if (refreshed.isSuccess) {
                ForgeAuthSessionState.setSession(context, refreshed.getOrThrow())
                break
            }

            val secondsRemaining = current.expiresAtEpochSeconds -
                System.currentTimeMillis() / 1000L
            if (secondsRemaining <= 0L) break
            delay(minOf(60L, secondsRemaining) * 1000L)
        }
    }
    var showIntro by rememberSaveable { mutableStateOf(true) }
    val view = LocalView.current
    val window = (view.context as? Activity)?.window

    LaunchedEffect(Unit) {
        ForgeDiagnostics.log(view.context, "INFO", "ForgeApp", "forge_app.compose_entered")
    }

    LaunchedEffect(selectedTab, navigationTraceId) {
        ForgeDiagnostics.log(
            context,
            "INFO",
            "Navigation",
            "navigation.tab_entered.${tabNames[selectedTab]}",
            traceId = navigationTraceId
        )
    }

    SideEffect {
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, !showIntro)
            val insetsController = WindowInsetsControllerCompat(window, window.decorView)
            if (showIntro) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = selectedTab == 0, onClick = { navigateToTab(0) }, icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") }, label = { Text("Home") })
                    NavigationBarItem(selected = selectedTab == 1, onClick = { navigateToTab(1) }, icon = { Icon(Icons.Outlined.MenuBook, contentDescription = "Learn") }, label = { Text("Learn") })
                    NavigationBarItem(selected = selectedTab == 2, onClick = { navigateToTab(2) }, icon = { Icon(Icons.Outlined.Calculate, contentDescription = "Network") }, label = { Text("Network") })
                    NavigationBarItem(selected = selectedTab == 3, onClick = { navigateToTab(3) }, icon = { Icon(Icons.Outlined.Science, contentDescription = "Simulate") }, label = { Text("Simulate") })
                    NavigationBarItem(selected = selectedTab == 4, onClick = { navigateToTab(4) }, icon = { Icon(Icons.Outlined.Build, contentDescription = "Tools") }, label = { Text("Tools") })
                    NavigationBarItem(selected = selectedTab == 5, onClick = { navigateToTab(5) }, icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }, label = { Text("Settings") })
                }
            }
        ) { padding ->
            when (selectedTab) {
                0 -> HomeScreen(
                    modifier = Modifier.padding(padding),
                    onToolSelected = { destinationTab, toolsSubtab ->
                        selectedToolsSubtab = toolsSubtab
                        navigateToTab(destinationTab)
                    }
                )
                1 -> KnowledgeCenterScreen(Modifier.padding(padding))
                2 -> CoreNetworkingScreen(Modifier.padding(padding))
                3 -> if (!hasActiveSession) {
                    AuthGateScreen(Modifier.padding(padding), "Simulate", onAuthenticated = {})
                } else {
                    Column(Modifier.padding(padding).fillMaxSize()) {
                        AuthenticatedAccessBanner()
                        SimulationScreen(Modifier.weight(1f))
                    }
                }
                4 -> if (!hasActiveSession) {
                    AuthGateScreen(Modifier.padding(padding), "Tools", onAuthenticated = {})
                } else {
                    Column(Modifier.padding(padding).fillMaxSize()) {
                        AuthenticatedAccessBanner()
                        ToolsScreen(Modifier.weight(1f), selectedToolsSubtab)
                    }
                }
                else -> SettingsScreen(Modifier.padding(padding))
            }
        }

        if (showIntro) {
            IntroVideoTextureViewHost(
                onFinished = { showIntro = false },
                modifier = Modifier.fillMaxSize()
            )
            TextButton(
                onClick = { showIntro = false },
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
            ) {
                Text("Skip intro")
            }
        }

        if (passwordRecoveryPending) {
            AuthGateScreen(
                modifier = Modifier.fillMaxSize(),
                requestedArea = "Password recovery",
                onAuthenticated = {},
                recoveryMode = true
            )
        }
    }
}

@Composable
private fun IntroVideoTextureViewHost(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            IntroVideoTextureView(context, R.raw.intro_video, onFinished)
        },
        modifier = modifier
    )
}


@Composable
private fun AuthenticatedAccessBanner() {
    val session = ForgeAuthSessionState.session ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authClient = remember {
        SupabaseAuthClient(
            ForgeCrashBackendConfig.SUPABASE_URL,
            ForgeCrashBackendConfig.PUBLISHABLE_KEY,
            HttpClientProvider.client
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("AUTHENTICATED • ${session.email ?: session.userId}", style = MaterialTheme.typography.labelMedium)
            Text("Simulate & Tools access enabled", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = {
            scope.launch {
                val active = ForgeAuthSessionState.session ?: return@launch
                withContext(Dispatchers.IO) { runCatching { authClient.signOut(active) } }
                if (ForgeAuthSessionState.session?.accessToken == active.accessToken) {
                    ForgeAuthSessionState.clearSession(context)
                }
            }
        }) { Text("Sign out") }
    }
}
