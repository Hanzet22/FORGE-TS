package com.HG.Forge.Ts.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.json.JSONObject

data class SupabaseAuthSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String?,
    val expiresAtEpochSeconds: Long
)

class SupabaseAuthException(val statusCode: Int, val errorCode: String) :
    Exception("$errorCode (HTTP $statusCode)")

/** Minimal password-auth client. Tokens are kept in memory only; no token is written to disk. */
class SupabaseAuthClient(
    private val supabaseUrl: String,
    private val publishableKey: String,
    private val httpClient: HttpClient
) {
    suspend fun signUp(email: String, password: String): SupabaseAuthSession? {
        requireValidInput(email, password)
        val response = httpClient.post("${supabaseUrl.trimEnd('/')}/auth/v1/signup") {
            header("apikey", publishableKey)
            contentType(ContentType.Application.Json)
            setBody(JSONObject().put("email", email.trim()).put("password", password).toString())
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status.value, body)
        val json = JSONObject(body)
        return json.takeIf { it.has("access_token") }?.toSession()
    }

    suspend fun signIn(email: String, password: String): SupabaseAuthSession {
        requireValidInput(email, password)
        val response = httpClient.post("${supabaseUrl.trimEnd('/')}/auth/v1/token?grant_type=password") {
            header("apikey", publishableKey)
            contentType(ContentType.Application.Json)
            setBody(JSONObject().put("email", email.trim()).put("password", password).toString())
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status.value, body)
        return JSONObject(body).toSession()
    }

    suspend fun refresh(session: SupabaseAuthSession): SupabaseAuthSession {
        val response = httpClient.post("${supabaseUrl.trimEnd('/')}/auth/v1/token?grant_type=refresh_token") {
            header("apikey", publishableKey)
            contentType(ContentType.Application.Json)
            setBody(JSONObject().put("refresh_token", session.refreshToken).toString())
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status.value, body)
        return JSONObject(body).toSession()
    }

    suspend fun validSession(session: SupabaseAuthSession): SupabaseAuthSession {
        return if (session.expiresAtEpochSeconds <= System.currentTimeMillis() / 1000L + 60L) {
            refresh(session)
        } else session
    }

    private fun requireValidInput(email: String, password: String) {
        require(supabaseUrl.startsWith("https://")) { "Supabase URL must use HTTPS." }
        require(publishableKey.isNotBlank()) { "Supabase publishable key is missing." }
        require(email.contains("@") && email.length <= 254) { "Enter a valid email address." }
        require(password.length >= 6) { "Password must contain at least 6 characters." }
    }

    private fun ensureSuccess(statusCode: Int, body: String) {
        if (statusCode !in 200..299) {
            val json = runCatching { JSONObject(body) }.getOrNull()
            val code = json?.optString("msg")?.takeIf { it.isNotBlank() }
                ?: json?.optString("message")?.takeIf { it.isNotBlank() }
                ?: json?.optString("error_description")?.takeIf { it.isNotBlank() }
                ?: json?.optString("error")?.takeIf { it.isNotBlank() }
                ?: "supabase_auth_failed"
            throw SupabaseAuthException(statusCode, code.take(160))
        }
    }

    private fun JSONObject.toSession(): SupabaseAuthSession {
        val user = optJSONObject("user") ?: throw IllegalStateException("Auth response did not include a user.")
        val access = optString("access_token")
        val refresh = optString("refresh_token")
        require(access.isNotBlank() && refresh.isNotBlank()) {
            "No active session returned. Email confirmation may be required."
        }
        val expires = optLong("expires_at").takeIf { it > 0 }
            ?: (System.currentTimeMillis() / 1000L + optLong("expires_in", 3600L))
        return SupabaseAuthSession(
            accessToken = access,
            refreshToken = refresh,
            userId = user.optString("id"),
            email = user.optString("email").takeIf { it.isNotBlank() },
            expiresAtEpochSeconds = expires
        )
    }
}
