package com.HG.Forge.Ts.data.simulation

import android.app.ActivityManager
import android.content.Context
import android.os.StatFs
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

private val Context.runtimeRegistryDataStore by preferencesDataStore(name = "forge_runtime_registry")

enum class RuntimeEnvironmentId { XFCE_NATIVE, XFCE_PROOT, MIKROTIK_ROUTEROS, LINUX_NATIVE, VM_WORKSTATION }
enum class RuntimeLifecycleState { NOT_INSTALLED, READY, STARTING, RUNNING, STOPPING, STOPPED, FAILED, UNAVAILABLE, RECOVERY_REQUIRED }

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

private class RuntimeRegistryStore(private val context: Context) {
    data class Record(val state: RuntimeLifecycleState, val workspacePrepared: Boolean)

    suspend fun readAll(): Map<RuntimeEnvironmentId, Record> {
        val preferences = context.runtimeRegistryDataStore.data.first()
        return RuntimeEnvironmentId.entries.associateWith { id ->
            val state = preferences[stringPreferencesKey("${id.name}.state")]
                ?.let { runCatching { RuntimeLifecycleState.valueOf(it) }.getOrNull() }
                ?: if (id == RuntimeEnvironmentId.MIKROTIK_ROUTEROS) RuntimeLifecycleState.UNAVAILABLE
                   else RuntimeLifecycleState.NOT_INSTALLED
            Record(state, preferences[stringPreferencesKey("${id.name}.workspace_prepared")] == "true")
        }
    }

    suspend fun write(id: RuntimeEnvironmentId, state: RuntimeLifecycleState, prepared: Boolean) {
        context.runtimeRegistryDataStore.edit { preferences ->
            preferences[stringPreferencesKey("${id.name}.state")] = state.name
            preferences[stringPreferencesKey("${id.name}.workspace_prepared")] = prepared.toString()
            preferences[stringPreferencesKey("${id.name}.updated_at")] = System.currentTimeMillis().toString()
            preferences[stringPreferencesKey("manager.last_event")] =
                "${System.currentTimeMillis()}|${id.name}|${state.name}"
        }
    }

    suspend fun recordEvent(message: String) {
        context.runtimeRegistryDataStore.edit { preferences ->
            preferences[stringPreferencesKey("manager.last_event")] =
                "${System.currentTimeMillis()}|MANAGER|${message.take(240)}"
        }
    }

    suspend fun lastEvent(): String? =
        context.runtimeRegistryDataStore.data.first()[stringPreferencesKey("manager.last_event")]
}

class HybridRuntimeManager(context: Context) {
    private val appContext = context.applicationContext
    private val runtimeRoot = File(appContext.filesDir, "forge-runtime")
    private val managerRoot = File(runtimeRoot, "manager")
    private val registry = RuntimeRegistryStore(appContext)
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
    @Volatile private var initialized = false

