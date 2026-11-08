package com.HG.Forge.Ts

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.HG.Forge.Ts.ui.ForgeApp
import com.HG.Forge.Ts.ui.theme.ForgeTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ForgeTheme {
                Surface(Modifier.fillMaxSize()) { ForgeApp() }
            }
        }
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
