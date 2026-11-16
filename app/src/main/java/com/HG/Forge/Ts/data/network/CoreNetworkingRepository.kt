package com.HG.Forge.Ts.data.network

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class PublicIpResult(
    val ipv4: String?,
    val ipv6: String?,
    val sources: String
) {
    val addresses: List<String> get() = listOfNotNull(ipv4, ipv6)
}

data class HostCheckResult(
    val host: String,
    val addresses: List<String>,
    val port: Int,
    val tcpReachable: Boolean,
    val tlsValid: Boolean?,
    val details: String
)

class CoreNetworkingRepository {
    suspend fun fetchPublicIp(): PublicIpResult = coroutineScope {
        suspend fun query(endpoint: String): String? = runCatching {
            withTimeout(7000) { HttpClientProvider.client.get(endpoint).bodyAsText().trim() }
                .takeIf { it.isNotBlank() && InetAddress.getByName(it).hostAddress != null }
                ?.let { raw ->
                    val address = InetAddress.getByName(raw)
                    require((endpoint.contains("api4") && address is java.net.Inet4Address) ||
                        (endpoint.contains("api6") && address is java.net.Inet6Address))
                    address.hostAddress
                }
        }.getOrNull()
        val v4 = async { query("https://api4.ipify.org") }
        val v6 = async { query("https://api6.ipify.org") }
        val ipv4 = v4.await()
        val ipv6 = v6.await()
        require(ipv4 != null || ipv6 != null) { "Could not retrieve a public IPv4 or IPv6 address. Check your connection and try again." }
        PublicIpResult(ipv4, ipv6, "api4.ipify.org / api6.ipify.org")
    }

    suspend fun checkHost(hostInput: String, port: Int, checkTls: Boolean): HostCheckResult =
        withContext(Dispatchers.IO) {
            val rawHost = hostInput.trim().removePrefix("https://").removePrefix("http://").substringBefore("/")
            val host = if (rawHost.startsWith("[")) rawHost.substringAfter("[").substringBefore("]") else if (rawHost.count { it == ':' } > 1) rawHost else rawHost.substringBefore(":").trim()
            require(host.isNotBlank()) { "Enter a hostname or IP address" }
            require(host.length <= 253 && host.none { it.isWhitespace() }) { "Invalid hostname" }
            require(port in 1..65535) { "Port must be between 1 and 65535" }

            val addresses = InetAddress.getAllByName(host).map { it.hostAddress ?: it.hostName }
            val socket = Socket()
            val reachable = try {
                socket.connect(InetSocketAddress(host, port), 4000)
                true
            } catch (_: Exception) {
                false
            } finally {
                try { socket.close() } catch (_: Exception) { }
            }

            var tlsValid: Boolean? = null
            var details = if (reachable) "TCP connection succeeded." else "TCP connection failed or timed out."
            if (checkTls) {
                val urlHost = if (host.contains(":")) "[$host]" else host
                val connection = (URL("https://$urlHost:$port/").openConnection() as HttpsURLConnection)
                try {
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.instanceFollowRedirects = false
                    connection.connect()
                    tlsValid = connection.serverCertificates.isNotEmpty()
                    details += if (tlsValid == true) " TLS certificate and hostname verification succeeded." else " TLS certificate was not available."
                } catch (error: Exception) {
                    tlsValid = false
                    details += " TLS verification failed: ${error.javaClass.simpleName}."
                } finally {
                    connection.disconnect()
                }
            }
            HostCheckResult(host, addresses, port, reachable, tlsValid, details)
        }
}
