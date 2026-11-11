package com.HG.Forge.Ts.data.simulation

import android.system.Os
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.zip.GZIPInputStream

data class RootfsInstallResult(val installed: Boolean, val distribution: ProotDistribution, val message: String, val version: String? = null)

/**
 * Installs official ARM64 rootfs archives into the existing app-private PRoot workspaces.
 * Archive bytes are never executed. A verified staging tree is promoted only after validation.
 */
class ProotRootfsProvisioner(private val runtimeRoot: File) {
    private data class Source(
        val distribution: ProotDistribution,
        val version: String,
        val archiveUrl: String,
        val checksumUrl: String,
        val archiveName: String,
        val format: String,
        val checksumPinned: String? = null,
        val maxArchiveBytes: Long = 512L * 1024 * 1024,
        val maxExpandedBytes: Long = 3L * 1024 * 1024 * 1024
    )

    private val sources = mapOf(
        ProotDistribution.ALPINE to Source(
            ProotDistribution.ALPINE, "3.24.4",
            "https://dl-cdn.alpinelinux.org/alpine/v3.24/releases/aarch64/alpine-minirootfs-3.24.4-aarch64.tar.gz",
            "https://dl-cdn.alpinelinux.org/alpine/v3.24/releases/aarch64/alpine-minirootfs-3.24.4-aarch64.tar.gz.sha256",
            "alpine-minirootfs-3.24.4-aarch64.tar.gz", "gzip",
            "4b14c18f7e90a063b61462ea1462a5732c154bf6adad594974fe6487cd98a8de",
            maxArchiveBytes = 64L * 1024 * 1024
        ),
        ProotDistribution.UBUNTU to Source(
            ProotDistribution.UBUNTU, "24.04-noble",
            "https://partner-images.canonical.com/oci/noble/current/ubuntu-noble-oci-arm64-root.tar.gz",
            "",
            "ubuntu-noble-oci-arm64-root.tar.gz", "gzip",
            "c5a6798b238c14d4714ea0b2db40e83af7c86008e7e31c90ca44355dc36933db",
            maxArchiveBytes = 256L * 1024 * 1024
        ),
        ProotDistribution.DEBIAN to Source(
            ProotDistribution.DEBIAN, "12-bookworm-20261104",
            "https://raw.githubusercontent.com/debuerreotype/docker-debian-artifacts/3b26c3995c7fae47d95ba678632a3520e97054f7/bookworm/oci/blobs/rootfs.tar.gz",
            "",
            "rootfs.tar.gz", "gzip",
            "f59e747f549eda321051637e213896378a19033924e1bfa7ae23a6572729c23c",
            maxArchiveBytes = 256L * 1024 * 1024
        )
    )

    suspend fun install(distribution: ProotDistribution): RootfsInstallResult = withContext(Dispatchers.IO) {
        val source = sources[distribution]
            ?: return@withContext RootfsInstallResult(false, distribution,
                "Debian is gated: its official rootfs is XZ-compressed and its immutable archive endpoint is not yet pinned.")
        val workspace = File(runtimeRoot, "proot/${distribution.directoryName}").canonicalFile
        val expectedParent = File(runtimeRoot, "proot").canonicalFile
        if (!workspace.path.startsWith(expectedParent.path + File.separator) ||
            !(workspace.isDirectory || workspace.mkdirs()) || !workspace.canWrite()) {
            return@withContext RootfsInstallResult(false, distribution, "PRoot workspace failed app-private containment checks.")
        }

        val token = UUID.randomUUID().toString()
        val archive = File(workspace, "rootfs-$token.part")
        val checksumFile = File(workspace, "checksum-$token.part")
        val staging = File(workspace, "rootfs.staging-$token")
        val rootfs = File(workspace, "rootfs")
        val backup = File(workspace, "rootfs.backup-$token")
        try {
            val digest = if (source.checksumUrl.isNotBlank()) {
                val checksumText = downloadText(source.checksumUrl, 256 * 1024)
                val publishedDigest = parseChecksum(checksumText, source.archiveName)
                val pinned = source.checksumPinned
                if (pinned != null && !publishedDigest.equals(pinned, ignoreCase = true)) {
                    throw IllegalStateException("Pinned digest does not match the official upstream checksum metadata.")
                }
                publishedDigest
            } else {
                source.checksumPinned ?: throw IllegalStateException("No pinned or published SHA-256 digest is configured.")
            }
            if (!digest.matches(Regex("[0-9a-fA-F]{64}"))) {
                throw IllegalStateException("Official checksum metadata did not contain a valid SHA-256 digest.")
            }
            download(source.archiveUrl, archive, source.maxArchiveBytes)
            val actual = sha256(archive)
            if (!actual.equals(digest, ignoreCase = true)) {
                throw IllegalStateException("Rootfs SHA-256 mismatch. Archive was rejected before extraction.")
            }
            if (staging.exists() && !staging.deleteRecursively()) throw IllegalStateException("Cannot clear staging directory.")
            if (!staging.mkdirs()) throw IllegalStateException("Cannot create isolated staging directory.")
            extractArchive(archive, staging, source)
            validateRootfs(staging)
            File(staging, ".forge-rootfs.json").writeText(
                """{"distribution":"${distribution.directoryName}","version":"${source.version}","architecture":"arm64","archive":"${source.archiveName}","sha256":"$actual","verified":"official-https-sha256","installedAt":"${System.currentTimeMillis()}"}"""
            )
            if (rootfs.exists()) {
                if (!rootfs.isDirectory || !rootfs.renameTo(backup)) throw IllegalStateException("Cannot preserve previous rootfs for atomic replacement.")
            }
            if (!staging.renameTo(rootfs)) {
                if (backup.exists()) backup.renameTo(rootfs)
                throw IllegalStateException("Cannot atomically promote the verified rootfs.")
            }
            backup.deleteRecursively()
            RootfsInstallResult(true, distribution, "Official ARM64 rootfs downloaded, SHA-256 verified, extracted and promoted. PRoot engine probe is still required before runtime start.", source.version)
        } catch (error: Exception) {
            staging.deleteRecursively()
            if (backup.exists() && !rootfs.exists()) backup.renameTo(rootfs)
            RootfsInstallResult(false, distribution, error.message ?: "Rootfs installation failed.")
        } finally {
            archive.delete()
            checksumFile.delete()
        }
    }

