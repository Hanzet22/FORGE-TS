package com.HG.Forge.Ts.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
            currentCoroutineContext().ensureActive()
            if (!ForgeDiagnosticsConsent.isEnabled(applicationContext)) return@withContext Result.success()
            val all = ForgeDiagnostics.read(applicationContext)
            if (all.length() == 0) return@withContext Result.success()

            // Send the complete bounded local journal as one diagnostic envelope.
            // The local journal is capped below the Edge Function request-size limit.
            val uploadedIds = mutableSetOf<String>()
            for (index in 0 until all.length()) {
                all.optJSONObject(index)?.let { uploadedIds.add(it.optString("eventId")) }
            }
            if (uploadedIds.isEmpty()) return@withContext Result.success()

            try {
                currentCoroutineContext().ensureActive()
                if (!ForgeDiagnosticsConsent.isEnabled(applicationContext)) return@withContext Result.success()
                val payload = JSONObject()
                    .put("schemaVersion", 1)
                    .put("deliveryMode", "app_background_session")
                    .put("events", all)
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
                currentCoroutineContext().ensureActive()
                when {
                    code in 200..299 -> ForgeDiagnostics.acknowledge(applicationContext, uploadedIds)
                    code == 429 || code >= 500 -> return@withContext Result.retry()
                    else -> return@withContext Result.failure()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
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