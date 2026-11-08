package com.HG.Forge.Ts.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

data class RdapIpReport(
    val address: String,
    val ipVersion: String?,
    val networkName: String?,
    val handle: String?,
    val startAddress: String?,
    val endAddress: String?,
    val country: String?,
    val source: String
)

/**
 * Read-only IP network attribution using the public RDAP bootstrap service.
 * The lookup is limited to one already-resolved public A/AAAA address.
 * Registrant contact/vCard data is deliberately not collected.
 */
class RdapIpRepository {
    suspend fun lookup(input: String): RdapIpReport = withContext(Dispatchers.IO) {
        val address = input.trim()
        require(isLiteralIp(address)) { "A literal IP address is required for network attribution." }
        require(isPublicAddress(address)) { "Private, reserved or non-public IP addresses are not queried." }

        var current = URL("https://rdap.org/ip/$address")
        repeat(4) { hop ->
            require(current.protocol == "https") { "RDAP redirects must remain on HTTPS." }
            validatePublicHost(current.host)
            val connection = (current.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/rdap+json, application/json")
                setRequestProperty("User-Agent", "FORGE-TS-IP-RDAP/1.0")
                useCaches = false
            }
            try {
                val status = connection.responseCode
                if (status in listOf(301, 302, 303, 307, 308)) {
                    val location = connection.getHeaderField("Location")
                        ?: error("IP RDAP redirect omitted its Location header.")
                    require(hop < 3) { "IP RDAP redirect limit exceeded." }
                    val next = current.toURI().resolve(location).toURL()
                    require(next.protocol == "https") { "Insecure IP RDAP redirect was blocked." }
                    current = next
                    return@repeat
                }
                require(status in 200..299) { "IP RDAP service returned HTTP $status." }
                val bytes = connection.inputStream.use { inputStream ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(4096)
                    var total = 0
                    while (true) {
                        val count = inputStream.read(buffer)
                        if (count <= 0) break
                        total += count
                        require(total <= 131072) { "IP RDAP response exceeded the 128 KiB limit." }
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
                val json = JSONObject(bytes.toString(Charsets.UTF_8))
                return@withContext RdapIpReport(
                    address = address,
                    ipVersion = json.optString("ipVersion").takeIf(String::isNotBlank),
                    networkName = json.optString("name").takeIf(String::isNotBlank),
                    handle = json.optString("handle").takeIf(String::isNotBlank),
                    startAddress = json.optString("startAddress").takeIf(String::isNotBlank),
                    endAddress = json.optString("endAddress").takeIf(String::isNotBlank),
                    country = json.optString("country").takeIf(String::isNotBlank),
                    source = current.host
                )
            } finally {
                connection.disconnect()
            }
        }
        error("IP RDAP lookup did not complete.")
    }

    private fun isLiteralIp(value: String): Boolean =
        value.contains(':') || value.matches(Regex("""\d{1,3}(?:\.\d{1,3}){3}"""))

    private fun isPublicAddress(value: String): Boolean {
        val address = runCatching { InetAddress.getByName(value) }.getOrNull() ?: return false
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
            address.isSiteLocalAddress || address.isMulticastAddress) return false
        val bytes = address.address.map { it.toInt() and 0xff }
        if (bytes.size == 4) {
            val a = bytes[0]; val b = bytes[1]
            if (a == 0 || a == 10 || a == 127 || a >= 224) return false
            if (a == 100 && b in 64..127) return false
            if (a == 169 && b == 254) return false
            if (a == 172 && b in 16..31) return false
            if (a == 192 && b == 168) return false
            if (a == 192 && b == 0) return false
            if (a == 192 && b == 2) return false
            if (a == 198 && b in 18..19) return false
            if (a == 198 && b == 51) return false
            if (a == 203 && b == 0) return false
        } else {
            val first = bytes[0]
            if ((first and 0xfe) == 0xfc || first == 0xff) return false
            if (first == 0x20 && bytes[1] == 0x01 && bytes[2] == 0x0d && bytes[3] == 0xb8) return false
        }
        return true
    }

    private fun validatePublicHost(host: String) {
        val addresses = InetAddress.getAllByName(host)
        require(addresses.isNotEmpty() && addresses.all { address ->
            !address.isAnyLocalAddress && !address.isLoopbackAddress &&
                !address.isLinkLocalAddress && !address.isSiteLocalAddress &&
                !address.isMulticastAddress &&
                !(address.address.size == 16 && (address.address[0].toInt() and 0xfe) == 0xfc)
        }) { "RDAP service resolved to a non-public network destination; request blocked." }
    }
}
