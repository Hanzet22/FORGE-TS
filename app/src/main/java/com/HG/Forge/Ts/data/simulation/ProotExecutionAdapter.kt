package com.HG.Forge.Ts.data.simulation

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * PRoot command execution uses ProcessBuilder for bounded one-shot commands.
 * Long-lived interactive sessions use the native Bionic PTY bridge so the
 * guest receives a real terminal (controlling TTY, terminal input and resize).
 */
class ProotExecutionAdapter(context: Context) : RuntimeExecutionAdapter {
    private data class PtySession(
        val handle: Long,
        val output: StringBuilder,
        val inputLock: Any,
        @Volatile var reader: Thread? = null
    )

    private val appContext = context.applicationContext
    private val sessions = ConcurrentHashMap<String, PtySession>()
    override val environmentId = RuntimeEnvironmentId.XFCE_PROOT

    override suspend fun inspect(request: RuntimeAdapterStartRequest): RuntimeAdapterCapability =
        withContext(Dispatchers.IO) {
            if (!NativePtyEngine.available) {
                return@withContext RuntimeAdapterCapability(false,
                    "Native PTY library is unavailable; interactive PRoot cannot start.")
            }
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
            RuntimeAdapterCapability(true, "PRoot engine, loader, rootfs shell, ptrace probe and native PTY library are available.")
        }

