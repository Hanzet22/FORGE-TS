package com.HG.Forge.Ts.data.simulation

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Small app-private lifecycle journal. It records state transitions only:
 * no command output, credentials, user-entered text, or network payloads.
 */
internal class RuntimeDiagnosticLog(context: Context) {
    private val appContext = context.applicationContext
    private val logFile = File(
        appContext.filesDir,
        "forge-runtime/manager/logs/runtime-events.log"
    )

    suspend fun append(category: String, subject: String, state: String) =
        withContext(Dispatchers.IO) {
            runCatching {
                val filesRoot = appContext.filesDir.canonicalFile
                val parent = logFile.parentFile ?: return@runCatching
                if (!parent.exists() && !parent.mkdirs()) return@runCatching

                val parentPath = parent.canonicalPath
                if (parentPath != filesRoot.path &&
                    !parentPath.startsWith(filesRoot.path + File.separator)
                ) return@runCatching
                if (!parent.isDirectory || !parent.canWrite()) return@runCatching

                val targetPath = logFile.canonicalPath
                if (!targetPath.startsWith(parentPath + File.separator)) return@runCatching

                val safeCategory = category.filter { it.isLetterOrDigit() || it in "-_." }.take(24)
                val safeSubject = subject.filter { it.isLetterOrDigit() || it in "-_." }.take(48)
                val safeState = state.filter { it.isLetterOrDigit() || it in "-_." }.take(24)
                val line = "${System.currentTimeMillis()}|$safeCategory|$safeSubject|$safeState\n"
                synchronized(FILE_LOCK) {
                    if (logFile.isFile && logFile.length() + line.toByteArray(Charsets.UTF_8).size > MAX_BYTES) {
                        val retained = logFile.readText(Charsets.UTF_8).takeLast(RETAIN_BYTES)
                        logFile.writeText(retained, Charsets.UTF_8)
                    }
                    FileOutputStream(logFile, true).bufferedWriter(Charsets.UTF_8).use {
                        it.write(line)
                    }
                }
            }
        }

    private companion object {
        const val MAX_BYTES = 64 * 1024
        const val RETAIN_BYTES = 32 * 1024
        val FILE_LOCK = Any()
    }
}
