package com.HG.Forge.Ts.data.sync

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

object ForgeDiagnosticsConsent {
    private const val PREFS = "forge_diagnostics_consent"
    private const val KEY_UPLOADS_ENABLED = "uploads_enabled"

    fun hasDecision(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(KEY_UPLOADS_ENABLED)

    /** Remote telemetry is opt-in per installation; local diagnostics remain enabled. */
    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_UPLOADS_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val wasEnabled = preferences.getBoolean(KEY_UPLOADS_ENABLED, false)
        if (wasEnabled == enabled) {
            if (enabled) {
                ForgeDiagnosticsUploadScheduler.enqueue(context)
                CrashReportUploadScheduler.enqueue(context)
            }
            return
        }
        if (enabled) {
            ForgeDiagnostics.clearPendingUploadJournal(context)
            check(preferences.edit().putBoolean(KEY_UPLOADS_ENABLED, true).commit()) {
                "Unable to persist diagnostics consent."
            }
            ForgeDiagnosticsUploadScheduler.enqueue(context)
            CrashReportUploadScheduler.enqueue(context)
        } else {
            check(preferences.edit().putBoolean(KEY_UPLOADS_ENABLED, false).commit()) {
                "Unable to persist diagnostics consent."
            }
            WorkManager.getInstance(context).cancelUniqueWork("forge-ts-diagnostic-upload")
            WorkManager.getInstance(context).cancelUniqueWork("forge-ts-crash-report-upload")
            ForgeDiagnostics.clearPendingUploadJournal(context)
            CrashReportStore.disableRemoteUploadForPending(context)
        }
    }
}

object ForgeDiagnostics {
    private val processSessionId: String = UUID.randomUUID().toString()

    /** Generate one correlation ID and pass it to related log calls in the same operation. */
    fun newTraceId(): String = UUID.randomUUID().toString()

    private fun inferCategory(tag: String, event: String, level: String): String {
        if (level == "CRASH" || tag.contains("Crash", ignoreCase = true)) return "CRASH"
        return when {
            tag.contains("Application", ignoreCase = true) || event.startsWith("session.") || event.startsWith("application.") -> "APP_LIFECYCLE"
            tag.contains("Activity", ignoreCase = true) || event.startsWith("activity.") -> "ACTIVITY"
            tag.contains("Navigation", ignoreCase = true) || event.startsWith("navigation.") -> "NAVIGATION"
            tag.contains("Network", ignoreCase = true) || event.startsWith("network.") -> "NETWORK"
            tag.contains("Dns", ignoreCase = true) || event.startsWith("dns.") -> "DNS"
            tag.contains("Runtime", ignoreCase = true) || tag.contains("Pty", ignoreCase = true) || tag.contains("Proot", ignoreCase = true) -> "RUNTIME"
            tag.contains("Media", ignoreCase = true) || tag.contains("Video", ignoreCase = true) || tag.contains("Intro", ignoreCase = true) -> "MEDIA"
            tag.contains("Work", ignoreCase = true) || event.startsWith("background.") -> "BACKGROUND"
            tag.contains("Storage", ignoreCase = true) || tag.contains("Database", ignoreCase = true) -> "STORAGE"
            tag.contains("Telemetry", ignoreCase = true) || tag.contains("Diagnostic", ignoreCase = true) -> "TELEMETRY"
            else -> "FEATURE"
        }
    }

    private const val FILE_NAME = "forge-diagnostic-events.json"
    private const val MAX_EVENTS = 200
    private const val MAX_FILE_BYTES = 48 * 1024

