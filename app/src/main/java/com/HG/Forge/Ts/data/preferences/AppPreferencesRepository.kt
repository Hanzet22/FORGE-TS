package com.HG.Forge.Ts.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.forgePreferences by preferencesDataStore(name = "forge_preferences")

enum class ForgeThemeMode(val label: String, val description: String) {
    WHITE("White", "Bright light surfaces"),
    BLACK("Black", "Dark grey surfaces"),
    AMOLED_BLACK("AMOLED Black", "Pure black background with separated dark surfaces"),
    SUPER_BLACK("Super Black", "Near-total black surfaces with high-contrast text"),
    DYNAMIC_COLOR("Dynamic Color", "Use the Android wallpaper palette when supported");

    companion object {
        fun fromStored(value: String?): ForgeThemeMode =
            entries.firstOrNull { it.name == value } ?: DYNAMIC_COLOR
    }
}

class AppPreferencesRepository(private val context: Context) {
    private val defaultDnsTypeKey = stringPreferencesKey("default_dns_record_type")
    private val themeModeKey = stringPreferencesKey("theme_mode")

    val defaultDnsRecordType: Flow<String> = context.forgePreferences.data.map { preferences ->
        preferences[defaultDnsTypeKey] ?: "A"
    }

    val themeMode: Flow<ForgeThemeMode> = context.forgePreferences.data.map { preferences ->
        ForgeThemeMode.fromStored(preferences[themeModeKey])
    }

    suspend fun setThemeMode(mode: ForgeThemeMode) {
        context.forgePreferences.edit { preferences -> preferences[themeModeKey] = mode.name }
    }

    suspend fun setDefaultDnsRecordType(type: String) {
        require(type in setOf("A", "AAAA", "CNAME", "MX", "TXT", "NS", "SRV", "CAA"))
        context.forgePreferences.edit { preferences ->
            preferences[defaultDnsTypeKey] = type
        }
    }
}
