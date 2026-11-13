package com.HG.Forge.Ts

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.HG.Forge.Ts.data.sync.CrashReportStore
import com.HG.Forge.Ts.data.sync.ForgeDiagnostics
import com.HG.Forge.Ts.data.preferences.AppPreferencesRepository
import com.HG.Forge.Ts.data.preferences.ForgeThemeMode
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.HG.Forge.Ts.ui.ForgeApp
import com.HG.Forge.Ts.ui.DiagnosticConsentGate
import com.HG.Forge.Ts.ui.theme.ForgeTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Route to a platform-View-only recovery screen before Compose or app UI starts.
        val pendingCrash = CrashReportStore.read(this)
        if (pendingCrash != null && !pendingCrash.optBoolean("recoveryAcknowledged")) {
            startActivity(Intent(this, CrashRecoveryActivity::class.java))
            finish()
            return
        }

        ForgeDiagnostics.log(this, "INFO", "MainActivity", "activity.on_create.enter")
        setContent {
            val preferences = remember { AppPreferencesRepository(applicationContext) }
            val themeMode by preferences.themeMode.collectAsState(initial = ForgeThemeMode.DYNAMIC_COLOR)
            ForgeTheme(themeMode = themeMode) {
                Surface(Modifier.fillMaxSize()) {
                    ForgeApp()
                    DiagnosticConsentGate()
                }
            }
        }
        ForgeDiagnostics.log(this, "INFO", "MainActivity", "activity.compose_content_set")
        intent.dataString?.let(::publishAuthCallback)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.let(::publishAuthCallback)
    }

    private fun publishAuthCallback(uri: String) {
        lifecycleScope.launch { AuthDeepLinkBus.publish(uri) }
    }
}
