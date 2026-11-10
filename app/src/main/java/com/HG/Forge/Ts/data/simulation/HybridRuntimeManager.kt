package com.HG.Forge.Ts.data.simulation

import android.app.ActivityManager
import android.content.Context
import android.os.StatFs
import java.io.File

enum class RuntimeEnvironmentId { XFCE_NATIVE, XFCE_PROOT, MIKROTIK_ROUTEROS, LINUX_NATIVE, VM_WORKSTATION }
enum class RuntimeLifecycleState { NOT_INSTALLED, READY, STARTING, RUNNING, STOPPING, STOPPED, FAILED, UNAVAILABLE }

data class RuntimeEnvironmentSnapshot(
    val id: RuntimeEnvironmentId,
    val title: String,
    val lifecycle: RuntimeLifecycleState,
    val workspacePath: String,
    val workspaceReady: Boolean,
    val notes: List<String>
)
data class RuntimeResourceSnapshot(
    val availableMemoryBytes: Long,
    val totalMemoryBytes: Long,
    val availableStorageBytes: Long,
    val totalStorageBytes: Long,
    val availableProcessors: Int
)
data class HybridRuntimeSnapshot(
    val environments: List<RuntimeEnvironmentSnapshot>,
    val resources: RuntimeResourceSnapshot,
    val managerWorkspaceReady: Boolean,
    val notes: List<String>
)
data class RuntimeOperationResult(val accepted: Boolean, val message: String)

/**
 * Phase 2 manager foundation: registry, isolated private workspaces, lifecycle
 * model, and device resource observations. No environment process is launched.
 */
class HybridRuntimeManager(context: Context) {
    private val appContext = context.applicationContext
    private val runtimeRoot = File(appContext.filesDir, "forge-runtime")
    private val managerRoot = File(runtimeRoot, "manager")
    private val roots = mapOf(
        RuntimeEnvironmentId.XFCE_NATIVE to File(runtimeRoot, "native-xfce"),
        RuntimeEnvironmentId.XFCE_PROOT to File(runtimeRoot, "proot"),
        RuntimeEnvironmentId.MIKROTIK_ROUTEROS to File(runtimeRoot, "routeros"),
        RuntimeEnvironmentId.LINUX_NATIVE to File(runtimeRoot, "linux-native"),
        RuntimeEnvironmentId.VM_WORKSTATION to File(runtimeRoot, "vm-workstation")
    )
    private val titles = mapOf(
        RuntimeEnvironmentId.XFCE_NATIVE to "XFCE4 Desktop Native",
        RuntimeEnvironmentId.XFCE_PROOT to "XFCE4 PRoot",
        RuntimeEnvironmentId.MIKROTIK_ROUTEROS to "MikroTik RouterOS",
        RuntimeEnvironmentId.LINUX_NATIVE to "Linux Ubuntu/Debian Desktop Native",
        RuntimeEnvironmentId.VM_WORKSTATION to "VM WStation"
    )
    private val states = RuntimeEnvironmentId.entries.associateWith {
        if (it == RuntimeEnvironmentId.MIKROTIK_ROUTEROS) RuntimeLifecycleState.UNAVAILABLE
        else RuntimeLifecycleState.NOT_INSTALLED
    }.toMutableMap()
    private val prepared = mutableSetOf<RuntimeEnvironmentId>()

