package com.HG.Forge.Ts.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ForgeDiagnosticsUploadWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!ForgeDiagnosticsConsent.isEnabled(applicationContext)) return@withContext Result.success()

        while (true) {
            val all = ForgeDiagnostics.read(applicationContext)
            if (all.length() == 0) return@withContext Result.success()

            // Four bounded events keep the request below the Edge Function body limit,
            // even when one event contains a useful exception stack trace.
            val batch = JSONArray()
            val uploadedIds = mutableSetOf<String>()
            for (index in 0 until minOf(all.length(), 4)) {
                val event = all.optJSONObject(index) ?: continue
                batch.put(event)
                uploadedIds.add(event.optString("eventId"))
            }
            if (batch.length() == 0) return@withContext Result.success()

            try {
                val payload = JSONObject().put("events", batch)
                val connection = URL(ForgeDiagnosticsBackendConfig.INGEST_URL)
                    .openConnection() as HttpURLConnection
                val code = try {
                    connection.connectTimeout = 5000
                    connection.readTimeout = 7000
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.setRequestProperty("apikey", ForgeDiagnosticsBackendConfig.PUBLISHABLE_KEY)
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Accept", "application/json")
                    connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                    connection.responseCode
                } finally {
                    connection.disconnect()
                }
                when {
                    code in 200..299 -> ForgeDiagnostics.acknowledge(applicationContext, uploadedIds)
                    code == 429 || code >= 500 -> return@withContext Result.retry()
                    else -> return@withContext Result.failure()
                }
            } catch (_: Exception) {
                return@withContext Result.retry()
            }
        }
        @Suppress("UNREACHABLE_CODE")
        Result.success()
    }
}

object ForgeDiagnosticsBackendConfig {
    const val INGEST_URL = "https://hzwyxvdeeetkxhijgxwy.supabase.co/functions/v1/forge-diagnostic-ingest"
    const val PUBLISHABLE_KEY = "sb_publishable_5rbPwWvKvIo7i4mDs9x7Ww_SNw1VFNO"
}