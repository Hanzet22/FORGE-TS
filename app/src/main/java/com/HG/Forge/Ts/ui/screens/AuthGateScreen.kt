package com.HG.Forge.Ts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.HG.Forge.Ts.AuthDeepLinkBus
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
    onAuthenticated: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val authClient = remember {
        SupabaseAuthClient(
            ForgeCrashBackendConfig.SUPABASE_URL,
            ForgeCrashBackendConfig.PUBLISHABLE_KEY,
            HttpClientProvider.client
        )
    }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Sign in or create an account to unlock this area.") }

    LaunchedEffect(authClient) {
        AuthDeepLinkBus.callbacks.collect { callbackUrl ->
            busy = true
            message = "Verifying your email callback..."
            val result = withContext(Dispatchers.IO) {
                runCatching { authClient.sessionFromCallback(callbackUrl) }
            }
            result.onSuccess {
                ForgeAuthSessionState.session = it
                message = "Email verified. Access granted."
                onAuthenticated()
            }.onFailure {
                message = "Verification failed: ${it.message ?: "unknown error"}"
            }
            busy = false
        }
    }

    Card(modifier.fillMaxSize().padding(20.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("FORGE-TS ACCOUNT", style = MaterialTheme.typography.titleLarge)
            Text("Authentication required", style = MaterialTheme.typography.headlineSmall)
            Text(
                "The ${requestedArea} tab is locked until you sign in. All feature panels and actions in Simulate and Tools require this shared account session.",
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
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                enabled = !busy
            )
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        message = "Signing in..."
                        val result = withContext(Dispatchers.IO) {
                            runCatching { authClient.signIn(email, password) }
                        }
                        result.onSuccess {
                            ForgeAuthSessionState.session = it
                            message = "Signed in. Simulate and Tools are unlocked."
                            onAuthenticated()
                        }.onFailure {
                            message = it.message ?: "Sign-in failed."
                        }
                        busy = false
                    }
                },
                enabled = !busy && email.contains("@") && password.length >= 6,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "Please wait..." else "Sign in") }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        busy = true
                        message = "Creating account..."
                        val result = withContext(Dispatchers.IO) {
                            runCatching { authClient.signUp(email, password) }
                        }
                        result.onSuccess { created ->
                            if (created != null) {
                                ForgeAuthSessionState.session = created
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
                enabled = !busy && email.contains("@") && password.length >= 6,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Create account") }
            Text(message, style = MaterialTheme.typography.bodySmall)
            Text(
                "Home, Learn, Network and Settings remain available without login. Session tokens stay in app memory and are not stored on disk. Internet access is required to authenticate.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
