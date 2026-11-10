package com.HG.Forge.Ts.data.simulation

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import java.io.File

enum class RuntimeFoundationStatus { IDLE, INSPECTING, READY, LIMITED, FAILED }
enum class RuntimeCapabilityStatus { AVAILABLE, UNAVAILABLE, BLOCKED, UNKNOWN }

data class RuntimeCapabilityObservation(
    val status: RuntimeCapabilityStatus,
    val detail: String
)

data class RuntimeCapabilityReport(
    val processArchitecture: String,
    val kvmNode: RuntimeCapabilityObservation,
    val kvmReadPermission: RuntimeCapabilityObservation,
    val kvmWritePermission: RuntimeCapabilityObservation,
    val kvmApiProbe: RuntimeCapabilityObservation,
    val kvmVmCreationProbe: RuntimeCapabilityObservation,
    val prootPtraceProbe: RuntimeCapabilityObservation,
    val executionAdapters: Map<RuntimeEnvironmentId, RuntimeCapabilityObservation>
)

data class RuntimeFoundationReport(
    val status: RuntimeFoundationStatus,
    val supportedAbis: List<String>,
    val apiLevel: Int,
    val availableProcessors: Int,
    val availableMemoryBytes: Long,
    val totalMemoryBytes: Long,
    val availableStorageBytes: Long,
    val totalStorageBytes: Long,
    val kvmNodeVisible: Boolean,
    val kvmNodeReadable: Boolean,
    val kvmNodeWritable: Boolean,
    val runtimeRoot: String,
    val imageDirectoryReady: Boolean,
    val instanceDirectoryReady: Boolean,
    val logDirectoryReady: Boolean,
    val notes: List<String>,
    val capabilities: RuntimeCapabilityReport
)

/**
 * Reports device facts and app-private storage readiness only.
 * It never infers KVM usability from /dev/kvm metadata or PRoot support
 * without a process-local ptrace probe performed by a concrete adapter.
 */
class RuntimeFoundationSupervisor(context: Context) {
    private val appContext = context.applicationContext
    private val root = File(appContext.filesDir, "forge-runtime")

