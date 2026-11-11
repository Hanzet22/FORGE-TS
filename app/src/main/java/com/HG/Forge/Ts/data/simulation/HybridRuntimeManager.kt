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
enum class ProotDistribution(val directoryName: String) {
    DEBIAN("debian"), UBUNTU("ubuntu"), ALPINE("alpine")
}

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
    val notes: List<String>,
    val prootInstances: List<ProotRuntimeInstanceSnapshot> = emptyList(),
    val foundation: RuntimeFoundationReport? = null
)
data class RuntimeOperationResult(val accepted: Boolean, val message: String)
data class ProotRuntimeInstanceSnapshot(
    val distribution: ProotDistribution,
    val lifecycle: RuntimeLifecycleState,
    val workspacePath: String,
    val workspaceReady: Boolean
)

private class RuntimeRegistryStore(private val context: Context) {
    data class Record(val state: RuntimeLifecycleState, val workspacePrepared: Boolean)
    data class ProotRecord(val state: RuntimeLifecycleState, val workspacePrepared: Boolean)

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

    suspend fun readProotInstances(): Map<ProotDistribution, ProotRecord> {
        val preferences = context.runtimeRegistryDataStore.data.first()
        return ProotDistribution.entries.associateWith { distribution ->
            val prefix = "proot.instance.${distribution.name}"
            val state = preferences[stringPreferencesKey("$prefix.state")]
                ?.let { runCatching { RuntimeLifecycleState.valueOf(it) }.getOrNull() }
                ?: RuntimeLifecycleState.NOT_INSTALLED
            ProotRecord(state, preferences[stringPreferencesKey("$prefix.workspace_prepared")] == "true")
        }
    }