    fun log(
        context: Context,
        level: String,
        tag: String,
        event: String,
        throwable: Throwable? = null,
        traceId: String? = null
    ) {
        val normalizedLevel = level.uppercase().take(8)
        val safeTag = tag.replace(Regex("[^A-Za-z0-9_.-]"), "_").take(48)
        val safeEvent = event.replace(Regex("[^A-Za-z0-9_.:-]"), "_").take(120)
        val priority = when (normalizedLevel) {
            "ERROR", "CRITICAL", "CRASH" -> Log.ERROR
            "WARNING", "WARN" -> Log.WARN
            "DEBUG" -> Log.DEBUG
            else -> Log.INFO
        }
        val logMessage = "event=$safeEvent"
        if (throwable == null) Log.println(priority, "FORGE/$safeTag", logMessage)
        else Log.println(priority, "FORGE/$safeTag", "$logMessage exception=${throwable.javaClass.name}")

        runCatching {
            val eventJson = JSONObject()
                .put("eventId", UUID.randomUUID().toString())
                .put("installationId", installationId(context))
                .put("sessionId", processSessionId)
                .put("occurredAt", Instant.now().toString())
                .put("appVersion", appVersion(context))
                .put("androidSdk", Build.VERSION.SDK_INT)
                .put("androidRelease", Build.VERSION.RELEASE.orEmpty().take(32))
                .put("deviceBrand", Build.MANUFACTURER.orEmpty().take(48))
                .put("deviceModel", Build.MODEL.orEmpty().take(80))
                .put("deviceRamMb", totalRamMb(context))
                .put("displayResolution", "${context.resources.displayMetrics.widthPixels}x${context.resources.displayMetrics.heightPixels}")
                .put("cpuChipset", chipsetModel())
                .put("cpuArchitecture", Build.SUPPORTED_ABIS.firstOrNull().orEmpty().take(40))
                .put("level", if (normalizedLevel == "WARN") "WARNING" else normalizedLevel)
                .put("category", inferCategory(safeTag, safeEvent, normalizedLevel))
                .put("tag", safeTag)
                .put("eventName", safeEvent)
                .put("exceptionType", throwable?.javaClass?.name?.take(200) ?: "")
                .put("stackTrace", throwable?.let(::safeStackTrace) ?: "")
            if (!traceId.isNullOrBlank()) eventJson.put("traceId", traceId.take(36))
            append(context, eventJson)
            if (ForgeDiagnosticsConsent.isEnabled(context) &&
                (normalizedLevel == "ERROR" || normalizedLevel == "WARNING" || normalizedLevel == "WARN" || normalizedLevel == "CRITICAL" || normalizedLevel == "CRASH" ||
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

    private fun totalRamMb(context: Context): Long = runCatching {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(memory)
        memory.totalMem / (1024L * 1024L)
    }.getOrDefault(0L)

    private fun chipsetModel(): String = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else Build.HARDWARE
    }.getOrDefault(Build.HARDWARE.orEmpty()).ifBlank { "unknown" }.take(80)

    private fun appVersion(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    }.getOrDefault("unknown").take(32)

    private fun safeStackTrace(error: Throwable): String {
        val out = StringBuilder()
        val seen = HashSet<Throwable>()
        var current: Throwable? = error
        var depth = 0
        while (current != null && seen.add(current) && depth < 3 && out.length < 3000) {
            if (depth > 0) out.append("\nCaused by: ")
            out.append(current.javaClass.name).append('\n')
            current.stackTrace.take(20).forEach { frame ->
                if (out.length < 3000) {
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
        return out.toString().take(3000)
    }

    @Synchronized
    private fun append(context: Context, event: JSONObject) {
        val events = read(context)
        events.put(event)
        while (events.length() > MAX_EVENTS) events.remove(0)
        var encoded = events.toString()
        while (encoded.toByteArray(Charsets.UTF_8).size > MAX_FILE_BYTES && events.length() > 1) {
            events.remove(0)
            encoded = events.toString()
        }
        writeJournalAtomically(context, encoded)
    }

    @Synchronized
    fun read(context: Context): JSONArray = runCatching {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.isFile || file.length() > MAX_FILE_BYTES) JSONArray()
        else JSONArray(file.readText(Charsets.UTF_8))
    }.getOrDefault(JSONArray())

    @Synchronized
    fun clearPendingUploadJournal(context: Context) {
        val journal = File(context.filesDir, FILE_NAME)
        val temporary = File(context.filesDir, "$FILE_NAME.tmp")
        listOf(journal, temporary).forEach { file ->
            if (file.exists() && !file.delete() && file.exists()) {
                throw IOException("Unable to clear pending diagnostics journal")
            }
        }
    }

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
            if (!file.delete() && file.exists()) {
                throw IOException("Unable to remove acknowledged diagnostic journal")
            }
        } else {
            writeJournalAtomically(context, remaining.toString())
        }
    }

    private fun writeJournalAtomically(context: Context, content: String) {
        val target = File(context.filesDir, FILE_NAME)
        val temporary = File(context.filesDir, "$FILE_NAME.tmp")
        FileOutputStream(temporary).use { stream ->
            stream.write(content.toByteArray(Charsets.UTF_8))
            stream.fd.sync()
        }
        try {
            Files.move(
                temporary.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
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