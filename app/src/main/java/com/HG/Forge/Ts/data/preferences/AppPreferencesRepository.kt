package com.HG.Forge.Ts.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.forgePreferences by preferencesDataStore(name = "forge_preferences")

class AppPreferencesRepository(private val context: Context) {
    private val defaultDnsTypeKey = stringPreferencesKey("default_dns_record_type")

    val defaultDnsRecordType: Flow<String> = context.forgePreferences.data.map { preferences ->
        preferences[defaultDnsTypeKey] ?: "A"
    }

    suspend fun setDefaultDnsRecordType(type: String) {
        require(type in setOf("A", "AAAA", "CNAME", "MX", "TXT", "NS", "SRV", "CAA"))
        context.forgePreferences.edit { preferences ->
            preferences[defaultDnsTypeKey] = type
        }
    }
}
