package com.HG.Forge.Ts.data.simulation

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import java.io.File

enum class RuntimeFoundationStatus { IDLE, INSPECTING, READY, LIMITED, FAILED }

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
    val notes: List<String>
)

/**
 * Phase 6 foundation only. Inspects Android resources and prepares app-private
 * directories. It never launches processes, opens /dev/kvm, downloads images,
 * creates networks, or starts a guest.
 */
class RuntimeFoundationSupervisor(context: Context) {
    private val appContext = context.applicationContext
    private val root = File(appContext.filesDir, "forge-runtime")

    @Synchronized
    fun inspect(): RuntimeFoundationReport {
        val notes = mutableListOf<String>()
        val memory = ActivityManager.MemoryInfo()
        val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memory)

        val storage = StatFs(appContext.filesDir.absolutePath)
        val kvm = File("/dev/kvm")
        val directories = listOf(
            File(root, "images"),
            File(root, "instances"),
            File(root, "logs")
        )

        var directoriesReady = true
        directories.forEach { directory ->
            val created = directory.exists() || directory.mkdirs()
            val canonicalRoot = root.canonicalFile
            val canonicalDirectory = directory.canonicalFile
            val contained = canonicalDirectory.path.startsWith(canonicalRoot.path + File.separator)
            val valid = created && contained && directory.isDirectory && directory.canWrite()
            if (!valid) directoriesReady = false
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            notes += "Android API below the declared app minimum; runtime foundation is unsupported."
        }
        if (Build.SUPPORTED_ABIS.none { it.contains("arm64", ignoreCase = true) }) {
            notes += "No ARM64 ABI reported. RouterOS ARM64 guest integration is unavailable on this ABI."
        }
        if (!directoriesReady) notes += "One or more app-private runtime directories could not be prepared safely."
        if (!kvm.exists()) {
            notes += "/dev/kvm is not visible. This does not establish whether another virtualization path exists."
        } else {
            notes += "/dev/kvm node is visible, but node visibility and file permissions do not prove KVM can be used."
        }
        notes += "KVM functionality, CHR boot compatibility, guest isolation, and device performance are not tested in Phase 6."

        val ready = directoriesReady && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            Build.SUPPORTED_ABIS.any { it.contains("arm64", ignoreCase = true) }
        return RuntimeFoundationReport(
            status = if (ready) RuntimeFoundationStatus.READY else RuntimeFoundationStatus.LIMITED,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            apiLevel = Build.VERSION.SDK_INT,
            availableProcessors = Runtime.getRuntime().availableProcessors(),
            availableMemoryBytes = memory.availMem,
            totalMemoryBytes = memory.totalMem,
            availableStorageBytes = storage.availableBytes,
            totalStorageBytes = storage.totalBytes,
            kvmNodeVisible = kvm.exists(),
            kvmNodeReadable = kvm.canRead(),
            kvmNodeWritable = kvm.canWrite(),
            runtimeRoot = root.canonicalPath,
            imageDirectoryReady = directories[0].isDirectory && directories[0].canWrite(),
            instanceDirectoryReady = directories[1].isDirectory && directories[1].canWrite(),
            logDirectoryReady = directories[2].isDirectory && directories[2].canWrite(),
            notes = notes
        )
    }
}
