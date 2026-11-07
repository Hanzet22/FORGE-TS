package com.HG.Forge.Ts.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.IDN
import java.net.InetAddress
import java.net.URI
import java.net.URL
import java.net.URLEncoder

data class RdapDomainReport(
    val domain: String,
    val handle: String?,
    val registrar: String?,
    val statuses: List<String>,
    val events: List<String>,
    val nameServers: List<String>,
    val source: String
)

/**
 * Read-only RDAP domain metadata lookup. Contact/vCard data is deliberately not collected.
 * Only the rdap.org HTTPS endpoint and HTTPS redirects to public hosts are accepted.
 */
class RdapDomainRepository {
    suspend fun lookup(input: String): RdapDomainReport = withContext(Dispatchers.IO) {
        val domain = normalizeDomain(input)
        var current = URL("https://rdap.org/domain/${URLEncoder.encode(domain, "UTF-8")}")
        repeat(4) { hop ->
            require(current.protocol == "https") { "RDAP redirects must remain on HTTPS." }
            validatePublicHost(current.host)
            val connection = (current.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/rdap+json, application/json")
                setRequestProperty("User-Agent", "FORGE-TS-RDAP/1.0")
                useCaches = false
            }
            try {
                val status = connection.responseCode
                if (status in listOf(301, 302, 303, 307, 308)) {
                    val location = connection.getHeaderField("Location")
                        ?: error("RDAP endpoint returned a redirect without a Location header.")
                    require(hop < 3) { "RDAP redirect limit exceeded." }
                    val next = current.toURI().resolve(location).toURL()
                    require(next.protocol == "https") { "Insecure RDAP redirect was blocked." }
                    current = next
                    return@repeat
                }
                require(status in 200..299) { "RDAP service returned HTTP $status." }
                val bytes = connection.inputStream.use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(4096)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count <= 0) break
                        total += count
                        require(total <= 262144) { "RDAP response exceeded the 256 KiB limit." }
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
                val json = JSONObject(bytes.toString(Charsets.UTF_8))
                val statuses = json.optJSONArray("status")?.let { array ->
                    (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
                }.orEmpty()
                val events = json.optJSONArray("events")?.let { array ->
                    buildList {
                        for (index in 0 until array.length()) {
                            val event = array.optJSONObject(index) ?: continue
                            val action = event.optString("eventAction")
                            val date = event.optString("eventDate")
                            if (action.isNotBlank() && date.isNotBlank()) add("$action: $date")
                        }
                    }
                }.orEmpty()
                val nameServers = json.optJSONArray("nameservers")?.let { array ->
                    buildList {
                        for (index in 0 until array.length()) {
                            val server = array.optJSONObject(index) ?: continue
                            server.optString("ldhName").takeIf(String::isNotBlank)?.let { add(it) }
                        }
                    }
                }.orEmpty()
                val registrar = extractRegistrar(json)
                return@withContext RdapDomainReport(
                    domain = json.optString("ldhName", domain),
                    handle = json.optString("handle").takeIf(String::isNotBlank),
                    registrar = registrar,
                    statuses = statuses,
                    events = events,
                    nameServers = nameServers,
                    source = current.host
                )
            } finally {
                connection.disconnect()
            }
        }
        error("RDAP lookup did not complete.")
    }

    private fun extractRegistrar(json: JSONObject): String? {
        val entities = json.optJSONArray("entities") ?: return null
        for (index in 0 until entities.length()) {
            val entity = entities.optJSONObject(index) ?: continue
            val roles = entity.optJSONArray("roles") ?: continue
            val isRegistrar = (0 until roles.length()).any { roles.optString(it).equals("registrar", true) }
            if (!isRegistrar) continue
            val vcard = entity.optJSONArray("vcardArray")?.optJSONArray(1) ?: continue
            for (fieldIndex in 0 until vcard.length()) {
                val field = vcard.optJSONArray(fieldIndex) ?: continue
                if (field.optString(0).equals("fn", true)) {
                    return field.optString(3).takeIf(String::isNotBlank)
                }
            }
            return entity.optString("handle").takeIf(String::isNotBlank)
        }
        return null
    }

    private fun normalizeDomain(input: String): String {
        val value = input.trim().removePrefix("https://").removePrefix("http://").substringBefore('/').substringBefore(':').trimEnd('.')
        require(value.isNotBlank() && value.length <= 253 && value.contains('.')) { "Enter a valid domain name for RDAP." }
        return IDN.toASCII(value, IDN.USE_STD3_ASCII_RULES).lowercase()
    }

    private fun validatePublicHost(host: String) {
        val addresses = InetAddress.getAllByName(host)
        require(addresses.isNotEmpty() && addresses.all { address ->
            !address.isAnyLocalAddress && !address.isLoopbackAddress && !address.isLinkLocalAddress &&
                !address.isSiteLocalAddress && !address.isMulticastAddress &&
                !(address.address.size == 16 && (address.address[0].toInt() and 0xfe) == 0xfc)
        }) { "RDAP resolved to a non-public network destination; request blocked." }
    }
}
