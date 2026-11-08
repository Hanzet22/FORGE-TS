package com.HG.Forge.Ts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.HG.Forge.Ts.data.network.HttpClientProvider
import com.HG.Forge.Ts.data.remote.RedisGatewayClient
import com.HG.Forge.Ts.data.remote.RedisGatewayOperation
import com.HG.Forge.Ts.data.remote.RedisGatewayRequest
import com.HG.Forge.Ts.data.remote.SupabaseAuthClient
import com.HG.Forge.Ts.data.remote.SupabaseAuthSession
import com.HG.Forge.Ts.data.remote.SupabaseLabRecordsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val FORGE_SUPABASE_URL = "https://hzwyxvdeeetkxhijgxwy.supabase.co"
private const val FORGE_SUPABASE_PUBLISHABLE_KEY = "sb_publishable_5rbPwWvKvIo7i4mDs9x7Ww_SNw1VFNO"

@Composable
fun RemoteAccessPanel(internetAvailable: Boolean) {
    val scope = rememberCoroutineScope()
    val httpClient = HttpClientProvider.client
    val authClient = remember { SupabaseAuthClient(FORGE_SUPABASE_URL, FORGE_SUPABASE_PUBLISHABLE_KEY, httpClient) }
    val redisClient = remember { RedisGatewayClient(FORGE_SUPABASE_URL, FORGE_SUPABASE_PUBLISHABLE_KEY, httpClient) }
    val recordsClient = remember { SupabaseLabRecordsClient(FORGE_SUPABASE_URL, FORGE_SUPABASE_PUBLISHABLE_KEY, httpClient) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var session by remember { mutableStateOf<SupabaseAuthSession?>(null) }
    var busy by remember { mutableStateOf(false) }
    var output by remember { mutableStateOf("Sign in to test the real Supabase and Redis connections.") }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("REMOTE ACCESS • SUPABASE AUTH", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (!internetAvailable) Text("OFFLINE • remote requests are paused; local simulation remains available.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            if (session == null) {
                OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true)
                OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = internetAvailable && !busy, onClick = {
                        scope.launch {
                            busy = true
                            output = "Signing in..."
                            val result = withContext(Dispatchers.IO) {
                                runCatching { authClient.signIn(email, password) }
                            }
                            result.onSuccess {
                                session = it
                                output = "AUTHENTICATED • ${it.email ?: it.userId}"
                            }.onFailure {
                                output = it.message ?: "Sign-in failed."
                            }
                            busy = false
                        }
                    }) { Text("Sign in") }
                    OutlinedButton(enabled = internetAvailable && !busy, onClick = {
                        scope.launch {
                            busy = true
                            output = "Creating account..."
                            val result = withContext(Dispatchers.IO) {
                                runCatching { authClient.signUp(email, password) }
                            }
                            result.onSuccess { created ->
                                if (created != null) {
                                    session = created
                                    output = "ACCOUNT CREATED • authenticated"
                                } else {
                                    output = "SIGNUP SUBMITTED • check your email for confirmation, then sign in."
                                }
                            }.onFailure {
                                output = it.message ?: "Sign-up failed."
                            }
                            busy = false
                        }
                    }) { Text("Create account") }
                }
            } else {
                Text("SIGNED IN • ${session?.email ?: session?.userId}", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = internetAvailable && !busy, onClick = {
                        scope.launch {
                            busy = true
                            output = "Checking Redis gateway..."
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    val fresh = authClient.validSession(requireNotNull(session))
                                    val ping = redisClient.execute(fresh.accessToken, RedisGatewayRequest(RedisGatewayOperation.PING))
                                    fresh to ping
                                }
                            }
                            result.onSuccess {
                                session = it.first
                                output = "REDIS GATEWAY ONLINE • PING result: ${it.second.result} • source: ${it.second.source}"
                            }.onFailure {
                                output = "REDIS GATEWAY CHECK FAILED • ${it.message ?: "unknown error"}"
                            }
                            busy = false
                        }
                    }) { Text("Test Redis PING") }
                    OutlinedButton(enabled = internetAvailable && !busy, onClick = {
                        scope.launch {
                            busy = true
                            output = "Checking Supabase RLS table..."
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    val fresh = authClient.validSession(requireNotNull(session))
                                    val rows = recordsClient.select(fresh.accessToken)
                                    fresh to rows.size
                                }
                            }
                            result.onSuccess {
                                session = it.first
                                output = "SUPABASE REST ONLINE • RLS SELECT succeeded • ${it.second} row(s) visible to this user."
                            }.onFailure {
                                output = "SUPABASE CHECK FAILED • ${it.message ?: "unknown error"}"
                            }
                            busy = false
                        }
                    }) { Text("Test Supabase SELECT") }
                }
                OutlinedButton(enabled = !busy, onClick = {
                    session = null
                    password = ""
                    output = "Signed out locally. No token was persisted on this device."
                }, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
            }
            Text(output, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Session tokens stay in app memory and are not saved to disk. Sign in again after restarting the app. The publishable key is public; Redis secrets remain server-side.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
