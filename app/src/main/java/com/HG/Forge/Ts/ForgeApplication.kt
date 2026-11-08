package com.HG.Forge.Ts

import android.app.Application
import com.HG.Forge.Ts.data.sync.CrashReportConsent
import com.HG.Forge.Ts.data.sync.CrashReportStore
import com.HG.Forge.Ts.data.sync.CrashReportUploadScheduler

class ForgeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (CrashReportConsent.isEnabled(this)) CrashReportStore.capture(this, throwable)
            previousHandler?.uncaughtException(thread, throwable)
        }
        CrashReportUploadScheduler.enqueue(this)
    }
}