    override suspend fun start(request: RuntimeAdapterStartRequest): RuntimeAdapterStartResult =
        withContext(Dispatchers.IO) {
            val capability = inspect(request)
            if (!capability.available) return@withContext RuntimeAdapterStartResult(false, false, capability.detail)
            val paths = resolvePaths(request)
                ?: return@withContext RuntimeAdapterStartResult(false, false, "PRoot paths changed after inspection.")
            val (root, engine, loader, logs, tmp) = paths
            val key = request.workspacePath
            val current = sessions[key]
            if (current != null && NativePtyEngine.nativeWait(current.handle) == 0) {
                return@withContext RuntimeAdapterStartResult(false, true, "A native PTY PRoot session is already active for this workspace.")
            }
            if (current != null) {
                NativePtyEngine.nativeClose(current.handle, true)
                sessions.remove(key, current)
            }

            val argv = command(engine, root, listOf("/bin/sh", "-l")).toTypedArray()
            val envp = environment(loader, tmp).map { (name, value) -> "$name=$value" }.toTypedArray()
            val handle = NativePtyEngine.nativeStart(argv, envp, root.absolutePath, 24, 80)
            if (handle <= 0L) {
                return@withContext RuntimeAdapterStartResult(false, false,
                    "Native PTY could not launch PRoot (errno ${-handle}).")
            }

            val session = PtySession(handle, StringBuilder(), Any())
            sessions[key] = session
            val logFile = File(logs, "session.log")
            session.reader = Thread({
                val bytes = ByteArray(8192)
                try {
                    logFile.outputStream().buffered().use { log ->
                        while (true) {
                            val count = NativePtyEngine.nativeRead(handle, bytes, 250)
                            if (count > 0) {
                                log.write(bytes, 0, count)
                                log.flush()
                                val chunk = String(bytes, 0, count, Charsets.UTF_8)
                                synchronized(session.output) {
                                    session.output.append(chunk)
                                    if (session.output.length > 262144) {
                                        session.output.delete(0, session.output.length - 262144)
                                    }
                                }
                            } else if (count < 0 || NativePtyEngine.nativeWait(handle) != 0) {
                                break
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Session shutdown closes the PTY master and ends this reader.
                }
            }, "forge-proot-pty-${key.hashCode()}").apply {
                isDaemon = true
                start()
            }

            Thread.sleep(150)
            val status = NativePtyEngine.nativeWait(handle)
            if (status != 0) {
                NativePtyEngine.nativeClose(handle, true)
                sessions.remove(key, session)
                return@withContext RuntimeAdapterStartResult(false, false,
                    "PRoot shell exited during PTY startup (status=$status); inspect session.log.")
            }
            RuntimeAdapterStartResult(true, true,
                "PRoot is running in a native PTY session (24x80). Terminal job control and resize are now backed by the PTY.")
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
            if (input.isEmpty() || input.length > 16384 || input.indexOf('\u0000') >= 0) {
                return@withContext RuntimeInteractiveResult(false, message = "Input is empty or exceeds the 16 KiB session limit.")
            }
            val session = sessions[request.workspacePath]
                ?: return@withContext RuntimeInteractiveResult(false, message = "No active native PTY session exists.")
            if (NativePtyEngine.nativeWait(session.handle) != 0) {
                return@withContext RuntimeInteractiveResult(false, message = "PRoot PTY session has exited.")
            }
            val bytes = input.toByteArray(Charsets.UTF_8)
            try {
                synchronized(session.inputLock) {
                    var offset = 0
                    while (offset < bytes.size) {
                        val written = NativePtyEngine.nativeWrite(session.handle, bytes, offset, bytes.size - offset)
                        if (written <= 0) {
                            return@withContext RuntimeInteractiveResult(false,
                                message = "PTY input stopped after $offset of ${bytes.size} bytes.")
                        }
                        offset += written
                    }
                }
                RuntimeInteractiveResult(true, message = "Input delivered through the native PTY master.")
            } catch (error: Exception) {
                RuntimeInteractiveResult(false, message = "Could not write to the guest PTY: ${error.message ?: "I/O error"}")
            }
        }

    override suspend fun readInteractive(request: RuntimeAdapterStartRequest): RuntimeInteractiveResult =
        withContext(Dispatchers.IO) {
            val session = sessions[request.workspacePath]
                ?: return@withContext RuntimeInteractiveResult(false, message = "No active native PTY session exists.")
            val chunk = synchronized(session.output) {
                val value = session.output.toString()
                session.output.setLength(0)
                value
            }
            val running = NativePtyEngine.nativeWait(session.handle) == 0
            RuntimeInteractiveResult(running, chunk,
                if (running) "Native PTY output read." else "PRoot PTY session exited; buffered output returned.")
        }

    override suspend fun resizeInteractive(
        request: RuntimeAdapterStartRequest,
        rows: Int,
        columns: Int
    ): RuntimeInteractiveResult = withContext(Dispatchers.IO) {
        if (rows !in 1..1000 || columns !in 1..1000) {
            return@withContext RuntimeInteractiveResult(false, message = "Terminal dimensions must be between 1 and 1000.")
        }
        val session = sessions[request.workspacePath]
            ?: return@withContext RuntimeInteractiveResult(false, message = "No active native PTY session exists.")
        if (NativePtyEngine.nativeWait(session.handle) != 0) {
            return@withContext RuntimeInteractiveResult(false, message = "PRoot PTY session has exited.")
        }
        val resized = NativePtyEngine.nativeResize(session.handle, rows, columns)
        RuntimeInteractiveResult(resized,
            message = if (resized) "PTY window size updated to ${rows}x${columns}."
            else "Native PTY rejected the window-size update.")
    }

    override suspend fun stop(request: RuntimeAdapterStartRequest): RuntimeAdapterStopResult =
        withContext(Dispatchers.IO) {
            val session = sessions.remove(request.workspacePath)
                ?: return@withContext RuntimeAdapterStopResult(false, "No adapter-owned native PTY session exists for this workspace.")
            val closed = NativePtyEngine.nativeClose(session.handle, true)
            runCatching { session.reader?.join(750) }
            RuntimeAdapterStopResult(closed,
                if (closed) "PRoot PTY session received SIGHUP, was reaped, and its PTY master was closed."
                else "Native PTY session could not be closed; manual recovery is required.")
        }

    private data class Paths(
        val root: File,
        val engine: File,
        val loader: File,
        val logs: File,
        val tmp: File
    )

    private fun resolvePaths(request: RuntimeAdapterStartRequest): Paths? {
        if (request.environmentId != environmentId) return null
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
