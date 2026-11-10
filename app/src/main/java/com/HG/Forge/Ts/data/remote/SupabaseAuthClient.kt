package com.HG.Forge.Ts.data.remote

import android.net.Uri
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.json.JSONObject
import java.net.URLEncoder

const val FORGE_AUTH_REDIRECT_URI = "com.hg.forgets://auth/callback"

data class SupabaseAuthSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String?,
    val expiresAtEpochSeconds: Long,
    val emailConfirmedAt: String? = null
)

data class SupabaseTotpFactor(
    val id: String,
    val friendlyName: String?,
    val status: String
)

data class SupabaseTotpEnrollment(
    val factorId: String,
    val qrCodeSvg: String,
    val secret: String,
    val uri: String
)

data class SupabaseAuthUserProfile(
    val userId: String,
    val email: String?,
    val emailConfirmedAt: String?,
    val createdAt: String?,
    val lastSignInAt: String?
) {
    val isEmailVerified: Boolean get() = !emailConfirmedAt.isNullOrBlank()
}

class SupabaseAuthException(val statusCode: Int, val errorCode: String) :
    Exception("$errorCode (HTTP $statusCode)")

/** Password-auth client. Session persistence is handled by ForgeAuthSessionStore using Android Keystore encryption. */
class SupabaseAuthClient(
    private val supabaseUrl: String,
    private val publishableKey: String,
    private val httpClient: HttpClient
) {
    suspend fun signUp(email: String, password: String): SupabaseAuthSession? {
        requireValidInput(email, password)
        val redirect = URLEncoder.encode(FORGE_AUTH_REDIRECT_URI, "UTF-8")
        val response = httpClient.post("${supabaseUrl.trimEnd('/')}/auth/v1/signup?redirect_to=$redirect") {
            header("apikey", publishableKey)
            contentType(ContentType.Application.Json)
            setBody(JSONObject().put("email", email.trim()).put("password", password).toString())
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status.value, body)
        val json = JSONObject(body)
        return json.takeIf { it.has("access_token") }?.toSession()
    }

    suspend fun requestPasswordReset(email: String) {
        requireValidEmail(email)
        val redirect = URLEncoder.encode(FORGE_AUTH_REDIRECT_URI, "UTF-8")
        val response = httpClient.post("${supabaseUrl.trimEnd('/')}/auth/v1/recover?redirect_to=$redirect") {
            header("apikey", publishableKey)
            contentType(ContentType.Application.Json)
            setBody(JSONObject().put("email", email.trim()).toString())
        }
        ensureSuccess(response.status.value, response.bodyAsText())
    }

    suspend fun updatePassword(session: SupabaseAuthSession, newPassword: String) {
        requireValidPassword(newPassword)
        val response = httpClient.put("${supabaseUrl.trimEnd('/')}/auth/v1/user") {
            header("apikey", publishableKey)
            header("Authorization", "Bearer ${session.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(JSONObject().put("password", newPassword).toString())
        }
        ensureSuccess(response.status.value, response.bodyAsText())
    }

    fun isPasswordRecoveryCallback(callbackUrl: String): Boolean {
        val uri = Uri.parse(callbackUrl)
        val fragment = Uri.parse("https://callback.invalid/?${uri.encodedFragment.orEmpty()}")
        return fragment.getQueryParameter("type") == "recovery" ||
            uri.getQueryParameter("type") == "recovery"
    }

    /**
     * Exchanges the access/refresh tokens returned by Supabase email confirmation
     * or password recovery for a verified user profile.
     */
    suspend fun sessionFromCallback(callbackUrl: String): SupabaseAuthSession {
        val uri = Uri.parse(callbackUrl)
        require(uri.scheme == "com.hg.forgets" && uri.host == "auth" && uri.path == "/callback") {
            "Unrecognized authentication callback."
        }
        val params = Uri.parse("https://callback.invalid/?${uri.encodedFragment.orEmpty()}")
        val accessToken = params.getQueryParameter("access_token")
            ?: uri.getQueryParameter("access_token")
            ?: throw IllegalStateException(
                uri.getQueryParameter("error_description") ?: "Verification callback did not contain an access token."
            )
        val refreshToken = params.getQueryParameter("refresh_token")
            ?: uri.getQueryParameter("refresh_token")
            ?: throw IllegalStateException("Verification callback did not contain a refresh token.")
        val expiresIn = (params.getQueryParameter("expires_in") ?: uri.getQueryParameter("expires_in"))
            ?.toLongOrNull() ?: 3600L

        val response = httpClient.get("${supabaseUrl.trimEnd('/')}/auth/v1/user") {
            header("apikey", publishableKey)
            header("Authorization", "Bearer $accessToken")
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status.value, body)
        val user = JSONObject(body)
        return SupabaseAuthSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = user.optString("id"),
            email = user.optString("email").takeIf { it.isNotBlank() },
            expiresAtEpochSeconds = System.currentTimeMillis() / 1000L + expiresIn,
            emailConfirmedAt = user.optString("email_confirmed_at").takeIf { it.isNotBlank() && it != "null" }
        )
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

    suspend fun isEmailVerified(session: SupabaseAuthSession): Boolean {
        val response = httpClient.get("${supabaseUrl.trimEnd('/')}/auth/v1/user") {
            header("apikey", publishableKey)
            header("Authorization", "Bearer ${session.accessToken}")
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status.value, body)
        val user = JSONObject(body)
        val emailConfirmedAt = user.optString("email_confirmed_at")
        val confirmedAt = user.optString("confirmed_at")
        return (emailConfirmedAt.isNotBlank() && emailConfirmedAt != "null") ||
            (confirmedAt.isNotBlank() && confirmedAt != "null")
    }

    suspend fun getUserProfile(session: SupabaseAuthSession): SupabaseAuthUserProfile {
        val response = httpClient.get("${supabaseUrl.trimEnd('/')}/auth/v1/user") {
            header("apikey", publishableKey)
            header("Authorization", "Bearer ${session.accessToken}")
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status.value, body)
        val user = JSONObject(body)
        return SupabaseAuthUserProfile(
            userId = user.optString("id"),
            email = user.optString("email").takeIf { it.isNotBlank() },
            emailConfirmedAt = user.optString("email_confirmed_at").takeIf { it.isNotBlank() && it != "null" },
            createdAt = user.optString("created_at").takeIf { it.isNotBlank() && it != "null" },
            lastSignInAt = user.optString("last_sign_in_at").takeIf { it.isNotBlank() && it != "null" }
        )
    }

    suspend fun signOut(session: SupabaseAuthSession) {
        val response = httpClient.post("${supabaseUrl.trimEnd('/')}/auth/v1/logout") {
            header("apikey", publishableKey)
            header("Authorization", "Bearer ${session.accessToken}")
        }
        ensureSuccess(response.status.value, response.bodyAsText())
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

    suspend fun listTotpFactors(session: SupabaseAuthSession): List<SupabaseTotpFactor> {
        // Supabase documents factor discovery through the PostgREST auth schema.
        // The Auth REST route /auth/v1/factors returned HTTP 405 in FORGE-TS.
        val response = httpClient.get(
            "${supabaseUrl.trimEnd('/')}/rest/v1/auth/factors?select=id,factor_type,friendly_name,status"
        ) {
            header("apikey", publishableKey)
            header("Authorization", "Bearer ${session.accessToken}")
            header("Accept", "application/json")
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status.value, body)
        val factors = org.json.JSONArray(body)
        return buildList {
            for (index in 0 until factors.length()) {
                val factor = factors.optJSONObject(index) ?: continue
                if (factor.optString("factor_type").equals("totp", ignoreCase = true)) {
                    add(
                        SupabaseTotpFactor(
                            id = factor.optString("id"),
                            friendlyName = factor.optString("friendly_name")
                                .takeIf { it.isNotBlank() && it != "null" },
                            status = factor.optString("status")
                        )
                    )
                }
            }
        }.filter { it.id.isNotBlank() }
    }

    suspend fun enrollTotp(
        session: SupabaseAuthSession,
        friendlyName: String = "FORGE-TS Authenticator"
    ): SupabaseTotpEnrollment {
        val response = httpClient.post("${supabaseUrl.trimEnd('/')}/auth/v1/factors") {
            header("apikey", publishableKey)
            header("Authorization", "Bearer ${session.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(
                JSONObject()
                    .put("factor_type", "totp")
                    .put("friendly_name", friendlyName)
                    .toString()
            )
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status.value, body)
        val json = JSONObject(body)
        val totp = json.optJSONObject("totp")
            ?: throw IllegalStateException("Supabase did not return TOTP enrollment data.")
        return SupabaseTotpEnrollment(
            factorId = json.optString("id"),
            qrCodeSvg = totp.optString("qr_code"),
            secret = totp.optString("secret"),
            uri = totp.optString("uri")
        ).also {
            require(it.factorId.isNotBlank() && it.secret.isNotBlank()) {
                "Supabase returned incomplete TOTP enrollment data."
            }
        }
    }

    suspend fun createTotpChallenge(session: SupabaseAuthSession, factorId: String): String {
        val response = httpClient.post("${supabaseUrl.trimEnd('/')}/auth/v1/factors/${factorId}/challenge") {
            header("apikey", publishableKey)
            header("Authorization", "Bearer ${session.accessToken}")
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status.value, body)
        return JSONObject(body).optString("id").also {
            require(it.isNotBlank()) { "Supabase did not return an MFA challenge ID." }
        }
    }

    suspend fun verifyTotpChallenge(
        session: SupabaseAuthSession,
        factorId: String,
        challengeId: String,
        code: String
    ): SupabaseAuthSession {
        require(code.matches(Regex("\\d{6}"))) { "Enter the 6-digit authenticator code." }
        val response = httpClient.post("${supabaseUrl.trimEnd('/')}/auth/v1/factors/${factorId}/verify") {
            header("apikey", publishableKey)
            header("Authorization", "Bearer ${session.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(JSONObject().put("challenge_id", challengeId).put("code", code).toString())
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status.value, body)
        val json = runCatching { JSONObject(body) }.getOrNull()
        return if (json?.has("access_token") == true && json.has("refresh_token")) {
            json.toSession()
        } else {
            refresh(session)
        }
    }

    suspend fun unenrollTotp(session: SupabaseAuthSession, factorId: String) {
        val response = httpClient.delete("${supabaseUrl.trimEnd('/')}/auth/v1/factors/${factorId}") {
            header("apikey", publishableKey)
            header("Authorization", "Bearer ${session.accessToken}")
        }
        ensureSuccess(response.status.value, response.bodyAsText())
    }

    private fun requireValidInput(email: String, password: String) {
        requireValidEmail(email)
        requireValidPassword(password)
    }

    private fun requireValidEmail(email: String) {
        require(supabaseUrl.startsWith("https://")) { "Supabase URL must use HTTPS." }
        require(publishableKey.isNotBlank()) { "Supabase publishable key is missing." }
        require(email.contains("@") && email.length <= 254) { "Enter a valid email address." }
    }

    private fun requireValidPassword(password: String) {
        require(password.length >= 20) { "Password must contain at least 20 characters." }
        require(password.any(Char::isLowerCase) && password.any(Char::isUpperCase) &&
            password.any(Char::isDigit) && password.any { !it.isLetterOrDigit() }) {
            "Password must include uppercase, lowercase, a number, and a symbol."
        }
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
            expiresAtEpochSeconds = expires,
            emailConfirmedAt = user.optString("email_confirmed_at").takeIf { it.isNotBlank() && it != "null" }
        )
    }
}
