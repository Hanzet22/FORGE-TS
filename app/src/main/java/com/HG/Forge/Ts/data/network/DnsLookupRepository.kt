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
    val status: Int,
    val provider: String
)

class DnsLookupRepository {
    private val recordTypes = mapOf(
        "A" to 1,
        "AAAA" to 28,
        "CNAME" to 5,
        "MX" to 15,
        "TXT" to 16,
        "NS" to 2,
        "SRV" to 33,
        "CAA" to 257
    )

    suspend fun lookup(input: String, recordType: String): DnsLookupResult =
        withContext(Dispatchers.IO) {
            val normalized = normalizeDomain(input)
            val typeCode = recordTypes[recordType]
                ?: throw IllegalArgumentException("Unsupported DNS record type")

            val response = withTimeout(8_000) {
                HttpClientProvider.client.get("https://cloudflare-dns.com/dns-query") {
                    parameter("name", normalized)
                    parameter("type", typeCode)
                    header("accept", "application/dns-json")
                }.bodyAsText()
            }

            val json = JSONObject(response)
            val status = json.optInt("Status", -1)
            val answerArray = json.optJSONArray("Answer")
            val answers = buildList {
                if (answerArray != null) {
                    for (index in 0 until answerArray.length()) {
                        val item = answerArray.optJSONObject(index) ?: continue
                        add(
                            DnsAnswer(
                                name = item.optString("name"),
                                type = recordTypes.entries.firstOrNull {
                                    it.value == item.optInt("type")
                                }?.key ?: "TYPE ${item.optInt("type")}",
                                ttl = item.optLong("TTL"),
                                data = item.optString("data")
                            )
                        )
                    }
                }
            }

            DnsLookupResult(
                domain = normalized,
                recordType = recordType,
                answers = answers,
                status = status,
                provider = "Cloudflare DNS over HTTPS"
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
