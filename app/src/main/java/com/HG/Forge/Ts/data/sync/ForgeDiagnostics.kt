package com.HG.Forge.Ts.data.sync

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.*
import com.HG.Forge.Ts.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

object ForgeDiagnosticsConsent {
    private const val PREFS = "forge_diagnostics"
    private const val ENABLED = "diagnostic_upload_enabled"

    fun isEnabled(context: Context): Boolean {
        if (!BuildConfig.DIAGNOSTICS_UPLOAD_ENABLED) return false
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(ENABLED, true)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(ENABLED, enabled).apply()
        if (enabled) ForgeDiagnosticsUploadScheduler.enqueue(context)
    }
}

object ForgeDiagnostics {
    private const val FILE_NAME = "forge-diagnostic-events.json"
    private const val MAX_EVENTS = 200
    private const val MAX_FILE_BYTES = 128 * 1024

    fun log(
        context: Context,
        level: String,
        tag: String,
        event: String,
        throwable: Throwable? = null
    ) {
        val normalizedLevel = level.uppercase().take(8)
        val safeTag = tag.replace(Regex("[^A-Za-z0-9_.-]"), "_").take(48)
        val safeEvent = event.replace(Regex("[^A-Za-z0-9_.:-]"), "_").take(120)
        val priority = when (normalizedLevel) {
            "ERROR" -> Log.ERROR
            "WARNING", "WARN" -> Log.WARN
            "DEBUG" -> Log.DEBUG
            else -> Log.INFO
        }
        val logMessage = "event=$safeEvent"
        if (throwable == null) Log.println(priority, "FORGE/$safeTag", logMessage)
        else Log.println(priority, "FORGE/$safeTag", "$logMessage exception=${throwable.javaClass.name}")

        if (!BuildConfig.DEBUG) return
        runCatching {
            val eventJson = JSONObject()
                .put("eventId", UUID.randomUUID().toString())
                .put("installationId", installationId(context))
                .put("occurredAt", Instant.now().toString())
                .put("appVersion", appVersion(context))
                .put("androidSdk", Build.VERSION.SDK_INT)
                .put("androidRelease", Build.VERSION.RELEASE.orEmpty().take(32))
                .put("level", if (normalizedLevel == "WARN") "WARNING" else normalizedLevel)
                .put("tag", safeTag)
                .put("eventName", safeEvent)
                .put("exceptionType", throwable?.javaClass?.name?.take(200) ?: "")
                .put("stackTrace", throwable?.let(::safeStackTrace) ?: "")
            append(context, eventJson)
            if (ForgeDiagnosticsConsent.isEnabled(context) &&
                (normalizedLevel == "ERROR" || normalizedLevel == "WARNING" || normalizedLevel == "WARN" ||
                    safeEvent == "application.on_create" || safeEvent == "activity.on_create.enter")) {
                ForgeDiagnosticsUploadScheduler.enqueue(context)
            }
        }
    }

    private fun installationId(context: Context): String {
        val prefs = context.getSharedPreferences("forge_diagnostics", Context.MODE_PRIVATE)
        val current = prefs.getString("installation_id", null)
        if (!current.isNullOrBlank()) return current
        val created = UUID.randomUUID().toString()
        prefs.edit().putString("installation_id", created).commit()
        return created
    }

    private fun appVersion(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    }.getOrDefault("unknown").take(32)

    private fun safeStackTrace(error: Throwable): String {
        val out = StringBuilder()
        val seen = HashSet<Throwable>()
        var current: Throwable? = error
        var depth = 0
        while (current != null && seen.add(current) && depth < 4 && out.length < 12000) {
            if (depth > 0) out.append("\nCaused by: ")
            out.append(current.javaClass.name).append('\n')
            current.stackTrace.take(50).forEach { frame ->
                if (out.length < 12000) {
                    out.append(" at ").append(frame.className).append('.')
                        .append(frame.methodName).append('(')
                        .append(frame.fileName ?: "Unknown")
                    if (frame.lineNumber >= 0) out.append(':').append(frame.lineNumber)
                    out.append(")\n")
                }
            }
            current = current.cause
            depth++
        }
        return out.toString().take(12000)
    }

    @Synchronized
    private fun append(context: Context, event: JSONObject) {
        val file = File(context.filesDir, FILE_NAME)
        val events = read(context)
        events.put(event)
        while (events.length() > MAX_EVENTS) events.remove(0)
        var encoded = events.toString()
        while (encoded.toByteArray(Charsets.UTF_8).size > MAX_FILE_BYTES && events.length() > 1) {
            events.remove(0)
            encoded = events.toString()
        }
        val temp = File(context.filesDir, "$FILE_NAME.tmp")
        FileOutputStream(temp).use { stream ->
            stream.write(encoded.toByteArray(Charsets.UTF_8))
            stream.fd.sync()
        }
        if (!temp.renameTo(file)) {
            file.writeText(encoded, Charsets.UTF_8)
            temp.delete()
        }
    }

    @Synchronized
    fun read(context: Context): JSONArray = runCatching {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.isFile || file.length() > MAX_FILE_BYTES) JSONArray()
        else JSONArray(file.readText(Charsets.UTF_8))
    }.getOrDefault(JSONArray())

    @Synchronized
    fun acknowledge(context: Context, uploadedIds: Set<String>) {
        val remaining = JSONArray()
        val current = read(context)
        for (index in 0 until current.length()) {
            val item = current.optJSONObject(index) ?: continue
            if (!uploadedIds.contains(item.optString("eventId"))) remaining.put(item)
        }
        val file = File(context.filesDir, FILE_NAME)
        if (remaining.length() == 0) {
            file.delete()
        } else {
            file.writeText(remaining.toString(), Charsets.UTF_8)
        }
    }
}

object ForgeDiagnosticsUploadScheduler {
    fun enqueue(context: Context) {
        if (!ForgeDiagnosticsConsent.isEnabled(context)) return
        val request = OneTimeWorkRequestBuilder<ForgeDiagnosticsUploadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInitialDelay(8, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "forge-ts-diagnostic-upload",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}