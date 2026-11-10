package com.HG.Forge.Ts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.HG.Forge.Ts.data.network.HttpClientProvider
import com.HG.Forge.Ts.data.remote.ForgeAuthSessionState
import com.HG.Forge.Ts.data.remote.SupabaseAuthClient
import com.HG.Forge.Ts.data.sync.ForgeCrashBackendConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@Composable
fun AccountManagementPanel() {
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
    var verifiedStatus by remember { mutableStateOf("Not checked") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    val validPassword = newPassword.length >= 20 &&
        newPassword.any(Char::isLowerCase) &&
        newPassword.any(Char::isUpperCase) &&
        newPassword.any(Char::isDigit) &&
        newPassword.any { !it.isLetterOrDigit() }

    LaunchedEffect(session?.accessToken) {
        val active = session ?: return@LaunchedEffect
        verifiedStatus = "Checking..."
        val result = withContext(Dispatchers.IO) {
            runCatching { authClient.isEmailVerified(active) }
        }
        verifiedStatus = result.fold(
            onSuccess = { if (it) "Verified" else "Not verified" },
            onFailure = { "Unavailable" }
        )
    }

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("FORGE-TS Account", style = MaterialTheme.typography.titleLarge)
            if (session == null) {
                Text("No active account session on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("Email: ${session.email ?: "Not available"}")
                Text("User ID: ${session.userId}", style = MaterialTheme.typography.bodySmall)
                Text("Email status: $verifiedStatus")
                Text(
                    "Session expires: ${DateFormat.getDateTimeInstance().format(Date(session.expiresAtEpochSeconds * 1000L))}",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = !busy,
                        onClick = {
                            scope.launch {
                                busy = true
                                message = "Refreshing session..."
                                val result = withContext(Dispatchers.IO) {
                                    runCatching { authClient.refresh(ForgeAuthSessionState.session ?: session) }
                                }
                                result.onSuccess {
                                    ForgeAuthSessionState.setSession(context, it)
                                    message = "Session refreshed."
                                }.onFailure {
                                    message = it.message ?: "Session refresh failed."
                                    if (ForgeAuthSessionState.session?.expiresAtEpochSeconds?.let { expiry ->
                                            expiry <= System.currentTimeMillis() / 1000L
                                        } == true
                                    ) {
                                        ForgeAuthSessionState.clearSession(context)
                                    }
                                }
                                busy = false
                            }
                        }
                    ) { Text("Refresh session") }
                    OutlinedButton(
                        enabled = !busy,
                        onClick = {
                            scope.launch {
                                busy = true
                                val active = ForgeAuthSessionState.session
                                if (active != null) {
                                    withContext(Dispatchers.IO) {
                                        runCatching { authClient.signOut(active) }
                                    }
                                }
                                ForgeAuthSessionState.clearSession(context)
                                message = "Signed out on this device."
                                busy = false
                            }
                        }
                    ) { Text("Sign out") }
                }
                Text("Change password", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("New password") },
                    singleLine = true,
                    enabled = !busy,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    }
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Confirm new password") },
                    singleLine = true,
                    enabled = !busy,
                    visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmVisible = !confirmVisible }) {
                            Icon(
                                if (confirmVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (confirmVisible) "Hide password" else "Show password"
                            )
                        }
                    }
                )
                Button(
                    enabled = !busy && validPassword && newPassword == confirmPassword,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            busy = true
                            message = "Updating password..."
                            val active = ForgeAuthSessionState.session
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    requireNotNull(active) { "Account session is no longer available." }
                                    authClient.updatePassword(active, newPassword)
                                }
                            }
                            result.onSuccess {
                                newPassword = ""
                                confirmPassword = ""
                                message = "Password changed successfully."
                            }.onFailure {
                                message = it.message ?: "Password change failed."
                            }
                            busy = false
                        }
                    }
                ) { Text(if (busy) "Please wait..." else "Update password") }
                Text(
                    "Password policy: 20+ characters with uppercase, lowercase, a number and a symbol.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall)
        }
    }
}
