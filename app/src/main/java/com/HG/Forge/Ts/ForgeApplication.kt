package com.HG.Forge.Ts

import android.app.Application
import android.content.Context
import com.HG.Forge.Ts.data.sync.CrashReportStore
import com.HG.Forge.Ts.data.sync.ForgeDiagnostics
import com.HG.Forge.Ts.data.sync.ForgeDiagnosticsConsent
import com.HG.Forge.Ts.data.sync.ForgeDiagnosticsUploadScheduler
import com.HG.Forge.Ts.data.sync.CrashReportUploadScheduler
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

class ForgeApplication : Application(), DefaultLifecycleObserver {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        installEarlyCrashHandler()
        ForgeDiagnostics.log(this, "INFO", "Application", "application.attach_base_context")
    }

    override fun onCreate() {
        super.onCreate()
        // Observe the whole process, not individual Activity recreation.
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        ForgeDiagnostics.log(this, "INFO", "Application", "application.on_create")
        if (ForgeDiagnosticsConsent.isEnabled(this)) {
            ForgeDiagnosticsUploadScheduler.enqueue(this)
            CrashReportUploadScheduler.enqueue(this)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        ForgeDiagnostics.log(this, "INFO", "Application", "session.foreground")
    }

    override fun onStop(owner: LifecycleOwner) {
        // WorkManager persists this request and waits for connectivity after backgrounding.
        ForgeDiagnostics.log(this, "INFO", "Application", "session.background")
        if (ForgeDiagnosticsConsent.isEnabled(this)) {
            ForgeDiagnosticsUploadScheduler.enqueue(this)
            CrashReportUploadScheduler.enqueue(this)
        }
    }

    private fun installEarlyCrashHandler() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Persist locally before delegating to Android's normal crash handler.
            runCatching { CrashReportStore.capture(this, throwable) }
            ForgeDiagnostics.log(this, "CRASH", "UncaughtException", "process.uncaught_exception", throwable)
            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(10)
            }
        }
    }
}
