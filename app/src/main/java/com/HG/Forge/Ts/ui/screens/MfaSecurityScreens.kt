package com.HG.Forge.Ts.ui.screens

import android.graphics.Color as AndroidColor
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.HG.Forge.Ts.data.network.HttpClientProvider
import com.HG.Forge.Ts.data.remote.ForgeAuthSessionState
import com.HG.Forge.Ts.data.remote.SupabaseAuthClient
import com.HG.Forge.Ts.data.remote.SupabaseAuthSession
import com.HG.Forge.Ts.data.remote.SupabaseMfaFactor
import com.HG.Forge.Ts.data.remote.SupabaseTotpEnrollment
import com.HG.Forge.Ts.data.sync.ForgeCrashBackendConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MfaGateStatusScreen(
    message: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (message == null) CircularProgressIndicator()
            Text("Checking account security", style = MaterialTheme.typography.titleLarge)
            Text(
                message ?: "Checking whether this account has an enrolled authenticator. Protected tools stay locked until the check completes.",
                style = MaterialTheme.typography.bodyMedium
            )
            if (message != null) {
                Button(onClick = onRetry) { Text("Retry security check") }
            }
        }
    }
}

@Composable
fun MfaChallengeScreen(
    session: SupabaseAuthSession,
    factorId: String,
    onVerified: (SupabaseAuthSession) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val client = remember {
        SupabaseAuthClient(
            ForgeCrashBackendConfig.SUPABASE_URL,
            ForgeCrashBackendConfig.PUBLISHABLE_KEY,
            HttpClientProvider.client
        )
    }
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Enter the current code from your authenticator app.") }

    Card(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Additional verification", style = MaterialTheme.typography.headlineSmall)
            Text(
                "This account has an active authenticator factor. Verify it to continue. Accounts without an enrolled factor keep the normal password sign-in flow.",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = code,
                onValueChange = { value -> code = value.filter(Char::isDigit).take(10) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Authenticator code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                enabled = !busy
            )
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        message = "Verifying authenticator code..."
                        val result = withContext(Dispatchers.IO) {
                            runCatching {
                                val challengeId = client.createMfaChallenge(session, factorId)
                                client.verifyMfaChallenge(session, factorId, challengeId, code)
                            }
                        }
                        result.onSuccess { upgraded ->
                            if (upgraded.assuranceLevel != "aal2") {
                                message = "Supabase did not issue an AAL2 session. Try again."
                            } else {
                                onVerified(upgraded)
                            }
                        }.onFailure { error ->
                            message = error.message ?: "Authenticator verification failed."
                        }
                        busy = false
                    }
                },
                enabled = !busy && code.length in 6..10,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (busy) "Verifying..." else "Verify and continue")
            }
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Keep your authenticator available. If you lose access to every enrolled factor, account recovery may require administrator assistance.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MfaSettingsSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = ForgeAuthSessionState.session
    val client = remember {
        SupabaseAuthClient(
            ForgeCrashBackendConfig.SUPABASE_URL,
            ForgeCrashBackendConfig.PUBLISHABLE_KEY,
            HttpClientProvider.client
        )
    }
    var factors by remember(session?.userId) { mutableStateOf(emptyList<SupabaseMfaFactor>()) }
    var enrollment by remember(session?.userId) { mutableStateOf<SupabaseTotpEnrollment?>(null) }
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Authenticator setup is optional.") }
    var factorToRemove by remember { mutableStateOf<SupabaseMfaFactor?>(null) }

    LaunchedEffect(session?.accessToken) {
        val active = session
        if (active == null) {
            factors = emptyList()
            status = "Sign in to manage authenticator factors."
        } else {
            val result = withContext(Dispatchers.IO) {
                runCatching { client.listMfaFactors(active) }
            }
            result.onSuccess {
                factors = it
                status = if (it.any { factor -> factor.status == "verified" }) {
                    "A verified authenticator is active. New sign-ins require its code."
                } else {
                    "No verified authenticator is active. Password sign-in remains available."
                }
            }.onFailure {
                status = it.message ?: "Unable to load authenticator factors."
            }
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Two-step verification (TOTP)", style = MaterialTheme.typography.titleLarge)
            Text(
                "Optional authenticator-app protection. Enrollment is not forced on accounts. Supabase activates a factor only after a valid code is verified.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (session == null) {
                Text(status, style = MaterialTheme.typography.bodySmall)
            } else {
                factors.forEach { factor ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(factor.friendlyName ?: "Authenticator app", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${factor.factorType.uppercase()} • ${factor.status}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(enabled = !busy, onClick = { factorToRemove = factor }) {
                            Text("Remove")
                        }
                    }
                }

                if (enrollment == null) {
                    OutlinedButton(
                        enabled = !busy,
                        onClick = {
                            scope.launch {
                                busy = true
                                val active = ForgeAuthSessionState.session
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        client.enrollTotp(requireNotNull(active), "FORGE-TS Android")
                                    }
                                }
                                result.onSuccess {
                                    enrollment = it
                                    code = ""
                                    status = "Scan the QR code or enter the setup key, then verify the first code."
                                }.onFailure {
                                    status = it.message ?: "Unable to start authenticator enrollment."
                                }
                                busy = false
                            }
                        }
                    ) { Text(if (factors.any { it.status == "verified" }) "Add another authenticator" else "Set up authenticator") }
                } else {
                    Text("Scan this QR code in your authenticator app", style = MaterialTheme.typography.titleSmall)
                    if (enrollment?.qrCodeSvg?.startsWith("<svg", ignoreCase = true) == true) {
                        AndroidView(
                            factory = { viewContext ->
                                WebView(viewContext).apply {
                                    settings.javaScriptEnabled = false
                                    settings.allowFileAccess = false
                                    settings.allowContentAccess = false
                                    isVerticalScrollBarEnabled = false
                                    isHorizontalScrollBarEnabled = false
                                    setBackgroundColor(AndroidColor.TRANSPARENT)
                                }
                            },
                            update = { webView ->
                                webView.loadDataWithBaseURL(
                                    null,
                                    enrollment!!.qrCodeSvg,
                                    "image/svg+xml",
                                    "UTF-8",
                                    null
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(210.dp)
                        )
                    }
                    Text("Manual setup key", style = MaterialTheme.typography.titleSmall)
                    SelectionContainer {
                        Text(enrollment!!.secret, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        "Treat this key like a password. It is shown only in this temporary setup screen and is not written to local storage or telemetry.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.filter(Char::isDigit).take(10) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Current 6-digit code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        enabled = !busy
                    )
                    Button(
                        enabled = !busy && code.length in 6..10,
                        onClick = {
                            scope.launch {
                                busy = true
                                val active = ForgeAuthSessionState.session
                                val setup = enrollment
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val current = requireNotNull(active)
                                        val pending = requireNotNull(setup)
                                        val challenge = client.createMfaChallenge(current, pending.factorId)
                                        client.verifyMfaChallenge(current, pending.factorId, challenge, code)
                                    }
                                }
                                result.onSuccess { upgraded ->
                                    if (upgraded.assuranceLevel == "aal2") {
                                        ForgeAuthSessionState.setSession(context, upgraded)
                                        enrollment = null
                                        code = ""
                                        status = "Authenticator verified and enabled. Your session is now AAL2."
                                    } else {
                                        status = "The factor was not promoted to AAL2. Verify the code again."
                                    }
                                }.onFailure {
                                    status = it.message ?: "Authenticator verification failed."
                                }
                                busy = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (busy) "Verifying..." else "Verify and enable") }
                    OutlinedButton(
                        enabled = !busy,
                        onClick = {
                            scope.launch {
                                busy = true
                                val active = ForgeAuthSessionState.session
                                val pending = enrollment
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        if (active != null && pending != null) {
                                            client.unenrollMfaFactor(active, pending.factorId)
                                        }
                                    }
                                }
                                result.onSuccess {
                                    enrollment = null
                                    code = ""
                                    status = "Authenticator setup cancelled."
                                }.onFailure {
                                    status = it.message ?: "Could not cancel pending enrollment."
                                }
                                busy = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Cancel setup") }
                }
            }
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    val removalTarget = factorToRemove
    if (removalTarget != null) {
        AlertDialog(
            onDismissRequest = { factorToRemove = null },
            title = { Text("Remove authenticator?") },
            text = {
                Text(
                    if (removalTarget.status == "verified")
                        "Removing this factor reduces this account to password-only authentication unless another verified factor remains."
                    else "This removes the unverified authenticator setup."
                )
            },
            confirmButton = {
                Button(
                    enabled = !busy,
                    onClick = {
                        scope.launch {
                            busy = true
                            val active = ForgeAuthSessionState.session
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    val current = requireNotNull(active)
                                    client.unenrollMfaFactor(current, removalTarget.id)
                                    client.refresh(current)
                                }
                            }
                            result.onSuccess {
                                ForgeAuthSessionState.setSession(context, it)
                                factors = factors.filterNot { factor -> factor.id == removalTarget.id }
                                factorToRemove = null
                                status = "Authenticator removed. Session assurance refreshed."
                            }.onFailure {
                                status = it.message ?: "Could not remove authenticator."
                            }
                            busy = false
                        }
                    }
                ) { Text("Remove factor") }
            },
            dismissButton = {
                TextButton(onClick = { factorToRemove = null }, enabled = !busy) { Text("Keep") }
            }
        )
    }
}
