package com.HG.Forge.Ts.ui.screens

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
import com.HG.Forge.Ts.data.remote.SupabaseAuthClient
import com.HG.Forge.Ts.data.sync.ForgeCrashBackendConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AuthGateScreen(
    modifier: Modifier = Modifier,
    requestedArea: String,
    onAuthenticated: () -> Unit,
    recoveryMode: Boolean = false
) {
    val scope = rememberCoroutineScope()
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
            Text("FORGE-TS ACCOUNT", style = MaterialTheme.typography.titleLarge)
            Text(
                if (recoveryMode) "Reset password" else "Account authentication",
                style = MaterialTheme.typography.headlineSmall
            )

            if (recoveryMode) {
                Text(
                    "Recovery link verified. Set a new password to finish account recovery.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("New password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                        }
                    },
                    enabled = !busy
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Confirm new password") },
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(if (confirmPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password")
                        }
                    },
                    enabled = !busy
                )
                Button(
                    onClick = {
                        scope.launch {
                            busy = true
                            message = "Updating password..."
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
                                message = "Password updated. Your account is ready."
                                onAuthenticated()
                            }.onFailure {
                                message = it.message ?: "Password update failed."
                            }
                            busy = false
                        }
                    },
                    enabled = !busy && validPassword && password == confirmPassword &&
                        ForgeAuthSessionState.session != null,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (busy) "Please wait..." else "Update password") }
            } else {
                Text(
                    "The $requestedArea area requires a shared FORGE-TS account session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trimStart() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") },
                    singleLine = true,
                    enabled = !busy
                )

                if (mode != "reset") {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = if (passwordVisible) "Hide password" else "Show password")
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
                                    message = "Signing in..."
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching { authClient.signIn(email, password) }
                                    }
                                    result.onSuccess {
                                        ForgeAuthSessionState.setSession(context, it)
                                        message = "Signed in. Simulate and Tools are unlocked."
                                        onAuthenticated()
                                    }.onFailure {
                                        message = it.message ?: "Sign-in failed."
                                    }
                                    busy = false
                                }
                            },
                            enabled = !busy && email.contains("@") && password.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (busy) "Please wait..." else "Sign in") }
                        OutlinedButton(
                            onClick = { mode = "create"; message = "Create your FORGE-TS account." },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Create account") }
                        OutlinedButton(
                            onClick = { mode = "reset"; message = "Enter your account email to receive a password reset link." },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Reset password") }
                    }
                    "create" -> {
                        Button(
                            onClick = {
                                scope.launch {
                                    busy = true
                                    message = "Creating account..."
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching { authClient.signUp(email, password) }
                                    }
                                    result.onSuccess { created ->
                                        if (created != null) {
                                            ForgeAuthSessionState.setSession(context, created)
                                            message = "Account created and authenticated."
                                            onAuthenticated()
                                        } else {
                                            message = "Registration submitted. Check your email and open the verification link to return to FORGE-TS."
                                        }
                                    }.onFailure {
                                        message = it.message ?: "Account creation failed."
                                    }
                                    busy = false
                                }
                            },
                            enabled = !busy && email.contains("@") && validPassword,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (busy) "Please wait..." else "Create account") }
                        OutlinedButton(
                            onClick = { mode = "sign_in"; message = "Sign in to your existing account." },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Back to sign in") }
                    }
                    else -> {
                        Button(
                            onClick = {
                                scope.launch {
                                    busy = true
                                    message = "Sending password reset email..."
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching { authClient.requestPasswordReset(email) }
                                    }
                                    result.onSuccess {
                                        message = "If the account exists, a recovery link has been sent. Open it on this device to set a new password."
                                    }.onFailure {
                                        message = it.message ?: "Password reset request failed."
                                    }
                                    busy = false
                                }
                            },
                            enabled = !busy && email.contains("@"),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (busy) "Please wait..." else "Send reset link") }
                        OutlinedButton(
                            onClick = { mode = "sign_in"; message = "Sign in to your existing account." },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Back to sign in") }
                    }
                }
                Text(
                    "Password policy: at least 20 characters, including uppercase, lowercase, a number, and a symbol.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(message, style = MaterialTheme.typography.bodySmall)
            Text(
                "Account credentials are verified by Supabase Auth. Session tokens are encrypted with Android Keystore on this device.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
