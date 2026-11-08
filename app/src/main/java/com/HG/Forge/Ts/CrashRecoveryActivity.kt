package com.HG.Forge.Ts

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.FileProvider
import com.HG.Forge.Ts.data.sync.CrashReportStore
import com.HG.Forge.Ts.data.sync.ForgeDiagnostics
import com.HG.Forge.Ts.data.sync.CrashReportUploadScheduler
import com.HG.Forge.Ts.data.sync.ForgeDiagnosticsUploadScheduler
import java.io.File

/**
 * Deliberately uses platform Views only: this screen must not depend on Compose,
 * the intro player, networking, or the main navigation graph.
 */
class CrashRecoveryActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Persist in the crash handler; schedule network work on the next safe launch.
        runCatching { CrashReportUploadScheduler.enqueue(this) }
        runCatching { ForgeDiagnosticsUploadScheduler.enqueue(this) }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 36, 28, 28)
        }
        val title = TextView(this).apply {
            text = "FORGE-TS recovered from a previous crash"
            textSize = 22f
        }
        val details = TextView(this).apply {
            text = "A diagnostic report was saved locally. Share it so the startup problem can be investigated. The report contains technical event labels, app/device versions and stack frames; review it before sending."
            textSize = 15f
            setPadding(0, 18, 0, 24)
        }
        val share = Button(this).apply {
            text = "Share diagnostic report"
            setOnClickListener { shareReport() }
        }
        val continueButton = Button(this).apply {
            text = "Continue to FORGE-TS"
            setOnClickListener {
                CrashReportStore.markRecoveryAcknowledged(this@CrashRecoveryActivity)
                startActivity(Intent(this@CrashRecoveryActivity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                finish()
            }
        }
        val close = Button(this).apply {
            text = "Close"
            setOnClickListener { finish() }
        }
        root.addView(title)
        root.addView(details)
        root.addView(share)
        root.addView(continueButton)
        root.addView(close)
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun shareReport() {
        val crash = CrashReportStore.read(this)
        val events = ForgeDiagnostics.read(this)
        val report = buildString {
            appendLine("FORGE-TS diagnostic report")
            appendLine("Generated: ${System.currentTimeMillis()}")
            appendLine()
            appendLine("=== CRASH REPORT ===")
            appendLine(crash?.toString(2) ?: "No crash report available.")
            appendLine()
            appendLine("=== RECENT DIAGNOSTIC EVENTS ===")
            appendLine(events.toString(2))
        }

        runCatching {
            val directory = File(cacheDir, "shared-diagnostics").apply { mkdirs() }
            val file = File(directory, "forge-ts-diagnostics.txt")
            file.writeText(report, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "FORGE-TS diagnostic report")
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(contentResolver, "FORGE-TS diagnostics", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(send, "Share FORGE-TS diagnostics"))
        }.onFailure {
            android.widget.Toast.makeText(this, "Could not prepare diagnostic report.", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
