package com.HG.Forge.Ts.data.sync

import android.content.Context
import android.os.Build
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.UUID

object CrashReportConsent {
    private const val PREFS = "forge_crash_telemetry"
    private const val ENABLED = "crash_reports_enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(ENABLED, enabled).apply()
    }
}

internal object CrashReportStore {
    private const val PREFS = "forge_crash_telemetry"
    private const val INSTALLATION_ID = "installation_id"
    private const val REPORT_FILE = "forge-crash-report.json"

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
            writeAtomically(File(context.filesDir, REPORT_FILE), report.toString())
        }
    }

    fun read(context: Context): JSONObject? = runCatching {
        val file = File(context.filesDir, REPORT_FILE)
        if (!file.isFile || file.length() > 48 * 1024) null
        else JSONObject(file.readText(Charsets.UTF_8))
    }.getOrNull()

    fun markAnonymousUploaded(context: Context) {
        runCatching {
            val report = read(context) ?: return
            report.put("uploadedAnonymously", true)
            writeAtomically(File(context.filesDir, REPORT_FILE), report.toString())
        }
    }

    fun clear(context: Context) {
        runCatching { File(context.filesDir, REPORT_FILE).delete() }
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
        if (!temporary.renameTo(target)) {
            target.writeText(content, Charsets.UTF_8)
            temporary.delete()
        }
    }
}
