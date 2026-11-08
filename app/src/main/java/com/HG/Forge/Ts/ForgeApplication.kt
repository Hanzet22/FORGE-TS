package com.HG.Forge.Ts

import android.app.Application
import android.content.Context
import com.HG.Forge.Ts.data.sync.CrashReportStore
import com.HG.Forge.Ts.data.sync.ForgeDiagnostics

class ForgeApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        installEarlyCrashHandler()
        ForgeDiagnostics.log(this, "INFO", "Application", "application.attach_base_context")
    }

    override fun onCreate() {
        super.onCreate()
        // Keep Application startup minimal. Diagnostic data is stored locally;
        // recovery and sharing must not depend on WorkManager or network services.
        ForgeDiagnostics.log(this, "INFO", "Application", "application.on_create")
    }

    private fun installEarlyCrashHandler() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Persist locally before delegating to Android's normal crash handler.
            runCatching { CrashReportStore.capture(this, throwable) }
            ForgeDiagnostics.log(this, "ERROR", "UncaughtException", "process.uncaught_exception", throwable)
            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(10)
            }
        }
    }
}
