package com.HG.Forge.Ts.data.simulation

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URI
import java.net.InetAddress
import java.security.MessageDigest

/** Applies an explicit HTTPS mirror while preserving the distro's signed package metadata policy. */
internal class ProotPackageMirrorManager(private val runtimeRoot: File) {
    data class Result(val applied: Boolean, val message: String)

    fun apply(distribution: ProotDistribution, input: String?): Result = runCatching {
        val instance = File(runtimeRoot, "proot/${distribution.directoryName}").canonicalFile
        require(instance.path.startsWith(File(runtimeRoot, "proot").canonicalPath + File.separator)) {
            "PRoot mirror workspace failed its path containment check."
        }
        val rootfs = File(instance, "rootfs").canonicalFile
        require(rootfs.path.startsWith(instance.path + File.separator)) { "PRoot rootfs failed its path containment check." }
        require(rootfs.isDirectory && File(rootfs, ".forge-rootfs.json").isFile) {
            "Install and verify this distribution's rootfs before changing its package mirror."
        }
        val configFiles = findConfigFiles(rootfs, distribution)
        require(configFiles.isNotEmpty()) { "No package repository configuration was found in this rootfs." }
        val backupDirectory = File(instance, ".forge-mirror-originals").canonicalFile
        require(backupDirectory.path.startsWith(instance.path + File.separator))
        if (!backupDirectory.exists()) require(backupDirectory.mkdirs()) { "Could not create a private mirror backup directory." }
        val manifestFile = safeChild(backupDirectory, "manifest.json")
        val manifest = if (manifestFile.isFile) JSONObject(manifestFile.readText()) else {
            val entries = JSONArray()
            configFiles.forEach { file ->
                val relative = file.relativeTo(rootfs).invariantSeparatorsPath
                val backupName = sha256(relative.toByteArray()) + ".original"
                File(backupDirectory, backupName).writeBytes(file.readBytes())
                entries.put(JSONObject().put("path", relative).put("backup", backupName))
            }
            JSONObject().put("files", entries).also { manifestFile.writeText(it.toString()) }
        }

        val originals = manifest.getJSONArray("files")
        if (input.isNullOrBlank()) {
            val restores = mutableListOf<Pair<File, ByteArray>>()
            for (index in 0 until originals.length()) {
                val item = originals.getJSONObject(index)
                val destination = safeChild(rootfs, item.getString("path"))
                val original = safeChild(backupDirectory, item.getString("backup"))
                require(original.isFile) { "A saved package source backup is missing." }
                restores += destination to original.readBytes()
            }
            restores.forEach { (destination, bytes) -> destination.writeBytes(bytes) }
            return Result(true, "The original ${distribution.name} package sources were restored.")
        }

        val mirror = validateMirror(input, distribution)
        val updates = mutableListOf<Pair<File, String>>()
        for (index in 0 until originals.length()) {
            val item = originals.getJSONObject(index)
            val destination = safeChild(rootfs, item.getString("path"))
            val original = safeChild(backupDirectory, item.getString("backup"))
            require(original.isFile) { "A saved package source backup is missing." }
            val source = original.readText()
            val updated = when (distribution) {
                ProotDistribution.DEBIAN, ProotDistribution.UBUNTU -> updateAptSources(source, mirror, distribution)
                ProotDistribution.ALPINE -> source.lineSequence().joinToString("\n") { line ->
                    if (line.trimStart().startsWith("#") || line.isBlank()) line
                    else line.replace(Regex("^\\s*https?://[^/]+/alpine(?:/|$)"), "$mirror/")
                } + if (source.endsWith("\n")) "\n" else ""
            }
            require(updated != source || source.contains(mirror)) { "No supported repository URI was found in ${item.getString("path")}." }
            updates += destination to updated
        }
        updates.forEach { (destination, content) -> destination.writeText(content) }
        Result(true, "HTTPS package mirror configured for ${distribution.name}. Package signature verification remains enabled; run the distro package-index update to check availability.")
    }.getOrElse { Result(false, it.message ?: "Package mirror update failed.") }

    private fun findConfigFiles(rootfs: File, distribution: ProotDistribution): List<File> {
        val relativePaths = when (distribution) {
            ProotDistribution.DEBIAN, ProotDistribution.UBUNTU -> listOf("etc/apt/sources.list") +
                File(rootfs, "etc/apt/sources.list.d").listFiles().orEmpty()
                    .filter { it.isFile && (it.name.endsWith(".sources") || it.name.endsWith(".list")) }
                    .map { it.relativeTo(rootfs).invariantSeparatorsPath }
            ProotDistribution.ALPINE -> listOf("etc/apk/repositories")
        }
        return relativePaths.mapNotNull { relative ->
            runCatching { safeChild(rootfs, relative) }.getOrNull()?.takeIf { it.isFile }
        }
    }

