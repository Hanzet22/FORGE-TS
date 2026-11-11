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
    private val interactiveOutput = ConcurrentHashMap<String, StringBuilder>()
    private val interactiveInputLocks = ConcurrentHashMap<String, Any>()
    override val environmentId = RuntimeEnvironmentId.XFCE_PROOT

    override suspend fun inspect(request: RuntimeAdapterStartRequest): RuntimeAdapterCapability =
        withContext(Dispatchers.IO) {
            val paths = resolvePaths(request)
                ?: return@withContext RuntimeAdapterCapability(false,
                    "Missing native PRoot engine/loader or verified rootfs; no process was launched.")
            val (root, engine, loader, logs, tmp) = paths
            val guestShell = runCatching { File(root, "bin/sh").canonicalFile }.getOrNull()
            if (guestShell == null || !guestShell.path.startsWith(root.canonicalPath + File.separator) ||
                !guestShell.isFile || !guestShell.canRead()) {
                return@withContext RuntimeAdapterCapability(false, "Rootfs /bin/sh is missing, unreadable, or escapes the selected rootfs.")
            }
            val probeLog = File(logs, "ptrace-probe.log")
            val process = runCatching {
                ProcessBuilder(command(engine, root, listOf("/bin/sh", "-c", "exit 0")))
                    .directory(root).redirectErrorStream(true).redirectOutput(probeLog)
                    .apply { environment().putAll(environment(loader, tmp)) }.start()
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
            val (root, engine, loader, logs, tmp) = paths
            val key = request.workspacePath
            if (sessions[key]?.isAlive == true) {
                return@withContext RuntimeAdapterStartResult(false, true, "A PRoot process is already active for this workspace.")
            }
            val process = runCatching {
                ProcessBuilder(command(engine, root, listOf("/bin/sh", "-l")))
                    .directory(root).redirectErrorStream(true)
                    .apply { environment().putAll(environment(loader, tmp)) }.start()
            }.getOrElse {
                return@withContext RuntimeAdapterStartResult(false, false,
                    "PRoot session launch failed: " + (it.message ?: "unknown error"))
            }
            sessions[key] = process
            interactiveOutput[key] = StringBuilder()
            interactiveInputLocks[key] = Any()
            Thread({
                val logFile = File(logs, "session.log")
                val buffer = ByteArray(2048)
                try {
                    process.inputStream.use { stream ->
                        logFile.outputStream().buffered().use { log ->
                            while (true) {
                                val count = stream.read(buffer)
                                if (count < 0) break
                                val chunk = String(buffer, 0, count, Charsets.UTF_8)
                                synchronized(interactiveOutput[key] ?: continue) {
                                    val output = interactiveOutput[key] ?: continue
                                    output.append(chunk)
                                    if (output.length > 262144) output.delete(0, output.length - 262144)
                                }
                                log.write(buffer, 0, count)
                                log.flush()
                            }
                        }
                    }
                } catch (_: Exception) { }
            }, "forge-proot-output-${key.hashCode()}").apply { isDaemon = true; start() }
            Thread.sleep(150)
            if (!process.isAlive) {
                sessions.remove(key, process)
                return@withContext RuntimeAdapterStartResult(false, false, "PRoot shell exited during startup; inspect session.log.")
            }
            RuntimeAdapterStartResult(true, true,
                "PRoot shell process is active. Interactive terminal/display integration remains a separate milestone.")
        }

    override suspend fun execute(
        request: RuntimeAdapterStartRequest,
        arguments: List<String>,
        timeoutSeconds: Long
    ): RuntimeCommandResult = withContext(Dispatchers.IO) {
        if (arguments.isEmpty() || arguments.size > 64 ||
            arguments.any { it.isEmpty() || it.length > 4096 || it.indexOf('\u0000') >= 0 } ||
            timeoutSeconds !in 1L..120L) {
            return@withContext RuntimeCommandResult(false, null, "", "Command arguments or timeout are outside the supported bounds.")
        }
        val paths = resolvePaths(request)
            ?: return@withContext RuntimeCommandResult(false, null, "", "PRoot engine, loader, or selected rootfs is unavailable.")
        val (root, engine, loader, logs, tmp) = paths
        val outputFile = File(logs, "command-${System.currentTimeMillis()}.log")
        val process = runCatching {
            ProcessBuilder(command(engine, root, arguments))
                .directory(root)
                .redirectErrorStream(true)
                .redirectOutput(outputFile)
                .apply { environment().putAll(environment(loader, tmp)) }
                .start()
        }.getOrElse {
            return@withContext RuntimeCommandResult(false, null, "", "Guest command launch failed: ${it.message ?: "unknown error"}")
        }
        val completed = runCatching { process.waitFor(timeoutSeconds, TimeUnit.SECONDS) }.getOrDefault(false)
        if (!completed) {
            process.destroy()
            if (!runCatching { process.waitFor(500, TimeUnit.MILLISECONDS) }.getOrDefault(false)) process.destroyForcibly()
            val partial = runCatching { outputFile.takeIf { it.isFile }?.readText()?.take(32768) ?: "" }.getOrDefault("")
            return@withContext RuntimeCommandResult(false, null, partial, "Guest command timed out after $timeoutSeconds seconds.")
        }
        val output = runCatching { outputFile.takeIf { it.isFile }?.readText()?.take(32768) ?: "" }.getOrDefault("")
        val code = process.exitValue()
        RuntimeCommandResult(true, code, output, if (code == 0) "Guest command completed." else "Guest command exited with code $code.")
    }

    override suspend fun writeInteractive(request: RuntimeAdapterStartRequest, input: String): RuntimeInteractiveResult =
        withContext(Dispatchers.IO) {
            if (input.isEmpty() || input.length > 16384 || input.indexOf('\\u0000') >= 0) {
                return@withContext RuntimeInteractiveResult(false, message = "Input is empty or exceeds the 16 KiB session limit.")
            }
            val key = request.workspacePath
            val process = sessions[key]
                ?: return@withContext RuntimeInteractiveResult(false, message = "No active PRoot session exists.")
            if (!process.isAlive) return@withContext RuntimeInteractiveResult(false, message = "PRoot session is no longer running.")
            val lock = interactiveInputLocks[key] ?: return@withContext RuntimeInteractiveResult(false, message = "Session input channel is unavailable.")
            try {
                synchronized(lock) {
                    process.outputStream.write(input.toByteArray(Charsets.UTF_8))
                    process.outputStream.flush()
                }
                RuntimeInteractiveResult(true, message = "Input delivered to the active guest shell.")
            } catch (error: Exception) {
                RuntimeInteractiveResult(false, message = "Could not write to the guest shell: ${error.message ?: "I/O error"}")
            }
        }

    override suspend fun readInteractive(request: RuntimeAdapterStartRequest): RuntimeInteractiveResult =
        withContext(Dispatchers.IO) {
            val key = request.workspacePath
            val process = sessions[key]
                ?: return@withContext RuntimeInteractiveResult(false, message = "No active PRoot session exists.")
            val output = interactiveOutput[key]
                ?: return@withContext RuntimeInteractiveResult(false, message = "Session output channel is unavailable.")
            val chunk = synchronized(output) {
                val value = output.toString()
                output.setLength(0)
                value
            }
            RuntimeInteractiveResult(process.isAlive, chunk, if (process.isAlive) "Session output read." else "Session has exited; remaining output returned.")
        }

    override suspend fun stop(request: RuntimeAdapterStartRequest): RuntimeAdapterStopResult =
        withContext(Dispatchers.IO) {
            val process = sessions.remove(request.workspacePath)
                ?: return@withContext RuntimeAdapterStopResult(false, "No adapter-owned PRoot process exists for this workspace.")
            process.destroy()
            val graceful = runCatching { process.waitFor(3, TimeUnit.SECONDS) }.getOrDefault(false)
            if (!graceful) process.destroyForcibly()
            val terminated = graceful || runCatching { process.waitFor(2, TimeUnit.SECONDS) }.getOrDefault(false)
            interactiveOutput.remove(request.workspacePath)
            interactiveInputLocks.remove(request.workspacePath)
            RuntimeAdapterStopResult(terminated,
                if (graceful) "PRoot launcher stopped."
                else if (terminated) "PRoot launcher was force-stopped after the graceful timeout."
                else "PRoot launcher termination could not be confirmed; manual recovery is required.")
        }

    private data class Paths(
        val root: File,
        val engine: File,
        val loader: File,
        val logs: File,
        val tmp: File
    )

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
        val tmp = File(workspace, "tmp").canonicalFile
        if (!tmp.path.startsWith(workspace.path + File.separator) ||
            !(tmp.isDirectory || tmp.mkdirs()) || !tmp.canWrite()) return null
        return Paths(root, engine, loader, logs, tmp)
    }

    private fun command(engine: File, root: File, guest: List<String>): List<String> =
        listOf(engine.absolutePath, "--kill-on-exit", "-0", "-r", root.absolutePath, "-w", "/") + guest

    private fun environment(loader: File, tmp: File): Map<String, String> = mapOf(
        "PROOT_LOADER" to loader.absolutePath,
        "PROOT_TMP_DIR" to tmp.absolutePath,
        "TMPDIR" to tmp.absolutePath,
        "LD_LIBRARY_PATH" to appContext.applicationInfo.nativeLibraryDir
    )
}
