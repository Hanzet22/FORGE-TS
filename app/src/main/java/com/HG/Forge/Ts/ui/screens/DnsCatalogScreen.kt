package com.HG.Forge.Ts.ui.screens

import com.HG.Forge.Ts.ui.localization.forgeText
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject

private data class DnsProviderEntry(
    val name: String,
    val sourceUrl: String,
    val ipv4: List<String>,
    val ipv6: List<String>,
    val verification: String
)

private data class DnsCountryEntry(val code: String, val name: String, val sourceUrl: String, val ipv4: List<String>, val ipv6: List<String>, val verification: String)

private fun loadDnsProviders(context: Context): List<DnsProviderEntry> {
    val json = context.assets.open("dns-catalog/providers.json").bufferedReader().use { it.readText() }
    val providers = JSONObject(json).getJSONArray("providers")
    return buildList {
        for (index in 0 until providers.length()) {
            val item = providers.getJSONObject(index)
            fun addresses(key: String): List<String> {
                val array = item.optJSONArray(key) ?: return emptyList()
                return buildList {
                    for (addressIndex in 0 until array.length()) {
                        val value = array.optString(addressIndex).trim()
                        if (value.isNotEmpty()) add(value)
                    }
                }
            }
            add(DnsProviderEntry(
                name = item.optString("name", item.optString("slug")),
                sourceUrl = item.optString("sourceUrl"),
                ipv4 = addresses("ipv4"),
                ipv6 = addresses("ipv6"),
                verification = item.optString("verification", "Not verified")
            ))
        }
    }
}

private fun loadDnsCountries(context: Context): List<DnsCountryEntry> {
    val json = context.assets.open("dns-catalog/countries.json").bufferedReader().use { it.readText() }
    val countries = JSONObject(json).getJSONArray("countries")
    return buildList {
        for (index in 0 until countries.length()) {
            val item = countries.getJSONObject(index)
            fun addresses(key: String): List<String> {
                val array = item.optJSONArray(key) ?: return emptyList()
                return buildList { for (addressIndex in 0 until array.length()) add(array.getJSONObject(addressIndex).optString("address")) }
            }
            add(DnsCountryEntry(item.optString("code"), item.optString("name"), item.optString("sourceUrl"), addresses("ipv4"), addresses("ipv6"), item.optString("verification")))
        }
    }
}

@Composable
fun DnsCatalogScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val providers = remember(context) { runCatching { loadDnsProviders(context) }.getOrElse { emptyList() } }
    val countries = remember(context) { runCatching { loadDnsCountries(context) }.getOrElse { emptyList() } }
    var query by remember { mutableStateOf("") }
    var family by remember { mutableStateOf("All") }
    var catalog by remember { mutableStateOf("Providers") }
    val filtered = providers.filter { provider ->
        val matchesQuery = query.isBlank() ||
            provider.name.contains(query, ignoreCase = true) ||
            provider.ipv4.any { it.contains(query.trim(), ignoreCase = true) } ||
            provider.ipv6.any { it.contains(query.trim(), ignoreCase = true) }
        val matchesFamily = when (family) {
            "IPv4" -> provider.ipv4.isNotEmpty()
            "IPv6" -> provider.ipv6.isNotEmpty()
            else -> true
        }
        matchesQuery && matchesFamily
    }

    val filteredCountries = countries.filter { country ->
        (query.isBlank() || country.name.contains(query, true) || country.code.contains(query, true) || (country.ipv4 + country.ipv6).any { it.contains(query.trim(), true) }) &&
            when (family) { "IPv4" -> country.ipv4.isNotEmpty(); "IPv6" -> country.ipv6.isNotEmpty(); else -> true }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(forgeText("Public DNS catalog"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Providers", "Countries").forEach { option -> FilterChip(selected = catalog == option, onClick = { catalog = option }, label = { Text(forgeText(option)) }) }
        }
        if (catalog == "Providers") Text(
            providers.size.toString() + " provider profiles bundled for offline reference. Addresses are directory-listed snapshots, not live-tested resolver guarantees.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        ) else Text(forgeText("${countries.size} countries from PublicDNS.xyz snapshots; directory entries are observations, not live-tested or endorsed resolvers."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(forgeText(if (catalog == "Providers") "Search provider or IP address" else "Search country or IP address")) },
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "IPv4", "IPv6").forEach { option ->
                FilterChip(selected = family == option, onClick = { family = option }, label = { Text(forgeText(option)) })
            }
        }
        if (catalog == "Countries") {
            if (filteredCountries.isEmpty()) Text(forgeText("No country has matching directory entries for this filter."), color = MaterialTheme.colorScheme.onSurfaceVariant)
            filteredCountries.forEach { country ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(forgeText("${country.name} (${country.code})"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(forgeText("${country.ipv4.size} IPv4 • ${country.ipv6.size} IPv6 observations"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        if (family != "IPv6") country.ipv4.take(5).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        if (family != "IPv4") country.ipv6.take(5).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        Text(forgeText("Showing up to five addresses per family."), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(country.verification, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(country.sourceUrl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else if (providers.isEmpty()) {
            Text(forgeText("DNS catalog asset could not be loaded."), color = MaterialTheme.colorScheme.error)
        } else if (filtered.isEmpty()) {
            Text(forgeText("No provider matches this filter."), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            filtered.forEach { provider ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(provider.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (provider.ipv4.isNotEmpty()) {
                            Text(forgeText("IPv4"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            provider.ipv4.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        }
                        if (provider.ipv6.isNotEmpty()) {
                            Text(forgeText("IPv6"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            provider.ipv6.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        }
                        Text(provider.verification, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(provider.sourceUrl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Text(forgeText("Confirm that a listed address is still operated by its provider, permits public recursive queries, and has a privacy policy you accept before configuring it. This catalog does not change Android or PRoot DNS settings."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