    suspend fun initialize(): RuntimeOperationResult = withContext(Dispatchers.IO) {
        try {
            val runtimeReady = createSafeDirectory(runtimeRoot, appContext.filesDir)
            val managerReady = runtimeReady && listOf("registry", "state", "logs").all {
                createSafeDirectory(File(managerRoot, it), runtimeRoot)
            }
            if (!managerReady) return@withContext RuntimeOperationResult(false, "Manager storage failed containment or filesystem checks.")
            val records = registry.readAll()
            synchronized(this@HybridRuntimeManager) {
                RuntimeEnvironmentId.entries.forEach { id ->
                    val previous = records.getValue(id).state
                    states[id] = when {
                        id == RuntimeEnvironmentId.MIKROTIK_ROUTEROS -> RuntimeLifecycleState.UNAVAILABLE
                        previous == RuntimeLifecycleState.STARTING ||
                            previous == RuntimeLifecycleState.RUNNING ||
                            previous == RuntimeLifecycleState.STOPPING -> RuntimeLifecycleState.RECOVERY_REQUIRED
                        else -> previous
                    }
                    val root = roots.getValue(id)
                    if (records.getValue(id).workspacePrepared && isSafeDirectory(root, runtimeRoot)) prepared += id
                    else prepared -= id
                }
                initialized = true
            }
            RuntimeEnvironmentId.entries.forEach { id ->
                val recovered = states.getValue(id)
                val wasPrepared = records.getValue(id).workspacePrepared
                val isPrepared = id in prepared
                if (records.getValue(id).state != recovered || wasPrepared != isPrepared) {
                    registry.write(id, recovered, isPrepared)
                }
            }
            registry.recordEvent("Startup recovery inspection completed.")
            RuntimeOperationResult(true, "Persistent registry loaded. Previous active sessions require manual recovery; no runtime was restarted.")
        } catch (error: Exception) {
            RuntimeOperationResult(false, error.message ?: "Registry initialization failed.")
        }
    }

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
            if (!initialized) notes += "Persistent registry is still initializing."
            if (!ready) notes += "Private workspace has not been verified as prepared."
            when (states.getValue(id)) {
                RuntimeLifecycleState.UNAVAILABLE -> notes += "Runtime is gated; workspace preparation does not enable execution."
                RuntimeLifecycleState.NOT_INSTALLED -> notes += "Runtime payload and execution adapter are not installed."
                RuntimeLifecycleState.RECOVERY_REQUIRED -> notes += "Previous process was active; verify and recover manually. It was not restarted."
                else -> Unit
            }
            RuntimeEnvironmentSnapshot(id, titles.getValue(id), states.getValue(id), root.absolutePath, ready, notes)
        }
        return HybridRuntimeSnapshot(
            environments,
            RuntimeResourceSnapshot(memory.availMem, memory.totalMem, storage.availableBytes, storage.totalBytes,
                Runtime.getRuntime().availableProcessors()),
            managerReady,
            listOf(
                "Registry metadata is persisted locally with DataStore; runtime payloads remain in app-private storage.",
                "Resource readings are device-wide, not reserved for this app.",
                "Inspection does not start PRoot, desktop services, guest VMs, or other processes."
            )
        )
    }

    suspend fun prepareWorkspace(id: RuntimeEnvironmentId): RuntimeOperationResult = withContext(Dispatchers.IO) {
        try {
            val runtimeReady = createSafeDirectory(runtimeRoot, appContext.filesDir)
            val managerReady = runtimeReady && listOf("registry", "state", "logs").all {
                createSafeDirectory(File(managerRoot, it), runtimeRoot)
            }
            val root = roots.getValue(id)
            val rootReady = managerReady && createSafeDirectory(root, runtimeRoot)
            val childrenReady = rootReady && listOf("home", "tmp", "logs", "data").all {
                createSafeDirectory(File(root, it), root)
            }
            if (!childrenReady) return@withContext RuntimeOperationResult(false, "Workspace preparation failed containment or filesystem checks.")
            synchronized(this@HybridRuntimeManager) { prepared += id }
            registry.write(id, states.getValue(id), true)
            RuntimeOperationResult(true, "Private workspace prepared and registry persisted. Runtime remains ${states.getValue(id)}; no process was launched.")
        } catch (error: Exception) {
            RuntimeOperationResult(false, error.message ?: "Workspace preparation failed.")
        }
    }

    fun requestStart(id: RuntimeEnvironmentId): RuntimeOperationResult {
        val state = states.getValue(id)
        if (state == RuntimeLifecycleState.UNAVAILABLE) return RuntimeOperationResult(false, "This environment is gated and cannot be started.")
        if (state == RuntimeLifecycleState.RECOVERY_REQUIRED) return RuntimeOperationResult(false, "Resolve the previous session state before requesting a start.")
        return RuntimeOperationResult(false, "No execution adapter is implemented. Workspace readiness is not runtime readiness (state: $state).")
    }

    suspend fun transitionForFutureAdapter(id: RuntimeEnvironmentId, next: RuntimeLifecycleState): RuntimeOperationResult {
        val current = states.getValue(id)
        val allowed = when (current) {
            RuntimeLifecycleState.NOT_INSTALLED -> setOf(RuntimeLifecycleState.READY, RuntimeLifecycleState.FAILED)
            RuntimeLifecycleState.READY -> setOf(RuntimeLifecycleState.STARTING, RuntimeLifecycleState.UNAVAILABLE, RuntimeLifecycleState.FAILED)
            RuntimeLifecycleState.STARTING -> setOf(RuntimeLifecycleState.RUNNING, RuntimeLifecycleState.FAILED, RuntimeLifecycleState.STOPPED)
            RuntimeLifecycleState.RUNNING -> setOf(RuntimeLifecycleState.STOPPING, RuntimeLifecycleState.FAILED)
            RuntimeLifecycleState.STOPPING -> setOf(RuntimeLifecycleState.STOPPED, RuntimeLifecycleState.FAILED)
            RuntimeLifecycleState.STOPPED -> setOf(RuntimeLifecycleState.STARTING, RuntimeLifecycleState.READY, RuntimeLifecycleState.FAILED)
            RuntimeLifecycleState.FAILED -> setOf(RuntimeLifecycleState.STOPPED, RuntimeLifecycleState.READY)
            RuntimeLifecycleState.RECOVERY_REQUIRED -> setOf(RuntimeLifecycleState.STOPPED, RuntimeLifecycleState.FAILED)
            RuntimeLifecycleState.UNAVAILABLE -> emptySet()
        }
        if (next !in allowed) return RuntimeOperationResult(false, "Rejected lifecycle transition: $current -> $next.")
        states[id] = next
        registry.write(id, next, id in prepared)
        return RuntimeOperationResult(true, "Lifecycle state persisted: $current -> $next.")
    }

    suspend fun lastManagerEvent(): String = registry.lastEvent() ?: "No manager event recorded."

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
        directory.exists() && directory.canonicalPath.startsWith(parent.canonicalPath + File.separator) &&
            directory.isDirectory && directory.canRead() && directory.canWrite()
    }.getOrDefault(false)
}