    @Synchronized
    fun inspect(
        adapterRegistry: RuntimeExecutionAdapterRegistry = RuntimeExecutionAdapterRegistry.empty()
    ): RuntimeFoundationReport {
        val notes = mutableListOf<String>()
        val memory = ActivityManager.MemoryInfo()
        val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memory)
        val storage = StatFs(appContext.filesDir.absolutePath)
        val kvm = File("/dev/kvm")
        // /dev/kvm is a character device, not a regular file.
        val kvmVisible = kvm.exists()
        val directories = listOf(File(root, "images"), File(root, "instances"), File(root, "logs"))
        val appFilesRoot = appContext.filesDir.canonicalFile
        val canonicalRoot = root.canonicalFile
        val rootCreated = root.exists() || root.mkdirs()
        val rootReady = rootCreated &&
            canonicalRoot.path.startsWith(appFilesRoot.path + File.separator) &&
            root.isDirectory && root.canRead() && root.canWrite()
        val directoryReadiness = directories.map { directory ->
            val created = rootReady && (directory.exists() || directory.mkdirs())
            val canonicalDirectory = directory.canonicalFile
            val contained = canonicalDirectory.path.startsWith(canonicalRoot.path + File.separator)
            created && contained && directory.isDirectory && directory.canRead() && directory.canWrite()
        }
        val directoriesReady = rootReady && directoryReadiness.all { it }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            notes += "Android API below the declared app minimum; runtime foundation is unsupported."
        }
        if (Build.SUPPORTED_ABIS.none { it.contains("arm64", ignoreCase = true) }) {
            notes += "No ARM64 ABI reported. RouterOS ARM64 integration is unavailable for this ABI; other runtime architectures are not disabled."
        }
        if (!directoriesReady) notes += "One or more app-private runtime directories could not be prepared safely."
        if (!kvmVisible) {
            notes += "/dev/kvm is not visible to this app process; hardware or kernel support remains unknown."
        } else {
            notes += "/dev/kvm is visible; read/write flags are advisory and do not prove KVM can be used."
        }
        notes += "PRoot ptrace remains UNKNOWN until a concrete adapter probes the actual app process and its disposable child."
        notes += "Foundation READY means platform-minimum and private-directory readiness only; it does not mean a runtime engine is executable."

        val kvmNodeObservation = if (kvmVisible) {
            RuntimeCapabilityObservation(RuntimeCapabilityStatus.AVAILABLE, "/dev/kvm is visible as a device node only; KVM usability is not established.")
        } else {
            RuntimeCapabilityObservation(RuntimeCapabilityStatus.UNKNOWN, "/dev/kvm is not visible to this app process; hardware/kernel support is not established as absent.")
        }
        val kvmReadObservation = when {
            !kvmVisible -> RuntimeCapabilityObservation(RuntimeCapabilityStatus.UNKNOWN, "No visible KVM node to inspect.")
            kvm.canRead() -> RuntimeCapabilityObservation(RuntimeCapabilityStatus.AVAILABLE, "File API reports readable; advisory metadata only, no open/ioctl probe.")
            else -> RuntimeCapabilityObservation(RuntimeCapabilityStatus.UNKNOWN, "File API does not report read permission; no open attempt was made, so access remains unknown.")
        }
        val kvmWriteObservation = when {
            !kvmVisible -> RuntimeCapabilityObservation(RuntimeCapabilityStatus.UNKNOWN, "No visible KVM node to inspect.")
            kvm.canWrite() -> RuntimeCapabilityObservation(RuntimeCapabilityStatus.AVAILABLE, "File API reports writable; advisory metadata only, no open/ioctl probe.")
            else -> RuntimeCapabilityObservation(RuntimeCapabilityStatus.UNKNOWN, "File API does not report write permission; no open attempt was made, so access remains unknown.")
        }
        val adapterObservations = RuntimeEnvironmentId.entries.associateWith { id ->
            if (adapterRegistry.find(id) != null) {
                RuntimeCapabilityObservation(RuntimeCapabilityStatus.AVAILABLE, "An adapter is registered; backend inspection is still required before start.")
            } else {
                RuntimeCapabilityObservation(RuntimeCapabilityStatus.UNAVAILABLE, "No execution adapter is registered for this environment.")
            }
        }
        val capabilities = RuntimeCapabilityReport(
            processArchitecture = System.getProperty("os.arch") ?: "unknown",
            kvmNode = kvmNodeObservation,
            kvmReadPermission = kvmReadObservation,
            kvmWritePermission = kvmWriteObservation,
            kvmApiProbe = RuntimeCapabilityObservation(RuntimeCapabilityStatus.UNKNOWN, "KVM_GET_API_VERSION and required extensions have not been probed."),
            kvmVmCreationProbe = RuntimeCapabilityObservation(RuntimeCapabilityStatus.UNKNOWN, "KVM_CREATE_VM has not been attempted; no VM is created during foundation inspection."),
            prootPtraceProbe = RuntimeCapabilityObservation(RuntimeCapabilityStatus.UNKNOWN, "No process-local ptrace probe has run; the concrete PRoot adapter must probe its actual launch path."),
            executionAdapters = adapterObservations
        )

        val foundationReady = directoriesReady && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        return RuntimeFoundationReport(
            status = if (foundationReady) RuntimeFoundationStatus.READY else RuntimeFoundationStatus.LIMITED,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            apiLevel = Build.VERSION.SDK_INT,
            availableProcessors = Runtime.getRuntime().availableProcessors(),
            availableMemoryBytes = memory.availMem,
            totalMemoryBytes = memory.totalMem,
            availableStorageBytes = storage.availableBytes,
            totalStorageBytes = storage.totalBytes,
            kvmNodeVisible = kvmVisible,
            kvmNodeReadable = kvmVisible && kvm.canRead(),
            kvmNodeWritable = kvmVisible && kvm.canWrite(),
            runtimeRoot = root.canonicalPath,
            imageDirectoryReady = directoryReadiness[0],
            instanceDirectoryReady = directoryReadiness[1],
            logDirectoryReady = directoryReadiness[2],
            notes = notes,
            capabilities = capabilities
        )
    }
}
