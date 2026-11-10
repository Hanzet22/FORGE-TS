package com.HG.Forge.Ts.data.remote

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Shared auth state backed by an Android-Keystore-encrypted local session store. */
object ForgeAuthSessionState {
    var session: SupabaseAuthSession? by mutableStateOf(null)
        private set

    fun setSession(context: Context, value: SupabaseAuthSession) {
        session = value
        ForgeAuthSessionStore.save(context.applicationContext, value)
    }

    fun clearSession(context: Context) {
        session = null
        ForgeAuthSessionStore.clear(context.applicationContext)
    }

    /**
     * Restore the saved session and refresh it when it is near expiry.
     * A temporary network failure does not erase the stored refresh token.
     */
    suspend fun restore(context: Context, authClient: SupabaseAuthClient) {
        val appContext = context.applicationContext
        val stored = ForgeAuthSessionStore.load(appContext) ?: return
        val restored = runCatching { authClient.validSession(stored) }.getOrDefault(stored)
        if (restored.expiresAtEpochSeconds > System.currentTimeMillis() / 1000L) {
            session = restored
            if (restored.accessToken != stored.accessToken ||
                restored.refreshToken != stored.refreshToken
            ) {
                ForgeAuthSessionStore.save(appContext, restored)
            }
        } else {
            // Keep the encrypted refresh token so a later online launch can retry recovery.
            session = null
        }
    }
}
