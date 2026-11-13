package com.HG.Forge.Ts.data.sync

import android.content.Context
import android.os.Build
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.UUID

object CrashReportConsent {
    fun isEnabled(context: Context): Boolean = ForgeDiagnosticsConsent.isEnabled(context)
}

internal object CrashReportStore {
    private const val PREFS = "forge_crash_telemetry"
    private const val INSTALLATION_ID = "installation_id"
    private const val REPORT_FILE = "forge-crash-report.json"
    private const val REPORT_PREFIX = "forge-crash-report-"
    private const val MAX_PENDING_REPORTS = 5

    fun capture(context: Context, throwable: Throwable) {
        runCatching {
            val report = JSONObject()
                .put("crashId", UUID.randomUUID().toString())
                .put("installationId", installationId(context))
                .put("occurredAt", Instant.now().toString())
                .put("appVersion", appVersion(context))
                .put("androidSdk", Build.VERSION.SDK_INT)
                .put("androidRelease", Build.VERSION.RELEASE.orEmpty().take(32))
                .put("exceptionType", throwable.javaClass.name.take(200))
                .put("stackTrace", safeStackTrace(throwable))
                .put("uploadedAnonymously", false)
            val pending = pendingFiles(context)
            if (pending.size >= MAX_PENDING_REPORTS) {
                Log.w("FORGE/CrashStore", "Pending crash queue is full; newest report was not persisted")
                return@runCatching
            }
            val filename = "$REPORT_PREFIX${System.currentTimeMillis()}-${UUID.randomUUID()}.json"
            writeAtomically(File(context.filesDir, filename), report.toString())
        }
    }

    @Synchronized
    fun read(context: Context): JSONObject? = runCatching {
        val file = pendingFiles(context).firstOrNull() ?: return@runCatching null
        if (file.length() > 48 * 1024) null
        else JSONObject(file.readText(Charsets.UTF_8))
    }.getOrNull()

    @Synchronized
    fun markAnonymousUploaded(context: Context, crashId: String) {
        updateReport(context, crashId) { it.put("uploadedAnonymously", true) }
    }

    @Synchronized
    fun readNextForAnonymousUpload(context: Context): JSONObject? = runCatching {
        val file = pendingFiles(context).firstOrNull { candidate ->
            runCatching {
                candidate.length() <= 48 * 1024 &&
                    !JSONObject(candidate.readText(Charsets.UTF_8)).optBoolean("uploadedAnonymously")
            }.getOrDefault(false)
        } ?: return@runCatching null
        JSONObject(file.readText(Charsets.UTF_8))
    }.getOrNull()

    @Synchronized
    fun markRecoveryAcknowledged(context: Context) {
        val file = pendingFiles(context).firstOrNull() ?: return
        val report = JSONObject(file.readText(Charsets.UTF_8))
        report.put("recoveryAcknowledged", true)
        writeAtomically(file, report.toString())
    }

    @Synchronized
    fun clear(context: Context, crashId: String) {
        val file = findReport(context, crashId)
            ?: throw IOException("Crash report is unavailable for removal")
        if (!file.delete() && file.exists()) {
            throw IOException("Unable to remove uploaded crash report")
        }
    }

    private fun updateReport(context: Context, crashId: String, update: (JSONObject) -> Unit) {
        val file = findReport(context, crashId)
            ?: throw IOException("Crash report is unavailable for acknowledgement")
        check(file.length() <= 48 * 1024) { "Crash report exceeds the local size limit" }
        val report = JSONObject(file.readText(Charsets.UTF_8))
        check(report.optString("crashId") == crashId) { "Crash report identity changed" }
        update(report)
        writeAtomically(file, report.toString())
    }

    private fun findReport(context: Context, crashId: String): File? =
        pendingFiles(context).firstOrNull { file ->
            runCatching {
                file.length() <= 48 * 1024 &&
                    JSONObject(file.readText(Charsets.UTF_8)).optString("crashId") == crashId
            }.getOrDefault(false)
        }

    private fun pendingFiles(context: Context): List<File> {
        val legacy = File(context.filesDir, REPORT_FILE)
        val queued = context.filesDir.listFiles { file ->
            file.isFile && file.name.startsWith(REPORT_PREFIX) && file.name.endsWith(".json")
        }?.toList().orEmpty()
        return (listOfNotNull(legacy.takeIf { it.isFile }) + queued)
            .distinctBy { it.absolutePath }
            .sortedBy { it.lastModified() }
    }

    private fun installationId(context: Context): String {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = preferences.getString(INSTALLATION_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val created = UUID.randomUUID().toString()
        preferences.edit().putString(INSTALLATION_ID, created).commit()
        return created
    }

    private fun appVersion(context: Context): String = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.versionName ?: "unknown"
    }.getOrDefault("unknown").take(32)

    private fun safeStackTrace(root: Throwable): String {
        val output = StringBuilder()
        val seen = HashSet<Throwable>()
        var current: Throwable? = root
        var causes = 0
        while (current != null && seen.add(current) && causes < 5 && output.length < 30000) {
            if (causes > 0) output.append("\nCaused by: ")
            output.append(current.javaClass.name).append('\n')
            current.stackTrace.take(80).forEach { frame ->
                if (output.length < 30000) {
                    output.append(" at ").append(frame.className).append('.')
                        .append(frame.methodName).append('(')
                        .append(frame.fileName ?: "Unknown Source")
                    if (frame.lineNumber >= 0) output.append(':').append(frame.lineNumber)
                    output.append(")\n")
                }
            }
            current = current.cause
            causes++
        }
        return output.toString().take(32000)
    }

    private fun writeAtomically(target: File, content: String) {
        val temporary = File(target.parentFile, "${target.name}.tmp")
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
