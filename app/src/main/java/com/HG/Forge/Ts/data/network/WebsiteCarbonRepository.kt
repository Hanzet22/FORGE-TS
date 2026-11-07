package com.HG.Forge.Ts.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.net.URL

data class WebsiteCarbonReport(
    val pageUrl: String,
    val bytes: Long?,
    val greenHosting: Boolean?,
    val rating: String?,
    val cleanerThanPercent: Double?,
    val gridCo2Grams: Double?,
    val renewableCo2Grams: Double?,
    val provider: String = "Website Carbon API"
)

/**
 * Optional third-party page-load estimate. The target URL is disclosed to Website Carbon.
 * This is a provider estimate, not a measurement performed by FORGE-TS.
 */
class WebsiteCarbonRepository {
    suspend fun estimate(pageUrl: String): WebsiteCarbonReport = withContext(Dispatchers.IO) {
        val uri = URI(pageUrl)
        require(uri.scheme == "https" && uri.host != null) {
            "Carbon estimation accepts HTTPS page URLs only."
        }
        val endpoint = URL("https://api.websitecarbon.com/site?url=" +
            URLEncoder.encode(uri.toString(), "UTF-8"))
        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = 12000
            readTimeout = 12000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "FORGE-TS-Carbon-Estimate/1.0")
            useCaches = false
        }
        try {
            val status = connection.responseCode
            require(status in 200..299) { "Website Carbon service returned HTTP $status." }
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(2048)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count <= 0) break
                    total += count
                    require(total <= 65536) { "Carbon API response exceeded 64 KiB." }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val json = JSONObject(bytes.toString(Charsets.UTF_8))
            val stats = json.optJSONObject("statistics") ?: JSONObject()
            val co2 = stats.optJSONObject("co2") ?: JSONObject()
            val grid = co2.optJSONObject("grid") ?: JSONObject()
            val renewable = co2.optJSONObject("renewable") ?: JSONObject()
            val cleaner = json.optDouble("cleanerThan", Double.NaN).takeUnless(Double::isNaN)
                ?.let { (it * 100.0).coerceIn(0.0, 100.0) }
            WebsiteCarbonReport(
                pageUrl = json.optString("url").takeIf(String::isNotBlank) ?: pageUrl,
                bytes = json.optLong("bytes", -1L).takeIf { it >= 0L },
                greenHosting = if (json.has("green") && !json.isNull("green")) json.optBoolean("green") else null,
                rating = json.optString("rating").takeIf(String::isNotBlank),
                cleanerThanPercent = cleaner,
                gridCo2Grams = grid.optDouble("grams", Double.NaN).takeUnless(Double::isNaN),
                renewableCo2Grams = renewable.optDouble("grams", Double.NaN).takeUnless(Double::isNaN)
            )
        } finally {
            connection.disconnect()
        }
    }
}
