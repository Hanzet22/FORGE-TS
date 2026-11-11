package com.HG.Forge.Ts.ui.screens

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.HG.Forge.Ts.R
import com.HG.Forge.Ts.data.preferences.AppPreferencesRepository
import com.HG.Forge.Ts.data.network.HttpClientProvider
import com.HG.Forge.Ts.data.remote.ForgeAuthSessionState
import com.HG.Forge.Ts.data.remote.SupabaseAuthClient
import com.HG.Forge.Ts.data.remote.SupabaseAuthException
import com.HG.Forge.Ts.data.remote.SupabaseAuthUserProfile
import com.HG.Forge.Ts.data.sync.ForgeCrashBackendConfig
import com.HG.Forge.Ts.data.sync.ForgeDiagnosticsConsent
import com.HG.Forge.Ts.ui.components.PasswordVisibilityField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember(context) { AppPreferencesRepository(context) }
    val scope = rememberCoroutineScope()
    val defaultType by repository.defaultDnsRecordType.collectAsState(initial = "A")
    var showAboutApps by rememberSaveable { mutableStateOf(false) }
    var diagnosticsUploadsEnabled by remember { mutableStateOf(ForgeDiagnosticsConsent.isEnabled(context)) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text("Local preferences are stored on this device.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AccountLifecycleSection()
        Text("Default DNS record type", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("A", "AAAA", "MX", "TXT").forEach { type ->
                FilterChip(
                    selected = defaultType == type,
                    onClick = { scope.launch { repository.setDefaultDnsRecordType(type) } },
                    label = { Text(type) }
                )
            }
        }
        Text("The selected record type is saved locally using Android DataStore.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Crash recovery & diagnostics", style = MaterialTheme.typography.titleMedium)
        Text(
            "Local technical logging starts automatically whenever FORGE-TS opens. Remote upload is optional and only runs after permission is granted. Uploaded diagnostics can include app/Android versions, a pseudonymous installation ID, event labels and technical exception traces. They exclude passwords, terminal input/output, message contents and network payloads. You can turn remote uploads off at any time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Automatic remote diagnostics", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (diagnosticsUploadsEnabled) "Uploads enabled when connected"
                    else "On-device logging only",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = diagnosticsUploadsEnabled,
                onCheckedChange = { enabled ->
                    ForgeDiagnosticsConsent.setEnabled(context, enabled)
                    diagnosticsUploadsEnabled = enabled
                }
            )
        }
        Text(
            "Admin email destination is not configured in the backend yet. Remote events are currently stored in the FORGE-TS Supabase diagnostics service.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = { showAboutApps = true }) { Text("About Apps") }
        Text("FORGE-TS 0.1.0", style = MaterialTheme.typography.labelMedium)
    }

    if (showAboutApps) {
        AlertDialog(
            onDismissRequest = { showAboutApps = false },
            title = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(
                        painter = painterResource(R.mipmap.forge_ts_logo),
                        contentDescription = "FORGE-TS app logo",
                        modifier = Modifier.size(44.dp)
                    )
                    Text("About FORGE-TS", style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("FORGE-TS • Version 0.1.0")
                    AndroidView(
                        factory = { viewContext ->
                            VideoView(viewContext).apply {
                                setVideoURI(Uri.parse("android.resource://${viewContext.packageName}/${R.raw.about_apps}"))
                                setOnPreparedListener { player ->
                                    player.isLooping = false
                                    start()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                    )
                }
            },
            confirmButton = { Button(onClick = { showAboutApps = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun AccountLifecycleSection() {
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
    var profile by remember(session?.userId) { mutableStateOf<SupabaseAuthUserProfile?>(null) }
    var status by remember { mutableStateOf("Account status is checked when a session is available.") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val validPassword = newPassword.length >= 20 &&
        newPassword.any(Char::isLowerCase) &&
        newPassword.any(Char::isUpperCase) &&
        newPassword.any(Char::isDigit) &&
        newPassword.any { !it.isLetterOrDigit() }

    LaunchedEffect(session?.accessToken) {
        val active = session ?: run {
            profile = null
            status = "No account is currently signed in on this device."
            return@LaunchedEffect
        }
        val result = withContext(Dispatchers.IO) { runCatching { authClient.getUserProfile(active) } }
        result.onSuccess {
            profile = it
            status = if (it.isEmailVerified) "Account profile is up to date." else "Email verification is still pending."
        }.onFailure { error ->
            status = error.message ?: "Unable to load account profile."
            if (error is SupabaseAuthException && error.statusCode in setOf(401, 403)) {
                ForgeAuthSessionState.clearSession(context)
                status = "Session is no longer valid. Sign in again."
            }
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Account & Session", style = MaterialTheme.typography.titleLarge)
            if (session == null) {
                Text("Not signed in", style = MaterialTheme.typography.titleMedium)
                Text("Open Simulate or Tools to create an account or sign in.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("Email: ${profile?.email ?: session.email ?: "Unavailable"}", style = MaterialTheme.typography.bodyMedium)
                Text("User ID: ${session.userId}", style = MaterialTheme.typography.bodySmall)
                Text("Email status: ${profile?.let { if (it.isEmailVerified) "Verified" else "Not verified" } ?: "Checking…"}", style = MaterialTheme.typography.bodyMedium)
                Text("Session expires: ${Date(session.expiresAtEpochSeconds * 1000L)}", style = MaterialTheme.typography.bodySmall)
                profile?.createdAt?.let { Text("Account created: $it", style = MaterialTheme.typography.bodySmall) }
                profile?.lastSignInAt?.let { Text("Last sign-in: $it", style = MaterialTheme.typography.bodySmall) }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = !busy, onClick = {
                        scope.launch {
                            busy = true
                            val current = ForgeAuthSessionState.session
                            val result = withContext(Dispatchers.IO) {
                                runCatching { authClient.refresh(requireNotNull(current)) }
                            }
                            result.onSuccess {
                                ForgeAuthSessionState.setSession(context, it)
                                status = "Session refreshed."
                            }.onFailure { error ->
                                status = error.message ?: "Session refresh failed."
                                if (error is SupabaseAuthException && error.statusCode in setOf(400, 401, 403)) {
                                    ForgeAuthSessionState.clearSession(context)
                                    status = "Refresh token is invalid or expired. Sign in again."
                                }
                            }
                            busy = false
                        }
                    }) { Text("Refresh session") }

                    OutlinedButton(enabled = !busy, onClick = {
                        scope.launch {
                            busy = true
                            val current = ForgeAuthSessionState.session
                            val result = withContext(Dispatchers.IO) {
                                runCatching { authClient.signOut(requireNotNull(current)) }
                            }
                            val code = (result.exceptionOrNull() as? SupabaseAuthException)?.statusCode
                            if (result.isSuccess || code == 400 || code == 401 || code == 403) {
                                ForgeAuthSessionState.clearSession(context)
                                profile = null
                                status = "Signed out from this device."
                            } else {
                                status = result.exceptionOrNull()?.message ?: "Sign-out failed."
                            }
                            busy = false
                        }
                    }) { Text("Sign out") }
                }

                Text("Change password", style = MaterialTheme.typography.titleMedium)
                PasswordVisibilityField(value = newPassword, onValueChange = { newPassword = it }, label = "New password", enabled = !busy)
                PasswordVisibilityField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Confirm new password", enabled = !busy)
                Text("At least 20 characters with uppercase, lowercase, a number, and a symbol.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    enabled = !busy && validPassword && newPassword == confirmPassword,
                    onClick = {
                        scope.launch {
                            busy = true
                            val current = ForgeAuthSessionState.session
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    requireNotNull(current)
                                    require(validPassword) { "Password does not meet the account policy." }
                                    require(newPassword == confirmPassword) { "Passwords do not match." }
                                    authClient.updatePassword(current, newPassword)
                                }
                            }
                            result.onSuccess {
                                newPassword = ""
                                confirmPassword = ""
                                status = "Password updated successfully."
                            }.onFailure { status = it.message ?: "Password update failed." }
                            busy = false
                        }
                    }
                ) { Text(if (busy) "Please wait…" else "Update password") }
            }
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
