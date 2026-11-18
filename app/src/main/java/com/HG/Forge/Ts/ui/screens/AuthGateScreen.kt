package com.HG.Forge.Ts.ui.screens

import com.HG.Forge.Ts.ui.localization.forgeTextFor
import com.HG.Forge.Ts.ui.localization.LocalForgeLanguage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import com.HG.Forge.Ts.ui.components.PasswordVisibilityField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.HG.Forge.Ts.data.network.HttpClientProvider
import com.HG.Forge.Ts.data.remote.ForgeAuthSessionState
import com.HG.Forge.Ts.data.remote.SupabaseAuthException
import com.HG.Forge.Ts.data.remote.SupabaseAuthClient
import com.HG.Forge.Ts.data.sync.ForgeCrashBackendConfig
import com.HG.Forge.Ts.data.sync.ForgeDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun authFailureEvent(operation: String, error: Throwable): String {
    val authError = generateSequence(error) { it.cause }
        .filterIsInstance<SupabaseAuthException>()
        .firstOrNull()
    return if (authError != null) "$operation.failed_http_${authError.statusCode}"
    else "$operation.failed_network_or_client"
}

@Composable
fun AuthGateScreen(
    modifier: Modifier = Modifier,
    requestedArea: String,
    onAuthenticated: () -> Unit,
    recoveryMode: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val language = LocalForgeLanguage.current.language
    val context = LocalContext.current
    val authClient = remember {
        SupabaseAuthClient(
            ForgeCrashBackendConfig.SUPABASE_URL,
            ForgeCrashBackendConfig.PUBLISHABLE_KEY,
            HttpClientProvider.client
        )
    }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf("sign_in") }
    var busy by remember { mutableStateOf(false) }
    var message by remember {
        mutableStateOf(
            if (recoveryMode) "Choose a new password for your FORGE-TS account."
            else "Sign in or create an account to unlock this area."
        )
    }
    val validPassword = password.length >= 20 &&
        password.any(Char::isLowerCase) &&
        password.any(Char::isUpperCase) &&
        password.any(Char::isDigit) &&
        password.any { !it.isLetterOrDigit() }

    Card(modifier.fillMaxSize().padding(20.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(forgeTextFor("FORGE-TS ACCOUNT", language), style = MaterialTheme.typography.titleLarge)
            Text(
                if (recoveryMode) "Reset password" else "Account authentication",
                style = MaterialTheme.typography.headlineSmall
            )

            if (recoveryMode) {
                Text(forgeTextFor("Recovery link verified. Set a new password to finish account recovery.", language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(forgeTextFor("New password", language)) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = if (passwordVisible) forgeTextFor("Hide password", language) else forgeTextFor("Show password", language))
                        }
                    },
                    enabled = !busy
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(forgeTextFor("Confirm new password", language)) },
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(if (confirmPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = if (confirmPasswordVisible) forgeTextFor("Hide password", language) else forgeTextFor("Show password", language))
                        }
                    },
                    enabled = !busy
                )
                Button(
                    onClick = {
                        scope.launch {
                            busy = true
                            message = forgeTextFor("Updating password...", language)
                            val active = ForgeAuthSessionState.session
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    requireNotNull(active) { "Recovery session is missing. Request a new reset email." }
                                    require(validPassword) {
                                        "Password needs 20+ characters with uppercase, lowercase, a number, and a symbol."
                                    }
                                    require(password == confirmPassword) { "Passwords do not match." }
                                    authClient.updatePassword(active, password)
                                }
                            }
                            result.onSuccess {
                                ForgeAuthSessionState.completePasswordRecovery(context)
                                password = ""
                                confirmPassword = ""
                                message = forgeTextFor("Password updated. Your account is ready.", language)
                                onAuthenticated()
                            }.onFailure {
                                ForgeDiagnostics.log(context, "ERROR", "SupabaseAuth", authFailureEvent("auth.password_update", it), it)
                                message = it.message ?: forgeTextFor("Password update failed.", language)
                            }
                            busy = false
                        }
                    },
                    enabled = !busy && validPassword && password == confirmPassword &&
                        ForgeAuthSessionState.session != null,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (busy) forgeTextFor("Please wait...", language) else forgeTextFor("Update password", language)) }
            } else {
                Text(forgeTextFor("A shared FORGE-TS account session is required to access this area.", language) + " " + forgeTextFor("Requested area:", language) + " " + requestedArea,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trimStart() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(forgeTextFor("Email", language)) },
                    singleLine = true,
                    enabled = !busy
                )

                if (mode != "reset") {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(forgeTextFor("Password", language)) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = if (passwordVisible) forgeTextFor("Hide password", language) else forgeTextFor("Show password", language))
                            }
                        },
                        enabled = !busy
                    )
                }

                when (mode) {
                    "sign_in" -> {
                        Button(
                            onClick = {
                                scope.launch {
                                    busy = true
                                    message = forgeTextFor("Signing in...", language)
                                    val traceId = ForgeDiagnostics.newTraceId()
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching { authClient.signIn(email, password) }
                                    }
                                    result.onSuccess { signedIn ->
                                        ForgeAuthSessionState.setSession(context, signedIn)
                                        ForgeDiagnostics.log(context, "INFO", "SupabaseAuth", "auth.sign_in.succeeded", traceId = traceId)
                                        message = forgeTextFor("Email and password verified. Simulate and Tools are unlocked.", language)
                                        onAuthenticated()
                                    }.onFailure {
                                        ForgeDiagnostics.log(context, "ERROR", "SupabaseAuth", authFailureEvent("auth.sign_in", it), it, traceId)
                                        message = it.message ?: forgeTextFor("Sign-in failed.", language)
                                    }
                                    busy = false
                                }
                            },
                            enabled = !busy && email.contains("@") && password.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (busy) forgeTextFor("Please wait...", language) else forgeTextFor("Sign in", language)) }
                        OutlinedButton(
                            onClick = { mode = "create"; message = forgeTextFor("Create your FORGE-TS account.", language) },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(forgeTextFor("Create account", language)) }
                        OutlinedButton(
                            onClick = { mode = "reset"; message = forgeTextFor("Enter your account email to receive a password reset link.", language) },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(forgeTextFor("Reset password", language)) }
                    }
                    "create" -> {
                        Button(
                            onClick = {
                                scope.launch {
                                    busy = true
                                    message = forgeTextFor("Creating account...", language)
                                    val traceId = ForgeDiagnostics.newTraceId()
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching { authClient.signUp(email, password) }
                                    }
                                    result.onSuccess { created ->
                                        ForgeDiagnostics.log(context, "INFO", "SupabaseAuth", "auth.sign_up.succeeded", traceId = traceId)
                                        if (created != null) {
                                            ForgeAuthSessionState.setSession(context, created)
                                            message = forgeTextFor("Account created and authenticated.", language)
                                            onAuthenticated()
                                        } else {
                                            message = forgeTextFor("Registration submitted. Check your email and open the verification link to return to FORGE-TS.", language)
                                        }
                                    }.onFailure {
                                        ForgeDiagnostics.log(context, "ERROR", "SupabaseAuth", authFailureEvent("auth.sign_up", it), it, traceId)
                                        message = it.message ?: forgeTextFor("Account creation failed.", language)
                                    }
                                    busy = false
                                }
                            },
                            enabled = !busy && email.contains("@") && validPassword,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (busy) forgeTextFor("Please wait...", language) else forgeTextFor("Create account", language)) }
                        OutlinedButton(
                            onClick = { mode = "sign_in"; message = forgeTextFor("Sign in to your existing account.", language) },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(forgeTextFor("Back to sign in", language)) }
                    }
                    else -> {
                        Button(
                            onClick = {
                                scope.launch {
                                    busy = true
                                    message = forgeTextFor("Sending password reset email...", language)
                                    val traceId = ForgeDiagnostics.newTraceId()
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching { authClient.requestPasswordReset(email) }
                                    }
                                    result.onSuccess {
                                        ForgeDiagnostics.log(context, "INFO", "SupabaseAuth", "auth.password_reset.requested", traceId = traceId)
                                        message = forgeTextFor("If the account exists, a recovery link has been sent. Open it on this device to set a new password.", language)
                                    }.onFailure {
                                        ForgeDiagnostics.log(context, "ERROR", "SupabaseAuth", authFailureEvent("auth.password_reset", it), it, traceId)
                                        message = it.message ?: forgeTextFor("Password reset request failed.", language)
                                    }
                                    busy = false
                                }
                            },
                            enabled = !busy && email.contains("@"),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (busy) forgeTextFor("Please wait...", language) else forgeTextFor("Send reset link", language)) }
                        OutlinedButton(
                            onClick = { mode = "sign_in"; message = forgeTextFor("Sign in to your existing account.", language) },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(forgeTextFor("Back to sign in", language)) }
                    }
                }
                Text(forgeTextFor("Password policy: at least 20 characters, including uppercase, lowercase, a number, and a symbol.", language),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(message, style = MaterialTheme.typography.bodySmall)
            Text(forgeTextFor("Account credentials are verified by Supabase Auth. Session tokens are encrypted with Android Keystore on this device.", language),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
