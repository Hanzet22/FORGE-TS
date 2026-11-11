package com.HG.Forge.Ts.data.sync

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant

/** Creates a deliberately minimized report for a human-reviewed email handoff. */
object DiagnosticEmailExporter {
    const val RECIPIENT = "papakjamsutnoruu@gmail.com"

    fun createShareIntent(context: Context): Intent {
        val report = JSONObject()
            .put("reportType", "FORGE-TS diagnostic summary")
            .put("generatedAt", Instant.now().toString())
            .put("appVersion", runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
            }.getOrDefault("unknown"))
            .put("androidSdk", Build.VERSION.SDK_INT)
            .put("androidRelease", Build.VERSION.RELEASE.orEmpty().take(32))
            .put("privacyNote", "No installation identifier, account identifier, stack trace, command output, password, message content or network payload is included.")
            .put("diagnosticEvents", minimizedEvents(context))
            .put("runtimeTransitions", runtimeTransitions(context))

        val directory = File(context.cacheDir, "shared-diagnostics")
        check((directory.isDirectory || directory.mkdirs()) && directory.canonicalFile.toPath().startsWith(context.cacheDir.canonicalFile.toPath())) {
            "Unable to prepare the private diagnostic export directory."
        }
        val output = File(directory, "forge-ts-diagnostics.json")
        output.writeText(report.toString(2), Charsets.UTF_8)
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            output
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(RECIPIENT))
            putExtra(Intent.EXTRA_SUBJECT, "FORGE-TS diagnostic report")
            putExtra(Intent.EXTRA_TEXT, "Attached is a minimized FORGE-TS diagnostic summary. Please review the attachment before sending.")
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, "FORGE-TS diagnostics", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun minimizedEvents(context: Context): JSONArray {
        val source = ForgeDiagnostics.read(context)
        val output = JSONArray()
        val start = (source.length() - 100).coerceAtLeast(0)
        for (index in start until source.length()) {
            val item = source.optJSONObject(index) ?: continue
            output.put(JSONObject()
                .put("occurredAt", item.optString("occurredAt"))
                .put("appVersion", item.optString("appVersion"))
                .put("androidSdk", item.optInt("androidSdk"))
                .put("level", item.optString("level"))
                .put("tag", item.optString("tag"))
                .put("eventName", item.optString("eventName"))
                .put("exceptionType", item.optString("exceptionType")))
        }
        return output
    }

    private fun runtimeTransitions(context: Context): JSONArray {
        val file = File(context.filesDir, "forge-runtime/manager/logs/runtime-events.log")
        if (!file.isFile || file.length() > 64 * 1024) return JSONArray()
        val output = JSONArray()
        file.readLines(Charsets.UTF_8).takeLast(100).forEach { line ->
            val parts = line.split('|', limit = 4)
            if (parts.size == 4 && parts[0].toLongOrNull() != null) {
                output.put(JSONObject()
                    .put("timestampEpochMs", parts[0].toLong())
                    .put("category", parts[1].take(24))
                    .put("subject", parts[2].take(48))
                    .put("state", parts[3].take(24)))
            }
        }
        return output
    }
}
