package com.HG.Forge.Ts.data.simulation

import android.content.Context

/** Stores user supplied HTTPS mirrors by distro and country; these are preferences, not verified upstream endpoints. */
class ProotMirrorProfileStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("forge_proot_mirror_profiles", Context.MODE_PRIVATE)

    fun get(distribution: ProotDistribution, countryCode: String): String? =
        preferences.getString(profileKey(distribution, countryCode), null)

    fun save(distribution: ProotDistribution, countryCode: String, url: String) {
        require(countryCode.matches(Regex("[A-Z]{2}"))) { "Choose a country from the supported catalog." }
        require(url.startsWith("https://", ignoreCase = true)) { "Country mirror profiles must use HTTPS." }
        check(preferences.edit().putString(profileKey(distribution, countryCode), url.trim()).commit()) {
            "Could not save the country mirror profile."
        }
    }

    private fun profileKey(distribution: ProotDistribution, countryCode: String) =
        "${distribution.name}_${countryCode.uppercase()}"
}
