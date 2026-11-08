package com.HG.Forge.Ts

import android.app.Application
import android.content.Context
import com.HG.Forge.Ts.data.sync.CrashReportStore
import com.HG.Forge.Ts.data.sync.CrashReportUploadScheduler

class ForgeApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        installEarlyCrashHandler()
    }

    override fun onCreate() {
        super.onCreate()
        // Scheduling must never prevent the app from reaching its first Activity.
        runCatching { CrashReportUploadScheduler.enqueue(this) }
    }

    private fun installEarlyCrashHandler() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Always keep one sanitized report locally so startup crashes can be
            // diagnosed even when the user cannot reach Settings. Network upload
            // remains separately gated by the explicit sharing preference.
            runCatching { CrashReportStore.capture(this, throwable) }
            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(10)
            }
        }
    }
}