    private fun updateAptSources(source: String, mirror: String, distribution: ProotDistribution): String = source.lineSequence().joinToString("\n") { line ->
        when {
            line.trimStart().startsWith("#") -> line
            line.trimStart().startsWith("URIs:") -> {
                val uriLine = Regex("^(\\s*URIs:\\s*)(.+)$")
                val match = uriLine.matchEntire(line)
                if (match == null) line else {
                    val originalUris = match.groupValues[2].split(Regex("\\s+")).filter(String::isNotBlank)
                    val updatedUris = originalUris.map { uri ->
                        if (distribution == ProotDistribution.DEBIAN && (uri.contains("debian-security") || uri.contains("security.debian.org"))) uri else mirror
                    }.distinct().joinToString(" ")
                    match.groupValues[1] + updatedUris
                }
            }
            Regex("^\\s*deb(?:-src)?\\s").containsMatchIn(line) -> {
                val aptLine = Regex("^(\\s*deb(?:-src)?\\s+(?:\\[[^]]*]\\s+)?)(\\S+)(\\s+.*)$")
                val match = aptLine.matchEntire(line)
                if (match == null) line else {
                    val originalUri = match.groupValues[2]
                    if (distribution == ProotDistribution.DEBIAN && (originalUri.contains("debian-security") || originalUri.contains("security.debian.org"))) line
                    else match.groupValues[1] + mirror + match.groupValues[3]
                }
            }
            else -> line
        }
    } + if (source.endsWith("\n")) "\n" else ""

    private fun validateMirror(input: String, distribution: ProotDistribution): String {
        val normalized = input.trim().removeSuffix("/")
        require(normalized.length <= 300) { "Mirror URL is too long." }
        val uri = URI(normalized)
        require(uri.scheme.equals("https", true) && uri.userInfo == null && uri.query == null && uri.fragment == null) {
            "Use an HTTPS mirror URL without credentials, query parameters or fragments."
        }
        require(uri.port == -1 || uri.port == 443) { "Only the standard HTTPS port is supported." }
        val host = uri.host ?: throw IllegalArgumentException("Enter a valid HTTPS mirror host.")
        require(host.contains('.') && host.matches(Regex("[A-Za-z0-9.-]+")) && host.split('.').all { it.isNotBlank() && it.length <= 63 && !it.startsWith('-') && !it.endsWith('-') }) {
            "Mirror host must be a public DNS hostname."
        }
        require(host.split('.').any { label -> label.any { !it.isDigit() } }) { "Enter a DNS hostname rather than a raw IP address." }
        val resolved = InetAddress.getAllByName(host)
        require(resolved.isNotEmpty() && resolved.all(::isPublicAddress)) { "Mirror host must resolve only to public addresses." }
        val requiredSuffix = when (distribution) {
            ProotDistribution.DEBIAN -> "/debian"
            ProotDistribution.UBUNTU -> "/ubuntu-ports"
            ProotDistribution.ALPINE -> "/alpine"
        }
        require(uri.path.trimEnd('/').endsWith(requiredSuffix)) {
            "For ${distribution.name}, the mirror URL must end in $requiredSuffix."
        }
        return normalized
    }

    private fun safeChild(parent: File, relativePath: String): File {
        require(!relativePath.startsWith('/') && relativePath.split('/').none { it == ".." || it.isBlank() })
        val child = File(parent, relativePath).canonicalFile
        require(child.path.startsWith(parent.canonicalPath + File.separator)) { "Mirror file path escaped its private workspace." }
        return child
    }

    private fun isPublicAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
            address.isSiteLocalAddress || address.isMulticastAddress) return false
        val bytes = address.address.map { it.toInt() and 0xff }
        return if (bytes.size == 4) {
            val a = bytes[0]
            val b = bytes[1]
            a != 0 && a != 10 && a != 127 && a < 224 &&
                !(a == 100 && b in 64..127) && !(a == 169 && b == 254) &&
                !(a == 172 && b in 16..31) && !(a == 192 && b == 168) &&
                !(a == 192 && b <= 2) && !(a == 198 && b in 18..19) &&
                !(a == 198 && b == 51) && !(a == 203 && b == 0)
        } else {
            (bytes[0] and 0xfe) != 0xfc && bytes[0] != 0xff
        }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }
}
