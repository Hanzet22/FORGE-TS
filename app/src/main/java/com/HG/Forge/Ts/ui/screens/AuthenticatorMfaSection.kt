package com.HG.Forge.Ts.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.HG.Forge.Ts.data.network.HttpClientProvider
import com.HG.Forge.Ts.data.remote.ForgeAuthSessionState
import com.HG.Forge.Ts.data.remote.SupabaseAuthClient
import com.HG.Forge.Ts.data.remote.SupabaseAuthException
import com.HG.Forge.Ts.data.remote.SupabaseTotpEnrollment
import com.HG.Forge.Ts.data.sync.ForgeCrashBackendConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AuthenticatorMfaSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = ForgeAuthSessionState.session
    val authClient = remember {
        SupabaseAuthClient(
            ForgeCrashBackendConfig.SUPABASE_URL,
            ForgeCrashBackendConfig.PUBLISHABLE_KEY,
            HttpClientProvider.client
        )
    }
    var factors by remember(session?.userId) { mutableStateOf(emptyList<com.HG.Forge.Ts.data.remote.SupabaseTotpFactor>()) }
    var enrollment by remember { mutableStateOf<SupabaseTotpEnrollment?>(null) }
    var challengeId by remember { mutableStateOf<String?>(null) }
    var code by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Authenticator MFA is optional. Enable it to protect your FORGE-TS account.") }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(session?.accessToken) {
        val active = session ?: run {
            factors = emptyList()
            enrollment = null
            challengeId = null
            status = "Sign in to manage authenticator MFA."
            return@LaunchedEffect
        }
        val result = withContext(Dispatchers.IO) {
            runCatching { authClient.listTotpFactors(active) }
        }
        result.onSuccess { factors = it }
            .onFailure { status = it.message ?: "Unable to load authenticator factors." }
    }

    val verified = factors.filter { it.status.equals("verified", ignoreCase = true) }
    val pending = factors.filterNot { it.status.equals("verified", ignoreCase = true) }

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Authenticator MFA", style = MaterialTheme.typography.titleLarge)
            Text(
                if (verified.isNotEmpty()) "Enabled • TOTP authenticator is verified."
                else "Disabled • No verified authenticator is linked.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (session == null) {
                Text("Sign in first to configure MFA.", style = MaterialTheme.typography.bodySmall)
            } else if (enrollment == null) {
                if (verified.isNotEmpty()) {
                    verified.forEach { factor ->
                        Text(
                            "Authenticator: ${factor.friendlyName ?: "TOTP"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedButton(
                            enabled = !busy,
                            onClick = {
                                scope.launch {
                                    busy = true
                                    val current = ForgeAuthSessionState.session
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching {
                                            authClient.unenrollTotp(requireNotNull(current), factor.id)
                                            authClient.refresh(requireNotNull(current))
                                        }
                                    }
                                    result.onSuccess {
                                        ForgeAuthSessionState.setSession(context, it)
                                        factors = authClient.listTotpFactors(it)
                                        status = "Authenticator MFA disabled."
                                    }.onFailure {
                                        status = it.message ?: "Unable to disable MFA."
                                    }
                                    busy = false
                                }
                            }
                        ) { Text("Disable authenticator") }
                    }
                } else {
                    Button(
                        enabled = !busy,
                        onClick = {
                            scope.launch {
                                busy = true
                                status = "Preparing authenticator enrollment…"
                                val current = ForgeAuthSessionState.session
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val active = requireNotNull(current)
                                        val created = authClient.enrollTotp(active)
                                        val challenge = authClient.createTotpChallenge(active, created.factorId)
                                        created to challenge
                                    }
                                }
                                result.onSuccess {
                                    enrollment = it.first
                                    challengeId = it.second
                                    code = ""
                                    status = "Scan the QR code or copy the setup key into your authenticator app."
                                }.onFailure {
                                    status = it.message ?: "Authenticator enrollment failed."
                                }
                                busy = false
                            }
                        }
                    ) { Text(if (busy) "Please wait…" else "Enable authenticator MFA") }
                }

                pending.forEach { factor ->
                    Text(
                        "Pending setup: ${factor.friendlyName ?: "TOTP"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        enabled = !busy,
                        onClick = {
                            scope.launch {
                                busy = true
                                val current = ForgeAuthSessionState.session
                                val result = withContext(Dispatchers.IO) {
                                    runCatching { authClient.unenrollTotp(requireNotNull(current), factor.id) }
                                }
                                result.onSuccess {
                                    factors = authClient.listTotpFactors(requireNotNull(current))
                                    status = "Incomplete authenticator setup removed."
                                }.onFailure { status = it.message ?: "Unable to remove pending setup." }
                                busy = false
                            }
                        }
                    ) { Text("Remove incomplete setup") }
                }
            } else {
                Text("Scan this QR code with your authenticator app.", style = MaterialTheme.typography.bodyMedium)
                if (enrollment?.qrCodeSvg?.isNotBlank() == true) {
                    AndroidView(
                        factory = { viewContext ->
                            WebView(viewContext).apply {
                                settings.javaScriptEnabled = false
                                settings.domStorageEnabled = false
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                            }
                        },
                        update = { webView ->
                            val svg = enrollment?.qrCodeSvg.orEmpty()
                            webView.loadDataWithBaseURL(
                                null,
                                "<html><meta name='viewport' content='width=device-width, initial-scale=1'><body style='margin:0;display:flex;justify-content:center;align-items:center;background:white;'>$svg</body></html>",
                                "text/html",
                                "UTF-8",
                                null
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                }
                Text("Can't scan? Enter this setup key manually:", style = MaterialTheme.typography.bodyMedium)
                Text(
                    enrollment?.secret.orEmpty(),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = !busy,
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("FORGE-TS TOTP setup key", enrollment?.secret.orEmpty()))
                            status = "Setup key copied. Keep it private."
                        }
                    ) { Text("Copy setup key") }
                    OutlinedButton(
                        enabled = !busy,
                        onClick = {
                            enrollment = null
                            challengeId = null
                            code = ""
                            status = "Enrollment cancelled. Remove any pending factor if you want to restart."
                        }
                    ) { Text("Cancel") }
                }
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter(Char::isDigit).take(6) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("6-digit authenticator code") },
                    singleLine = true,
                    enabled = !busy
                )
                Button(
                    enabled = !busy && code.length == 6 && challengeId != null,
                    onClick = {
                        scope.launch {
                            busy = true
                            val current = ForgeAuthSessionState.session
                            val activeEnrollment = enrollment
                            val activeChallenge = challengeId
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    authClient.verifyTotpChallenge(
                                        requireNotNull(current),
                                        requireNotNull(activeEnrollment).factorId,
                                        requireNotNull(activeChallenge),
                                        code
                                    )
                                }
                            }
                            result.onSuccess {
                                ForgeAuthSessionState.setSession(context, it)
                                enrollment = null
                                challengeId = null
                                code = ""
                                factors = authClient.listTotpFactors(it)
                                status = "Authenticator MFA enabled. This session has completed second-factor verification."
                            }.onFailure {
                                status = it.message ?: "Authenticator verification failed. Check the code and try again."
                            }
                            busy = false
                        }
                    }
                ) { Text(if (busy) "Verifying…" else "Verify and enable MFA") }
            }

            Text(
                "The setup key is shown only during enrollment. FORGE-TS does not store it in Redis, diagnostics or local preferences.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
