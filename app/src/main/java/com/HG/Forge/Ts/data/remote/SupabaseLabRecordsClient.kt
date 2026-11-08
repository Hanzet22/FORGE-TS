package com.HG.Forge.Ts.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.json.JSONArray
import org.json.JSONObject

data class RemoteLabRecord(
    val id: String,
    val label: String,
    val value: String,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Supabase REST client for the RLS-protected FORGE-TS lab table.
 * All calls require the current user's access token so Postgres RLS can
 * enforce owner_id = auth.uid(). The publishable key is not an auth token.
 */
class SupabaseLabRecordsClient(
    private val supabaseUrl: String,
    private val publishableKey: String,
    private val httpClient: HttpClient
) {
    private val tableUrl: String
        get() = "${supabaseUrl.trimEnd('/')}/rest/v1/forge_ts_lab_records"

    suspend fun select(accessToken: String): List<RemoteLabRecord> {
        val response = httpClient.get(tableUrl) {
            auth(accessToken)
            parameter("select", "id,label,value,created_at,updated_at")
            parameter("order", "created_at.desc")
            parameter("limit", "100")
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status.value, body)
        val rows = JSONArray(body)
        return List(rows.length()) { index -> rows.getJSONObject(index).toRecord() }
    }

    suspend fun insert(
        accessToken: String,
        label: String,
        value: String
    ): RemoteLabRecord {
        require(label.isNotBlank() && label.length <= 80)
        require(value.length <= 500)
        val response = httpClient.post(tableUrl) {
            auth(accessToken)
            header("Prefer", "return=representation")
            contentType(ContentType.Application.Json)
            setBody(JSONObject().put("label", label.trim()).put("value", value.trim()).toString())
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status.value, body)
        return JSONArray(body).getJSONObject(0).toRecord()
    }

    suspend fun update(
        accessToken: String,
        id: String,
        label: String,
        value: String
    ): Boolean {
        require(label.isNotBlank() && label.length <= 80)
        require(value.length <= 500)
        val response = httpClient.patch(tableUrl) {
            auth(accessToken)
            parameter("id", "eq.$id")
            header("Prefer", "return=representation")
            contentType(ContentType.Application.Json)
            setBody(JSONObject().put("label", label.trim()).put("value", value.trim()).toString())
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status.value, body)
        return JSONArray(body).length() > 0
    }

    suspend fun delete(accessToken: String, id: String): Boolean {
        val response = httpClient.delete(tableUrl) {
            auth(accessToken)
            parameter("id", "eq.$id")
            header("Prefer", "return=representation")
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status.value, body)
        return JSONArray(body).length() > 0
    }

    private fun HttpRequestBuilder.auth(accessToken: String) {
        require(supabaseUrl.startsWith("https://"))
        require(publishableKey.isNotBlank())
        require(accessToken.isNotBlank())
        header("apikey", publishableKey)
        header("Authorization", "Bearer $accessToken")
    }

    private fun ensureSuccess(statusCode: Int, body: String) {
        if (statusCode !in 200..299) {
            val errorCode = runCatching { JSONObject(body).optString("code") }
                .getOrDefault("")
                .ifBlank { "supabase_request_failed" }
            throw IllegalStateException("$errorCode (HTTP $statusCode)")
        }
    }

    private fun JSONObject.toRecord() = RemoteLabRecord(
        id = getString("id"),
        label = getString("label"),
        value = getString("value"),
        createdAt = getString("created_at"),
        updatedAt = getString("updated_at")
    )
}