    suspend fun writeProotInstance(distribution: ProotDistribution, state: RuntimeLifecycleState, prepared: Boolean) {
        context.runtimeRegistryDataStore.edit { preferences ->
            val prefix = "proot.instance.${distribution.name}"
            preferences[stringPreferencesKey("$prefix.state")] = state.name
            preferences[stringPreferencesKey("$prefix.workspace_prepared")] = prepared.toString()
            preferences[stringPreferencesKey("$prefix.updated_at")] = System.currentTimeMillis().toString()
            preferences[stringPreferencesKey("manager.last_event")] =
                "${System.currentTimeMillis()}|PRoot:${distribution.name}|$state"
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

class HybridRuntimeManager(
    context: Context,
    private val adapterRegistry: RuntimeExecutionAdapterRegistry = RuntimeExecutionAdapterRegistry.androidDefault(context)
) {
    private val appContext = context.applicationContext
    private val runtimeRoot = File(appContext.filesDir, "forge-runtime")
    private val managerRoot = File(runtimeRoot, "manager")
    private val registry = RuntimeRegistryStore(appContext)
    private val foundationSupervisor = RuntimeFoundationSupervisor(appContext)
    private val rootfsProvisioner = ProotRootfsProvisioner(runtimeRoot)
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
    private val prootInstanceStates = ProotDistribution.entries.associateWith {
        RuntimeLifecycleState.NOT_INSTALLED
    }.toMutableMap()
    private val preparedProotInstances = mutableSetOf<ProotDistribution>()
    @Volatile private var initialized = false

    suspend fun initialize(): RuntimeOperationResult = withContext(Dispatchers.IO) {
        try {
            val runtimeReady = createSafeDirectory(runtimeRoot, appContext.filesDir)
            val managerReady = runtimeReady && listOf("registry", "state", "logs").all {
                createSafeDirectory(File(managerRoot, it), runtimeRoot)
            }
            if (!managerReady) return@withContext RuntimeOperationResult(false, "Manager storage failed containment or filesystem checks.")
            val records = registry.readAll()
            val prootRecords = registry.readProotInstances()
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
                ProotDistribution.entries.forEach { distribution ->
                    val record = prootRecords.getValue(distribution)
                    val rootfs = File(roots.getValue(RuntimeEnvironmentId.XFCE_PROOT),
                        "${distribution.directoryName}/rootfs/.forge-rootfs.json")
                    val rootfsValid = rootfs.isFile
                    prootInstanceStates[distribution] = when {
                        record.state == RuntimeLifecycleState.STARTING ||
                            record.state == RuntimeLifecycleState.RUNNING ||
                            record.state == RuntimeLifecycleState.STOPPING -> RuntimeLifecycleState.RECOVERY_REQUIRED
                        record.state == RuntimeLifecycleState.READY && !rootfsValid -> RuntimeLifecycleState.RECOVERY_REQUIRED
                        else -> record.state
                    }
                    val instanceRoot = File(roots.getValue(RuntimeEnvironmentId.XFCE_PROOT), distribution.directoryName)
                    if (record.workspacePrepared && isSafeDirectory(instanceRoot, roots.getValue(RuntimeEnvironmentId.XFCE_PROOT))) {
                        preparedProotInstances += distribution
                    } else preparedProotInstances -= distribution
                }
                initialized = true
            }
            RuntimeEnvironmentId.entries.forEach { id ->
                val recovered = states.getValue(id)
                val wasPrepared = records.getValue(id).workspacePrepared
                val isPrepared = id in prepared
                if (records.getValue(id).state != recovered || wasPrepared != isPrepared) registry.write(id, recovered, isPrepared)
            }
            ProotDistribution.entries.forEach { distribution ->
                val previous = prootRecords.getValue(distribution)
                val recovered = prootInstanceStates.getValue(distribution)
                val isPrepared = distribution in preparedProotInstances
                if (previous.state != recovered || previous.workspacePrepared != isPrepared) {
                    registry.writeProotInstance(distribution, recovered, isPrepared)
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
        val managerReady = listOf("registry", "state", "logs").all { isSafeDirectory(File(managerRoot, it), managerRoot) }
        val memory = ActivityManager.MemoryInfo()
        val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memory)
        val storage = StatFs(appContext.filesDir.absolutePath)
        val foundation = foundationSupervisor.inspect(adapterRegistry)
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
            ),
            ProotDistribution.entries.map { distribution ->
                val instanceRoot = File(roots.getValue(RuntimeEnvironmentId.XFCE_PROOT), distribution.directoryName)
                ProotRuntimeInstanceSnapshot(
                    distribution, prootInstanceStates.getValue(distribution), instanceRoot.absolutePath,
                    distribution in preparedProotInstances && isSafeDirectory(instanceRoot, roots.getValue(RuntimeEnvironmentId.XFCE_PROOT))
                )
            },
            foundation
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

    suspend fun prepareProotInstance(distribution: ProotDistribution): RuntimeOperationResult = withContext(Dispatchers.IO) {
        try {
            val runtimeReady = createSafeDirectory(runtimeRoot, appContext.filesDir)
            val managerReady = runtimeReady && listOf("registry", "state", "logs").all {
                createSafeDirectory(File(managerRoot, it), runtimeRoot)
            }
            val prootRoot = roots.getValue(RuntimeEnvironmentId.XFCE_PROOT)
            val prootRootReady = managerReady && createSafeDirectory(prootRoot, runtimeRoot)
            val instanceRoot = File(prootRoot, distribution.directoryName)
            val instanceReady = prootRootReady && createSafeDirectory(instanceRoot, prootRoot)
            val childrenReady = instanceReady && listOf("home", "tmp", "logs", "data").all {
                createSafeDirectory(File(instanceRoot, it), instanceRoot)
            }
            if (!childrenReady) return@withContext RuntimeOperationResult(false, "PRoot instance workspace failed containment or filesystem checks.")
            synchronized(this@HybridRuntimeManager) { preparedProotInstances += distribution }
            registry.writeProotInstance(distribution, prootInstanceStates.getValue(distribution), true)
            RuntimeOperationResult(true, "${distribution.name} workspace prepared under the existing proot/ root.")
        } catch (error: Exception) {
            RuntimeOperationResult(false, error.message ?: "PRoot instance workspace preparation failed.")
        }
    }

    suspend fun installProotRootfs(distribution: ProotDistribution): RuntimeOperationResult = withContext(Dispatchers.IO) {
        val preparedResult = prepareProotInstance(distribution)
        if (!preparedResult.accepted) return@withContext preparedResult
        val current = prootInstanceStates.getValue(distribution)
        if (current == RuntimeLifecycleState.RUNNING || current == RuntimeLifecycleState.STARTING ||
            current == RuntimeLifecycleState.RECOVERY_REQUIRED) {
            return@withContext RuntimeOperationResult(false, "Stop or recover the selected PRoot instance before replacing its rootfs.")
        }
        registry.writeProotInstance(distribution, RuntimeLifecycleState.STARTING, true)
        synchronized(this@HybridRuntimeManager) { prootInstanceStates[distribution] = RuntimeLifecycleState.STARTING }
        val result = rootfsProvisioner.install(distribution)
        val next = if (result.installed) RuntimeLifecycleState.READY else RuntimeLifecycleState.FAILED
        synchronized(this@HybridRuntimeManager) { prootInstanceStates[distribution] = next }
        registry.writeProotInstance(distribution, next, true)
        registry.recordEvent("Rootfs ${distribution.name}: ${result.message}")
        RuntimeOperationResult(result.installed, result.message)
    }

    suspend fun requestStart(
        id: RuntimeEnvironmentId,
        distribution: ProotDistribution? = null
    ): RuntimeOperationResult {
        if (id == RuntimeEnvironmentId.XFCE_PROOT && distribution == null) return RuntimeOperationResult(false, "Select a Debian, Ubuntu, or Alpine instance before starting PRoot.")
        if (id != RuntimeEnvironmentId.XFCE_PROOT && distribution != null) return RuntimeOperationResult(false, "A PRoot distribution can only be used with XFCE_PROOT.")
        val state = if (distribution == null) states.getValue(id) else prootInstanceStates.getValue(distribution)
        if (state == RuntimeLifecycleState.UNAVAILABLE) return RuntimeOperationResult(false, "This environment is gated and cannot be started.")
        if (state == RuntimeLifecycleState.RECOVERY_REQUIRED) return RuntimeOperationResult(false, "Resolve the previous session state before requesting a start.")
        if (state != RuntimeLifecycleState.READY) return RuntimeOperationResult(false, "Runtime must be installed and READY before start; current state is $state. Workspace preparation alone is insufficient.")
        val root = roots.getValue(id)
        val workspace = if (distribution == null) root else File(root, distribution.directoryName)
        val workspaceReady = if (distribution == null) id in prepared && isSafeDirectory(workspace, runtimeRoot)
            else distribution in preparedProotInstances && isSafeDirectory(workspace, root)
        if (!workspaceReady) return RuntimeOperationResult(false, "The selected runtime workspace is not prepared and verified.")
        val adapter = adapterRegistry.find(id) ?: return RuntimeOperationResult(false, "No execution adapter is registered for $id. No process was launched.")
        val foundation = runCatching { foundationSupervisor.inspect(adapterRegistry) }.getOrElse {
            return RuntimeOperationResult(false, "Runtime foundation inspection failed: ${it.message ?: "unknown error"}.")
        }
        if (foundation.status != RuntimeFoundationStatus.READY) return RuntimeOperationResult(false, "Runtime foundation is ${foundation.status}; Android compatibility or app-private foundation storage is not ready. No process was launched.")
        val adapterObservation = foundation.capabilities.executionAdapters[id]
        if (adapterObservation?.status != RuntimeCapabilityStatus.AVAILABLE) return RuntimeOperationResult(false, "Runtime adapter is not reported as registered. No process was launched.")
        val request = RuntimeAdapterStartRequest(id, distribution?.directoryName, workspace.absolutePath)
        val capability = runCatching { adapter.inspect(request) }.getOrElse {
            return RuntimeOperationResult(false, "Adapter capability inspection failed: ${it.message ?: "unknown error"}.")
        }
        if (!capability.available) return RuntimeOperationResult(false, "Adapter unavailable: ${capability.detail}")
        setRuntimeState(id, distribution, RuntimeLifecycleState.STARTING)
        val result = runCatching { adapter.start(request) }.getOrElse {
            setRuntimeState(id, distribution, RuntimeLifecycleState.FAILED)
            return RuntimeOperationResult(false, "Adapter start failed: ${it.message ?: "unknown error"}.")
        }
        return when {
            result.accepted && result.confirmedRunning -> { setRuntimeState(id, distribution, RuntimeLifecycleState.RUNNING); RuntimeOperationResult(true, result.message) }
            !result.accepted -> { setRuntimeState(id, distribution, RuntimeLifecycleState.FAILED); RuntimeOperationResult(false, result.message) }
            else -> RuntimeOperationResult(true, "Start accepted but activity is not confirmed; lifecycle remains STARTING. ${result.message}")
        }
    }

    suspend fun writeProotTerminalInput(distribution: ProotDistribution, input: String): RuntimeInteractiveResult =
        writeProotTerminalBytes(distribution, input.toByteArray(Charsets.UTF_8))

    suspend fun writeProotTerminalBytes(
        distribution: ProotDistribution,
        input: ByteArray
    ): RuntimeInteractiveResult {
        if (prootInstanceStates.getValue(distribution) != RuntimeLifecycleState.RUNNING ||
            distribution !in preparedProotInstances) {
            return RuntimeInteractiveResult(false, message = "The selected PRoot instance is not confirmed RUNNING.")
        }
        val root = roots.getValue(RuntimeEnvironmentId.XFCE_PROOT)
        val workspace = File(root, distribution.directoryName)
        if (!isSafeDirectory(workspace, root))
            return RuntimeInteractiveResult(false, message = "PRoot workspace containment check failed.")
        val adapter = adapterRegistry.find(RuntimeEnvironmentId.XFCE_PROOT)
            ?: return RuntimeInteractiveResult(false, message = "No PRoot adapter is registered.")
        val request = RuntimeAdapterStartRequest(
            RuntimeEnvironmentId.XFCE_PROOT,
            distribution.directoryName,
            workspace.absolutePath
        )
        return runCatching { adapter.writeInteractiveBytes(request, input) }.getOrElse {
            RuntimeInteractiveResult(false, message = it.message ?: "Terminal input failed.")
        }
    }

    suspend fun resizeProotTerminal(
        distribution: ProotDistribution,
        rows: Int,
        columns: Int
    ): RuntimeInteractiveResult {
        if (rows !in 1..1000 || columns !in 1..1000)
            return RuntimeInteractiveResult(false, message = "Terminal dimensions must be between 1 and 1000.")
        if (prootInstanceStates.getValue(distribution) != RuntimeLifecycleState.RUNNING ||
            distribution !in preparedProotInstances) {
            return RuntimeInteractiveResult(false, message = "The selected PRoot instance is not confirmed RUNNING.")
        }
        val root = roots.getValue(RuntimeEnvironmentId.XFCE_PROOT)
        val workspace = File(root, distribution.directoryName)
        if (!isSafeDirectory(workspace, root))
            return RuntimeInteractiveResult(false, message = "PRoot workspace containment check failed.")
        val adapter = adapterRegistry.find(RuntimeEnvironmentId.XFCE_PROOT)
            ?: return RuntimeInteractiveResult(false, message = "No PRoot adapter is registered.")
        val request = RuntimeAdapterStartRequest(RuntimeEnvironmentId.XFCE_PROOT, distribution.directoryName, workspace.absolutePath)
        return runCatching { adapter.resizeInteractive(request, rows, columns) }.getOrElse {
            RuntimeInteractiveResult(false, message = it.message ?: "Terminal resize failed.")
        }
    }

    suspend fun readProotTerminalOutput(distribution: ProotDistribution): RuntimeInteractiveResult {
        if (distribution !in preparedProotInstances) return RuntimeInteractiveResult(false, message = "PRoot workspace is not prepared.")
        val root = roots.getValue(RuntimeEnvironmentId.XFCE_PROOT)
        val workspace = File(root, distribution.directoryName)
        if (!isSafeDirectory(workspace, root)) return RuntimeInteractiveResult(false, message = "PRoot workspace containment check failed.")
        val adapter = adapterRegistry.find(RuntimeEnvironmentId.XFCE_PROOT) ?: return RuntimeInteractiveResult(false, message = "No PRoot adapter is registered.")
        val request = RuntimeAdapterStartRequest(RuntimeEnvironmentId.XFCE_PROOT, distribution.directoryName, workspace.absolutePath)
        return runCatching { adapter.readInteractive(request) }.getOrElse { RuntimeInteractiveResult(false, message = it.message ?: "Terminal output failed.") }
    }

    suspend fun executeProotCommand(distribution: ProotDistribution, arguments: List<String>, timeoutSeconds: Long = 30): RuntimeCommandResult {
        if (prootInstanceStates.getValue(distribution) != RuntimeLifecycleState.RUNNING) return RuntimeCommandResult(false, null, "", "The selected PRoot instance must be confirmed RUNNING before command execution.")
        if (distribution !in preparedProotInstances) return RuntimeCommandResult(false, null, "", "The selected PRoot workspace is not registered as prepared.")
        val root = roots.getValue(RuntimeEnvironmentId.XFCE_PROOT)
        val workspace = File(root, distribution.directoryName)
        if (!isSafeDirectory(workspace, root)) return RuntimeCommandResult(false, null, "", "The selected PRoot workspace failed its containment check.")
        val adapter = adapterRegistry.find(RuntimeEnvironmentId.XFCE_PROOT) ?: return RuntimeCommandResult(false, null, "", "No PRoot execution adapter is registered.")
        val request = RuntimeAdapterStartRequest(RuntimeEnvironmentId.XFCE_PROOT, distribution.directoryName, workspace.absolutePath)
        return runCatching { adapter.execute(request, arguments, timeoutSeconds) }.getOrElse {
            RuntimeCommandResult(false, null, "", "Guest command execution failed: ${it.message ?: "unknown error"}")
        }
    }

    suspend fun requestStop(id: RuntimeEnvironmentId, distribution: ProotDistribution? = null): RuntimeOperationResult {
        if (id == RuntimeEnvironmentId.XFCE_PROOT && distribution == null) return RuntimeOperationResult(false, "Select the Debian, Ubuntu, or Alpine instance to stop.")
        if (id != RuntimeEnvironmentId.XFCE_PROOT && distribution != null) return RuntimeOperationResult(false, "A PRoot distribution can only be stopped through XFCE_PROOT.")
        val current = if (distribution == null) states.getValue(id) else prootInstanceStates.getValue(distribution)
        if (current != RuntimeLifecycleState.RUNNING) return RuntimeOperationResult(false, "Only a confirmed RUNNING runtime can be stopped; current state is $current.")
        val adapter = adapterRegistry.find(id) ?: return RuntimeOperationResult(false, "No execution adapter is registered; no process state was changed.")
        val root = roots.getValue(id)
        val workspace = if (distribution == null) root else File(root, distribution.directoryName)
        val request = RuntimeAdapterStartRequest(id, distribution?.directoryName, workspace.absolutePath)
        setRuntimeState(id, distribution, RuntimeLifecycleState.STOPPING)
        val result = runCatching { adapter.stop(request) }.getOrElse {
            setRuntimeState(id, distribution, RuntimeLifecycleState.FAILED)
            return RuntimeOperationResult(false, "Adapter stop failed: " + (it.message ?: "unknown error") + ".")
        }
        val next = if (result.stopped) RuntimeLifecycleState.STOPPED else RuntimeLifecycleState.FAILED
        setRuntimeState(id, distribution, next)
        return RuntimeOperationResult(result.stopped, result.message)
    }

    suspend fun transitionProotInstanceForFutureAdapter(distribution: ProotDistribution, next: RuntimeLifecycleState): RuntimeOperationResult {
        val current = prootInstanceStates.getValue(distribution)
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
        prootInstanceStates[distribution] = next
        registry.writeProotInstance(distribution, next, distribution in preparedProotInstances)
        return RuntimeOperationResult(true, "Lifecycle state persisted: $current -> $next.")
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

    private suspend fun setRuntimeState(id: RuntimeEnvironmentId, distribution: ProotDistribution?, next: RuntimeLifecycleState) {
        if (distribution == null) { states[id] = next; registry.write(id, next, id in prepared) }
        else { prootInstanceStates[distribution] = next; registry.writeProotInstance(distribution, next, distribution in preparedProotInstances) }
    }

    private fun createSafeDirectory(directory: File, parent: File): Boolean {
        val parentPath = parent.canonicalPath
        val appPath = appContext.filesDir.canonicalPath
        if (parentPath != appPath && !parentPath.startsWith(appPath + File.separator)) return false
        val created = directory.exists() || directory.mkdirs()
        val path = directory.canonicalPath
        return created && path.startsWith(parentPath + File.separator) && directory.isDirectory && directory.canRead() && directory.canWrite()
    }

    private fun isSafeDirectory(directory: File, parent: File): Boolean = runCatching {
        directory.exists() && directory.canonicalPath.startsWith(parent.canonicalPath + File.separator) &&
            directory.isDirectory && directory.canRead() && directory.canWrite()
    }.getOrDefault(false)
}