    @Synchronized
    fun inspect(): HybridRuntimeSnapshot {
        val managerReady = listOf("registry", "state", "logs").all {
            isSafeDirectory(File(managerRoot, it), managerRoot)
        }
        val memory = ActivityManager.MemoryInfo()
        val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memory)
        val storage = StatFs(appContext.filesDir.absolutePath)
        val environments = RuntimeEnvironmentId.entries.map { id ->
            val root = roots.getValue(id)
            val ready = id in prepared && isSafeDirectory(root, runtimeRoot)
            val notes = mutableListOf<String>()
            if (!ready) notes += "Private workspace has not been prepared."
            if (states.getValue(id) == RuntimeLifecycleState.UNAVAILABLE) {
                notes += "Runtime is gated; workspace preparation does not enable execution."
            } else if (states.getValue(id) == RuntimeLifecycleState.NOT_INSTALLED) {
                notes += "Runtime payload and execution adapter are not installed."
            }
            RuntimeEnvironmentSnapshot(id, titles.getValue(id), states.getValue(id), root.canonicalPath, ready, notes)
        }
        return HybridRuntimeSnapshot(
            environments,
            RuntimeResourceSnapshot(
                memory.availMem, memory.totalMem, storage.availableBytes, storage.totalBytes,
                Runtime.getRuntime().availableProcessors()
            ),
            managerReady,
            listOf(
                "Resource readings are device-wide, not reserved for this app.",
                "Lifecycle registry is in-memory and resets when the app process restarts.",
                "Inspection does not start PRoot, desktop services, guest VMs, or other processes."
            )
        )
    }

    @Synchronized
    fun prepareWorkspace(id: RuntimeEnvironmentId): RuntimeOperationResult = try {
        val runtimeReady = createSafeDirectory(runtimeRoot, appContext.filesDir)
        val managerReady = runtimeReady && listOf("registry", "state", "logs").all {
            createSafeDirectory(File(managerRoot, it), runtimeRoot)
        }
        val root = roots.getValue(id)
        val rootReady = managerReady && createSafeDirectory(root, runtimeRoot)
        val childrenReady = rootReady && listOf("home", "tmp", "logs", "data").all {
            createSafeDirectory(File(root, it), root)
        }
        if (!childrenReady) {
            RuntimeOperationResult(false, "Workspace preparation failed containment or filesystem checks.")
        } else {
            prepared += id
            RuntimeOperationResult(true, "Private workspace prepared. Runtime remains ${states.getValue(id)}; no process was launched.")
        }
    } catch (error: Exception) {
        RuntimeOperationResult(false, error.message ?: "Workspace preparation failed.")
    }

    @Synchronized
    fun requestStart(id: RuntimeEnvironmentId): RuntimeOperationResult {
        val state = states.getValue(id)
        if (state == RuntimeLifecycleState.UNAVAILABLE) {
            return RuntimeOperationResult(false, "This environment is gated and cannot be started.")
        }
        return RuntimeOperationResult(false, "No execution adapter is implemented. Workspace readiness is not runtime readiness (state: $state).")
    }

    @Synchronized
    fun transitionForFutureAdapter(id: RuntimeEnvironmentId, next: RuntimeLifecycleState): RuntimeOperationResult {
        val current = states.getValue(id)
        val allowed = when (current) {
            RuntimeLifecycleState.NOT_INSTALLED -> setOf(RuntimeLifecycleState.READY, RuntimeLifecycleState.FAILED)
            RuntimeLifecycleState.READY -> setOf(RuntimeLifecycleState.STARTING, RuntimeLifecycleState.UNAVAILABLE, RuntimeLifecycleState.FAILED)
            RuntimeLifecycleState.STARTING -> setOf(RuntimeLifecycleState.RUNNING, RuntimeLifecycleState.FAILED, RuntimeLifecycleState.STOPPED)
            RuntimeLifecycleState.RUNNING -> setOf(RuntimeLifecycleState.STOPPING, RuntimeLifecycleState.FAILED)
            RuntimeLifecycleState.STOPPING -> setOf(RuntimeLifecycleState.STOPPED, RuntimeLifecycleState.FAILED)
            RuntimeLifecycleState.STOPPED -> setOf(RuntimeLifecycleState.STARTING, RuntimeLifecycleState.READY, RuntimeLifecycleState.FAILED)
            RuntimeLifecycleState.FAILED -> setOf(RuntimeLifecycleState.STOPPED, RuntimeLifecycleState.READY)
            RuntimeLifecycleState.UNAVAILABLE -> emptySet()
        }
        if (next !in allowed) return RuntimeOperationResult(false, "Rejected lifecycle transition: $current -> $next.")
        states[id] = next
        return RuntimeOperationResult(true, "Lifecycle state updated: $current -> $next.")
    }

    private fun createSafeDirectory(directory: File, parent: File): Boolean {
        val parentPath = parent.canonicalPath
        val appPath = appContext.filesDir.canonicalPath
        if (parentPath != appPath && !parentPath.startsWith(appPath + File.separator)) return false
        val created = directory.exists() || directory.mkdirs()
        val path = directory.canonicalPath
        return created && path.startsWith(parentPath + File.separator) &&
            directory.isDirectory && directory.canRead() && directory.canWrite()
    }

    private fun isSafeDirectory(directory: File, parent: File): Boolean = runCatching {
        directory.exists() &&
            directory.canonicalPath.startsWith(parent.canonicalPath + File.separator) &&
            directory.isDirectory && directory.canRead() && directory.canWrite()
    }.getOrDefault(false)
}
