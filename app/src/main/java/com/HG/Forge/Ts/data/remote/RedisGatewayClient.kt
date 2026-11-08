package com.HG.Forge.Ts.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.headers
import org.json.JSONObject

enum class RedisGatewayOperation(val wireValue: String) {
    SET("set"), GET("get"), DEL("del"), TTL("ttl"), PING("ping")
}

data class RedisGatewayRequest(
    val operation: RedisGatewayOperation,
    val key: String? = null,
    val value: String? = null,
    val ttlSeconds: Long? = null
)

data class RedisGatewayResult(
    val operation: String,
    val result: Any?,
    val source: String
)

class RedisGatewayException(
    val statusCode: Int,
    val errorCode: String
) : Exception("Redis gateway request failed: $errorCode (HTTP $statusCode)")

/**
 * Calls the authenticated Supabase Edge Function. Pass a real Supabase user
 * access token; never pass a publishable key as the Bearer token.
 *
 * The Upstash REST token remains server-side in Supabase Edge Function secrets.
 */
class RedisGatewayClient(
    private val supabaseUrl: String,
    private val publishableKey: String,
    private val httpClient: HttpClient
) {
    suspend fun execute(
        accessToken: String,
        request: RedisGatewayRequest
    ): RedisGatewayResult {
        require(supabaseUrl.startsWith("https://")) { "Supabase URL must use HTTPS." }
        require(publishableKey.isNotBlank()) { "Supabase publishable key is missing." }
        require(accessToken.isNotBlank()) { "A signed-in user access token is required." }

        if (request.operation != RedisGatewayOperation.PING) {
            require(!request.key.isNullOrBlank() && request.key.length <= 128) {
                "Redis key must contain 1–128 characters."
            }
            require(request.key.matches(Regex("[A-Za-z0-9:_-]+"))) {
                "Redis key contains unsupported characters."
            }
        }
        if (request.operation == RedisGatewayOperation.SET) {
            require(request.value != null && request.value.length <= 4096) {
                "Redis value must contain at most 4096 characters."
            }
            require(request.ttlSeconds == null || request.ttlSeconds in 1..86400) {
                "TTL must be between 1 and 86400 seconds."
            }
        }

        val payload = JSONObject().put("operation", request.operation.wireValue)
        request.key?.let { payload.put("key", it) }
        request.value?.let { payload.put("value", it) }
        request.ttlSeconds?.let { payload.put("ttlSeconds", it) }

        val response = httpClient.post(
            "${supabaseUrl.trimEnd('/')}/functions/v1/forge-redis-gateway"
        ) {
            headers {
                append("apikey", publishableKey)
                append("Authorization", "Bearer $accessToken")
            }
            contentType(ContentType.Application.Json)
            setBody(payload.toString())
        }

        val responseBody = response.bodyAsText()
        val json = runCatching { JSONObject(responseBody) }.getOrNull()
        if (response.status.value !in 200..299 || json == null || !json.optBoolean("ok")) {
            throw RedisGatewayException(
                statusCode = response.status.value,
                errorCode = json?.optString("error")?.takeIf { it.isNotBlank() }
                    ?: "invalid_gateway_response"
            )
        }

        val result = json.opt("result").let {
            if (it == null || it === JSONObject.NULL) null else it
        }
        return RedisGatewayResult(
            operation = json.optString("operation", request.operation.wireValue),
            result = result,
            source = json.optString("source", "unknown")
        )
    }
}
