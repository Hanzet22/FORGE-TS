package com.HG.Forge.Ts.data.network

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class PublicIpResult(val address: String, val source: String)

data class HostCheckResult(
    val host: String,
    val addresses: List<String>,
    val port: Int,
    val tcpReachable: Boolean,
    val tlsValid: Boolean?,
    val details: String
)

class CoreNetworkingRepository {
    suspend fun fetchPublicIp(): PublicIpResult = withContext(Dispatchers.IO) {
        val response = HttpClientProvider.client.get("https://api.ipify.org?format=json").bodyAsText()
        val address = JSONObject(response).optString("ip")
        require(address.isNotBlank()) { "The IP lookup service returned no address" }
        PublicIpResult(address, "api.ipify.org")
    }

    suspend fun checkHost(hostInput: String, port: Int, checkTls: Boolean): HostCheckResult =
        withContext(Dispatchers.IO) {
            val host = hostInput.trim().removePrefix("https://").removePrefix("http://").substringBefore("/")
                .substringBefore(":").trim()
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
                val connection = (URL("https://$host:$port/").openConnection() as HttpsURLConnection)
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
