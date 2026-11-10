package com.HG.Forge.Ts.data.simulation

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * PRoot remains unavailable until the patched native engine, loader and a
 * distro rootfs are present. A prepared workspace is not an installed runtime.
 */
class ProotExecutionAdapter(context: Context) : RuntimeExecutionAdapter {
    private val appContext = context.applicationContext
    private val sessions = ConcurrentHashMap<String, Process>()
    override val environmentId = RuntimeEnvironmentId.XFCE_PROOT

    override suspend fun inspect(request: RuntimeAdapterStartRequest): RuntimeAdapterCapability =
        withContext(Dispatchers.IO) {
            val paths = resolvePaths(request)
                ?: return@withContext RuntimeAdapterCapability(false,
                    "Missing native PRoot engine/loader or verified rootfs; no process was launched.")
            val (root, engine, loader, logs) = paths
            if (!File(root, "bin/sh").let { it.isFile && it.canRead() }) {
                return@withContext RuntimeAdapterCapability(false, "Rootfs /bin/sh is missing or unreadable.")
            }
            val probeLog = File(logs, "ptrace-probe.log")
            val process = runCatching {
                ProcessBuilder(command(engine, root, listOf("/bin/sh", "-c", "exit 0")))
                    .directory(root).redirectErrorStream(true).redirectOutput(probeLog)
                    .apply { environment().putAll(environment(loader)) }.start()
            }.getOrElse {
                return@withContext RuntimeAdapterCapability(false,
                    "PRoot disposable-child probe could not launch: " + (it.message ?: "unknown error"))
            }
            val completed = runCatching { process.waitFor(8, TimeUnit.SECONDS) }.getOrDefault(false)
            if (!completed) {
                process.destroy()
                if (!runCatching { process.waitFor(500, TimeUnit.MILLISECONDS) }.getOrDefault(false)) process.destroyForcibly()
                return@withContext RuntimeAdapterCapability(false, "PRoot disposable-child probe timed out; ptrace is not confirmed.")
            }
            if (process.exitValue() != 0) {
                return@withContext RuntimeAdapterCapability(false, "PRoot probe exited " + process.exitValue() + "; inspect ptrace-probe.log.")
            }
            RuntimeAdapterCapability(true, "PRoot engine, loader, rootfs shell and disposable child-process probe succeeded.")
        }

    override suspend fun start(request: RuntimeAdapterStartRequest): RuntimeAdapterStartResult =
        withContext(Dispatchers.IO) {
            val capability = inspect(request)
            if (!capability.available) return@withContext RuntimeAdapterStartResult(false, false, capability.detail)
            val paths = resolvePaths(request)
                ?: return@withContext RuntimeAdapterStartResult(false, false, "PRoot paths changed after inspection.")
            val (root, engine, loader, logs) = paths
            val key = request.workspacePath
            if (sessions[key]?.isAlive == true) {
                return@withContext RuntimeAdapterStartResult(false, true, "A PRoot process is already active for this workspace.")
            }
            val process = runCatching {
                ProcessBuilder(command(engine, root, listOf("/bin/sh", "-l")))
                    .directory(root).redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(File(logs, "session.log")))
                    .apply { environment().putAll(environment(loader)) }.start()
            }.getOrElse {
                return@withContext RuntimeAdapterStartResult(false, false,
                    "PRoot session launch failed: " + (it.message ?: "unknown error"))
            }
            sessions[key] = process
            Thread.sleep(150)
            if (!process.isAlive) {
                sessions.remove(key, process)
                return@withContext RuntimeAdapterStartResult(false, false, "PRoot shell exited during startup; inspect session.log.")
            }
            RuntimeAdapterStartResult(true, true,
                "PRoot shell process is active. Interactive terminal/display integration remains a separate milestone.")
        }

    override suspend fun stop(request: RuntimeAdapterStartRequest): RuntimeAdapterStopResult =
        withContext(Dispatchers.IO) {
            val process = sessions.remove(request.workspacePath)
                ?: return@withContext RuntimeAdapterStopResult(false, "No adapter-owned PRoot process exists for this workspace.")
            process.destroy()
            val graceful = runCatching { process.waitFor(3, TimeUnit.SECONDS) }.getOrDefault(false)
            if (!graceful) process.destroyForcibly()
            val terminated = graceful || runCatching { process.waitFor(2, TimeUnit.SECONDS) }.getOrDefault(false)
            RuntimeAdapterStopResult(terminated,
                if (graceful) "PRoot launcher stopped."
                else if (terminated) "PRoot launcher was force-stopped after the graceful timeout."
                else "PRoot launcher termination could not be confirmed; manual recovery is required.")
        }

    private data class Paths(val root: File, val engine: File, val loader: File, val logs: File)

    private fun resolvePaths(request: RuntimeAdapterStartRequest): Paths? {
        if (request.environmentId != RuntimeEnvironmentId.XFCE_PROOT) return null
        val appFiles = appContext.filesDir.canonicalFile
        val workspace = File(request.workspacePath).canonicalFile
        if (!workspace.path.startsWith(appFiles.path + File.separator) || !workspace.isDirectory) return null
        val distro = request.instanceId ?: return null
        if (distro !in setOf("debian", "ubuntu", "alpine")) return null
        if (workspace != File(appFiles, "forge-runtime/proot/" + distro).canonicalFile) return null
        val root = File(workspace, "rootfs").canonicalFile
        if (!root.path.startsWith(workspace.path + File.separator) || !root.isDirectory) return null
        val nativeDir = File(appContext.applicationInfo.nativeLibraryDir).canonicalFile
        val engine = File(nativeDir, "libproot.so")
        val loader = File(nativeDir, "libproot_loader.so")
        if (!engine.isFile || !engine.canExecute() || !loader.isFile || !loader.canRead()) return null
        val logs = File(workspace, "logs").canonicalFile
        if (!logs.path.startsWith(workspace.path + File.separator) ||
            !(logs.isDirectory || logs.mkdirs()) || !logs.canWrite()) return null
        return Paths(root, engine, loader, logs)
    }

    private fun command(engine: File, root: File, guest: List<String>): List<String> =
        listOf(engine.absolutePath, "--kill-on-exit", "-0", "-r", root.absolutePath, "-w", "/") + guest

    private fun environment(loader: File): Map<String, String> = mapOf(
        "PROOT_LOADER" to loader.absolutePath,
        "LD_LIBRARY_PATH" to appContext.applicationInfo.nativeLibraryDir
    )
}
