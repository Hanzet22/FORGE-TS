package com.HG.Forge.Ts.data.remote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts Supabase access and refresh tokens with a non-exportable Android Keystore key. */
internal object ForgeAuthSessionStore {
    private const val PREFS = "forge_auth_secure_session"
    private const val SESSION_KEY = "encrypted_session"
    private const val RECOVERY_PENDING_KEY = "password_recovery_pending"
    private const val KEY_ALIAS = "forge_ts_supabase_session_v1"

    fun save(context: Context, session: SupabaseAuthSession) {
        val json = JSONObject()
            .put("access_token", session.accessToken)
            .put("refresh_token", session.refreshToken)
            .put("user_id", session.userId)
            .put("email", session.email)
            .put("expires_at", session.expiresAtEpochSeconds)
            .toString()
            .toByteArray(Charsets.UTF_8)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(json)
        val payload = ByteBuffer.allocate(4 + cipher.iv.size + encrypted.size)
            .putInt(cipher.iv.size)
            .put(cipher.iv)
            .put(encrypted)
            .array()

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(SESSION_KEY, Base64.encodeToString(payload, Base64.NO_WRAP))
            .apply()
    }

    fun load(context: Context): SupabaseAuthSession? {
        val encoded = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(SESSION_KEY, null) ?: return null

        return runCatching {
            val payload = ByteBuffer.wrap(Base64.decode(encoded, Base64.NO_WRAP))
            val ivLength = payload.int
            require(ivLength == 12 && payload.remaining() > ivLength)
            val iv = ByteArray(ivLength).also(payload::get)
            val encrypted = ByteArray(payload.remaining()).also(payload::get)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            val json = JSONObject(String(cipher.doFinal(encrypted), Charsets.UTF_8))
            SupabaseAuthSession(
                accessToken = json.getString("access_token"),
                refreshToken = json.getString("refresh_token"),
                userId = json.getString("user_id"),
                email = json.optString("email").takeIf { it.isNotBlank() && it != "null" },
                expiresAtEpochSeconds = json.getLong("expires_at")
            )
        }.getOrElse {
            clear(context)
            null
        }
    }

    fun setPasswordRecoveryPending(context: Context, pending: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(RECOVERY_PENDING_KEY, pending)
            .apply()
    }

    fun isPasswordRecoveryPending(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(RECOVERY_PENDING_KEY, false)

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(SESSION_KEY)
            .remove(RECOVERY_PENDING_KEY)
            .apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
