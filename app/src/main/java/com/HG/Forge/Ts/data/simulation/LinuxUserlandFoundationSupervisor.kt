package com.HG.Forge.Ts.data.simulation

import android.content.Context
import android.os.StatFs
import java.io.File

enum class LinuxUserlandFoundationStatus { READY, LIMITED }

data class LinuxUserlandFoundationReport(
    val status: LinuxUserlandFoundationStatus,
    val rootPath: String,
    val rootfsDirectoryReady: Boolean,
    val homeDirectoryReady: Boolean,
    val temporaryDirectoryReady: Boolean,
    val logDirectoryReady: Boolean,
    val availableStorageBytes: Long,
    val notes: List<String>
)

/**
 * Phase 8 foundation increment.
 *
 * Prepares a private workspace for a future Linux userland. It deliberately
 * does not download, extract, mount, or execute a root filesystem and does not
 * start XFCE4, a display server, QEMU, or a RouterOS guest.
 */
class LinuxUserlandFoundationSupervisor(context: Context) {
    private val appContext = context.applicationContext
    private val runtimeRoot = File(appContext.filesDir, "forge-runtime")
    private val root = File(runtimeRoot, "linux-userland")

    @Synchronized
    fun prepare(): LinuxUserlandFoundationReport {
        val notes = mutableListOf<String>()
        val appFiles = appContext.filesDir.canonicalFile
        val runtimeCanonical = runtimeRoot.canonicalFile
        val rootCanonical = root.canonicalFile

        val runtimeCreated = runtimeRoot.exists() || runtimeRoot.mkdirs()
        val rootCreated = root.exists() || root.mkdirs()
        val rootContained = rootCanonical.path.startsWith(appFiles.path + File.separator) &&
            rootCanonical.path.startsWith(runtimeCanonical.path + File.separator)
        val rootReady = runtimeCreated && rootCreated && rootContained &&
            runtimeRoot.isDirectory && root.isDirectory && root.canWrite()

        val directories = listOf(
            File(root, "rootfs"),
            File(root, "home"),
            File(root, "tmp"),
            File(root, "logs")
        )
        val ready = directories.map { directory ->
            val created = directory.exists() || directory.mkdirs()
            val canonical = directory.canonicalFile
            val contained = canonical.path.startsWith(rootCanonical.path + File.separator)
            created && contained && directory.isDirectory && directory.canRead() && directory.canWrite()
        }

        if (!rootReady || ready.any { !it }) {
            notes += "One or more Linux workspace directories could not be prepared safely."
        }
        notes += "Workspace preparation only: no Linux distribution or root filesystem is bundled, downloaded, extracted, mounted, or executed."
        notes += "XFCE4, display services, QEMU, and RouterOS CHR are not started by this component."
        notes += "A future rootfs importer must verify an official source and checksum, enforce archive size limits, and reject path traversal and unsafe links."

        val storage = StatFs(appContext.filesDir.absolutePath)
        val allReady = rootReady && ready.all { it }
        return LinuxUserlandFoundationReport(
            status = if (allReady) LinuxUserlandFoundationStatus.READY else LinuxUserlandFoundationStatus.LIMITED,
            rootPath = rootCanonical.path,
            rootfsDirectoryReady = ready.getOrElse(0) { false },
            homeDirectoryReady = ready.getOrElse(1) { false },
            temporaryDirectoryReady = ready.getOrElse(2) { false },
            logDirectoryReady = ready.getOrElse(3) { false },
            availableStorageBytes = storage.availableBytes,
            notes = notes
        )
    }
}