    private fun downloadText(url: String, limit: Int): String {
        val file = File.createTempFile("forge-checksum-", ".txt", runtimeRoot)
        try {
            download(url, file, limit.toLong())
            return file.readText(Charsets.UTF_8)
        } finally { file.delete() }
    }

    private fun download(url: String, destination: File, limit: Long) {
        var current = URL(url)
        repeat(4) { attempt ->
            require(current.protocol == "https") { "Only HTTPS rootfs sources are accepted." }
            val connection = current.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 20_000
            connection.readTimeout = 45_000
            connection.setRequestProperty("User-Agent", "FORGE-TS-Rootfs/0.8")
            try {
                val status = connection.responseCode
                if (status in 300..399) {
                    val location = connection.getHeaderField("Location") ?: error("Redirect without a target.")
                    val next = URL(current, location)
                    require(next.protocol == "https" && next.host.equals(current.host, true)) {
                        "Cross-origin or HTTPS-downgrade redirect rejected."
                    }
                    current = next
                    return@repeat
                }
                require(status in 200..299) { "Upstream download returned HTTP $status." }
                val length = connection.contentLengthLong
                require(length <= 0L || length <= limit) { "Upstream archive exceeds the configured download limit." }
                destination.parentFile?.mkdirs()
                FileOutputStream(destination).use { output ->
                    connection.inputStream.use { input ->
                        val buffer = ByteArray(64 * 1024)
                        var total = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            total += read
                            require(total <= limit) { "Download exceeded the configured size limit." }
                            output.write(buffer, 0, read)
                        }
                        output.fd.sync()
                    }
                }
                return
            } finally { connection.disconnect() }
        }
        error("Too many upstream redirects.")
    }

    private fun parseChecksum(text: String, fileName: String): String {
        val line = text.lineSequence().firstOrNull { it.contains(fileName) }
            ?: throw IllegalStateException("Official checksum list does not contain the selected ARM64 archive.")
        return line.trim().substringBefore(' ').trim().lowercase(Locale.US)
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun extractArchive(archive: File, destination: File, source: Source) {
        val deferredLinks = mutableListOf<TarArchiveEntry>()
        var entries = 0
        var expanded = 0L
        val raw = BufferedInputStream(archive.inputStream())
        val decoded = if (source.format == "xz") XZCompressorInputStream(raw) else GZIPInputStream(raw)
        TarArchiveInputStream(decoded).use { tar ->
            while (true) {
                val entry = tar.nextTarEntry ?: break
                entries++
                require(entries <= 250_000) { "Archive entry-count limit exceeded." }
                val relative = safeArchivePath(entry.name)
                val target = File(destination, relative)
                require(isContained(target, destination)) { "Archive path escaped staging." }
                if (entry.isSymbolicLink || entry.isLink) {
                    deferredLinks += entry
                    continue
                }
                require(entry.isFile || entry.isDirectory) { "Device nodes, sockets, and special archive entries are rejected." }
                ensureNoSymlinkParents(target, destination)
                if (entry.isDirectory) {
                    require(target.isDirectory || target.mkdirs()) { "Cannot create archive directory." }
                    Os.chmod(target.absolutePath, (entry.mode and 0x1ff).coerceAtLeast(0x1c0))
                } else {
                    require(entry.size >= 0L && entry.size <= 512L * 1024 * 1024) { "Archive file exceeds per-file limit." }
                    expanded += entry.size
                    require(expanded <= source.maxExpandedBytes) { "Expanded rootfs size limit exceeded." }
                    target.parentFile?.let { require(it.isDirectory || it.mkdirs()) { "Cannot create archive parent directory." } }
                    require(!target.exists()) { "Duplicate archive path rejected." }
                    FileOutputStream(target).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var copied = 0L
                        while (copied < entry.size) {
                            val read = tar.read(buffer, 0, minOf(buffer.size.toLong(), entry.size - copied).toInt())
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            copied += read
                        }
                        require(copied == entry.size) { "Truncated archive member rejected." }
                    }
                    Os.chmod(target.absolutePath, entry.mode and 0x1ff)
                }
            }
        }
        deferredLinks.filter { it.isLink }.forEach { entry ->
            val link = File(destination, safeArchivePath(entry.name))
            ensureNoSymlinkParents(link, destination)
            val linkTarget = File(destination, safeArchivePath(entry.linkName))
            require(linkTarget.isFile && isContained(linkTarget, destination)) { "Unsafe or missing hard-link target rejected." }
            require(!link.exists()) { "Duplicate hard-link path rejected." }
            java.nio.file.Files.createLink(link.toPath(), linkTarget.toPath())
        }
        deferredLinks.filter { it.isSymbolicLink }.forEach { entry ->
            val link = File(destination, safeArchivePath(entry.name))
            ensureNoSymlinkParents(link, destination)
            val linkTarget = entry.linkName
            require(linkTarget.isNotBlank() && '\u0000' !in linkTarget && '\\' !in linkTarget) { "Invalid symbolic link target." }
            val resolved = if (linkTarget.startsWith("/")) linkTarget.removePrefix("/")
                else File(link.parentFile, linkTarget).relativeTo(destination).path
            val normalized = normalizeGuestPath(resolved)
            require(normalized != null) { "Symbolic link target escapes the guest root." }
            require(!link.exists()) { "Duplicate symbolic-link path rejected." }
            Os.symlink(linkTarget, link.absolutePath)
        }
    }

    private fun safeArchivePath(raw: String): String {
        require(raw.isNotBlank() && raw.length <= 4096 && '\u0000' !in raw && '\\' !in raw && !raw.startsWith("/")) {
            "Unsafe absolute, empty, or malformed archive path rejected."
        }
        val normalized = normalizeGuestPath(raw) ?: throw IllegalStateException("Archive path traversal rejected.")
        return normalized
    }

    private fun normalizeGuestPath(raw: String): String? {
        val parts = ArrayDeque<String>()
        raw.split('/').forEach { part ->
            when (part) {
                "", "." -> Unit
                ".." -> if (parts.isEmpty()) return null else parts.removeLast()
                else -> parts.addLast(part)
            }
        }
        return parts.joinToString("/")
    }

    private fun ensureNoSymlinkParents(target: File, root: File) {
        var parent = target.parentFile ?: throw IllegalStateException("Archive entry has no parent.")
        while (parent != root && parent.path.startsWith(root.path + File.separator)) {
            require(!java.nio.file.Files.isSymbolicLink(parent.toPath())) { "Archive attempted to write through a symbolic-link parent." }
            if (!parent.exists()) require(parent.mkdirs()) { "Cannot create archive parent." }
            parent = parent.parentFile ?: break
        }
        require(isContained(target, root)) { "Archive member escaped staging." }
    }

    private fun isContained(file: File, root: File): Boolean {
        val rootPath = root.canonicalPath
        val filePath = file.canonicalPath
        return filePath == rootPath || filePath.startsWith(rootPath + File.separator)
    }

    private fun validateRootfs(root: File) {
        require(File(root, "etc").isDirectory && File(root, "usr").isDirectory) {
            "Rootfs validation failed: expected /etc and /usr trees are missing."
        }
        val shellCandidates = listOf("/bin/sh", "/usr/bin/sh")
        val shell = shellCandidates.firstOrNull { RootfsGuestPath.isExecutableGuestFile(root, it) }
        require(shell != null) {
            "Rootfs validation failed: no readable executable guest shell was found at /bin/sh or /usr/bin/sh."
        }
    }
}
