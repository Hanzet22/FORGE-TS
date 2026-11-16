package com.HG.Forge.Ts.ui.screens

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ProotVncViewerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val pageError = remember { mutableStateOf(false) }
    val viewer = remember(context) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    return !(uri.scheme == "http" && uri.host == "localhost" && uri.port == 6080)
                }

                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    pageError.value = false
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: android.webkit.WebResourceError
                ) {
                    if (request.isForMainFrame) pageError.value = true
                }
            }
            loadUrl("http://localhost:6080/vnc.html?autoconnect=true&resize=scale")
        }
    }
    DisposableEffect(viewer) {
        onDispose { viewer.stopLoading(); viewer.destroy() }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("XFCE desktop", style = MaterialTheme.typography.titleMedium)
                        Text("Built-in VNC · enter the temporary password if prompted", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
                if (pageError.value) {
                    Text("The built-in viewer could not reach the local noVNC service. Stop and restart the desktop, then try again.", modifier = Modifier.padding(16.dp))
                } else {
                    AndroidView(factory = { viewer }, modifier = Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
    }
}
