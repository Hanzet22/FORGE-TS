package com.HG.Forge.Ts.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

object ForgeCrashBackendConfig {
    const val SUPABASE_URL = "https://hzwyxvdeeetkxhijgxwy.supabase.co"
    const val PUBLISHABLE_KEY = "sb_publishable_5rbPwWvKvIo7i4mDs9x7Ww_SNw1VFNO"
    const val CRASH_INGEST_URL = "$SUPABASE_URL/functions/v1/forge-crash-ingest"
}

class CrashReportUploadWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!CrashReportConsent.isEnabled(applicationContext)) return@withContext Result.success()
        val report = CrashReportStore.read(applicationContext) ?: return@withContext Result.success()
        if (report.optBoolean("uploadedAnonymously")) return@withContext Result.success()
        try {
            val responseCode = postCrashReport(report, accessToken = null, linkOnly = false)
            if (responseCode in 200..299) {
                CrashReportStore.markAnonymousUploaded(applicationContext)
                Result.success()
            } else if (responseCode == 429 || responseCode >= 500) {
                Result.retry()
            } else {
                Result.failure()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

object CrashReportUploadScheduler {
    fun enqueue(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<CrashReportUploadWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "forge-ts-crash-report-upload",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}

object CrashReportUploader {
    suspend fun uploadPending(context: Context, accessToken: String): String? =
        withContext(Dispatchers.IO) {
            if (!CrashReportConsent.isEnabled(context)) return@withContext null
            val report = CrashReportStore.read(context) ?: return@withContext null
            try {
                val linkOnly = report.optBoolean("uploadedAnonymously")
                val code = postCrashReport(report, accessToken, linkOnly)
                if (code in 200..299) {
                    CrashReportStore.clear(context)
                    if (linkOnly) "previous anonymous report linked to your signed-in account"
                    else "report sent with authenticated account context"
                } else null
            } catch (_: Exception) {
                null
            }
        }
}

private fun postCrashReport(
    report: JSONObject,
    accessToken: String?,
    linkOnly: Boolean
): Int {
    val payload = JSONObject(report.toString()).put("linkOnly", linkOnly)
    val connection = URL(ForgeCrashBackendConfig.CRASH_INGEST_URL).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 5000
        connection.readTimeout = 7000
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("apikey", ForgeCrashBackendConfig.PUBLISHABLE_KEY)
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        if (!accessToken.isNullOrBlank()) {
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
        }
        connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        return connection.responseCode
    } finally {
        connection.disconnect()
    }
}
