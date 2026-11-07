package com.HG.Forge.Ts.data.network

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.net.IDN
import android.os.SystemClock

data class DnsAnswer(
    val name: String,
    val type: String,
    val ttl: Long,
    val data: String
)

data class DnsLookupResult(
    val domain: String,
    val recordType: String,
    val answers: List<DnsAnswer>,
    val authority: List<DnsAnswer>,
    val additional: List<DnsAnswer>,
    val status: Int,
    val provider: String,
    val elapsedMs: Long,
    val dnssecAuthenticated: Boolean,
    val checkingDisabled: Boolean
)

enum class DnsResolver(val label: String, val endpoint: String) {
    CLOUDFLARE("Cloudflare", "https://cloudflare-dns.com/dns-query"),
    GOOGLE("Google Public DNS", "https://dns.google/resolve")
}

class DnsLookupRepository {
    private val recordTypes = mapOf(
        "A" to 1, "NS" to 2, "CNAME" to 5, "SOA" to 6, "PTR" to 12,
        "MX" to 15, "TXT" to 16, "AAAA" to 28, "SRV" to 33,
        "DS" to 43, "SSHFP" to 44, "RRSIG" to 46, "NSEC" to 47,
        "DNSKEY" to 48, "TLSA" to 52, "SVCB" to 64, "HTTPS" to 65,
        "CAA" to 257
    )

    suspend fun lookup(
        input: String,
        recordType: String,
        resolver: DnsResolver = DnsResolver.CLOUDFLARE
    ): DnsLookupResult = withContext(Dispatchers.IO) {
        val normalized = normalizeDomain(input)
        val typeCode = recordTypes[recordType]
            ?: throw IllegalArgumentException("Unsupported DNS record type")
        val started = SystemClock.elapsedRealtime()
        val httpResponse = withTimeout(10_000) {
            HttpClientProvider.client.get(resolver.endpoint) {
                parameter("name", normalized)
                parameter("type", typeCode)
                parameter("do", "true")
                header("Accept", "application/dns-json")
            }
        }
        val response = httpResponse.bodyAsText()
        val elapsed = SystemClock.elapsedRealtime() - started
        val httpStatus = httpResponse.status.value
        val contentType = httpResponse.headers["Content-Type"] ?: "unknown"

        if (httpStatus !in 200..299) {
            val resolverMessage = runCatching {
                JSONObject(response).optString("error").takeIf { it.isNotBlank() }
            }.getOrNull()
            val rawBody = response
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(300)
            val diagnostic = resolverMessage
                ?: rawBody.takeIf { it.isNotBlank() }
                ?: "response body was empty"
            val server = httpResponse.headers["Server"]
            val cfRay = httpResponse.headers["CF-Ray"]
            val contentLength = httpResponse.headers["Content-Length"]
            val metadata = buildList {
                if (server != null) add("server=$server")
                if (cfRay != null) add("cf-ray=$cfRay")
                if (contentLength != null) add("content-length=$contentLength")
            }.joinToString(", ").takeIf { it.isNotBlank() }
            val detail = ": $diagnostic"
            val headerDetail = metadata?.let { " [$it]" } ?: ""
            throw IllegalStateException(
                "${resolver.label} returned HTTP $httpStatus ($contentType)$headerDetail$detail"
            )
        }
        if (response.isBlank()) {
            throw IllegalStateException(
                "${resolver.label} returned an empty response (HTTP $httpStatus, content-type $contentType)"
            )
        }
        val json = try {
            JSONObject(response)
        } catch (_: Exception) {
            throw IllegalStateException(
                "${resolver.label} returned a non-JSON response (HTTP $httpStatus, content-type $contentType)"
            )
        }
        val status = json.optInt("Status", -1)

        fun parseSection(section: String): List<DnsAnswer> {
            val array = json.optJSONArray(section) ?: return emptyList()
            return buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val code = item.optInt("type", -1)
                    val type = recordTypes.entries.firstOrNull { it.value == code }?.key
                        ?: "TYPE $code"
                    add(DnsAnswer(
                        name = item.optString("name"),
                        type = type,
                        ttl = item.optLong("TTL", item.optLong("ttl", 0L)),
                        data = item.optString("data")
                    ))
                }
            }
        }

        DnsLookupResult(
            domain = normalized,
            recordType = recordType,
            answers = parseSection("Answer"),
            authority = parseSection("Authority"),
            additional = parseSection("Additional"),
            status = status,
            provider = resolver.label,
            elapsedMs = elapsed,
            dnssecAuthenticated = json.optBoolean("AD", false),
            checkingDisabled = json.optBoolean("CD", false)
        )
    }

    private fun normalizeDomain(input: String): String {
        val trimmed = input.trim().trimEnd('.')
        require(trimmed.isNotBlank()) { "Enter a domain name" }
        require(trimmed.length <= 253 && !trimmed.contains('/') && !trimmed.contains(':')) {
            "Enter a domain name without a URL, path or port"
        }
        val ascii = try {
            IDN.toASCII(trimmed, IDN.USE_STD3_ASCII_RULES).lowercase()
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("The domain name is not valid")
        }
        val labels = ascii.split('.')
        require(labels.size >= 2 && labels.all { label ->
            label.length in 1..63 &&
                label.firstOrNull()?.isLetterOrDigit() == true &&
                label.lastOrNull()?.isLetterOrDigit() == true &&
                label.all { it.isLetterOrDigit() || it == '-' }
        }) { "Enter a valid fully qualified domain name" }
        return ascii
    }
}
