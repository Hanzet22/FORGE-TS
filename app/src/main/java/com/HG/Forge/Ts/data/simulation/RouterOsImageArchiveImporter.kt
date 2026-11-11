package com.HG.Forge.Ts.data.simulation

import android.content.Context
import android.net.Uri
import android.os.StatFs
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

enum class RouterOsImageArchitecture(val fileSuffix: String) {
    ARM64("-arm64"),
    X86_64("")
}

data class RouterOsImageArchiveResult(
    val filePath: String,
    val version: String,
    val architecture: RouterOsImageArchitecture,
    val archiveSha256: String,
    val imageSizeBytes: Long
)

/**
 * Stages a MikroTik CHR ZIP archive in app-private storage.
 * The supplied SHA-256 is checked against the ZIP bytes before extraction.
 * Only one flat .img entry matching the selected architecture is accepted.
 * This component never boots or executes the image.
 */
class RouterOsImageArchiveImporter(context: Context) {
    private val appContext = context.applicationContext
    private val runtimeRoot = File(appContext.filesDir, "forge-runtime")
    private val imageDirectory = File(runtimeRoot, "images")
    private val maxArchiveBytes = 512L * 1024L * 1024L
    private val maxImageBytes = 1536L * 1024L * 1024L
    private val minimumFreeBytes = 64L * 1024L * 1024L

    @Synchronized
    fun importArchive(
        uri: Uri,
        expectedSha256: String,
        architecture: RouterOsImageArchitecture
    ): RouterOsImageArchiveResult {
        val expected = expectedSha256.trim().lowercase()
        require(expected.matches(Regex("^[a-f0-9]{64}$"))) {
            "Expected SHA-256 must contain exactly 64 hexadecimal characters."
        }
        check(imageDirectory.exists() || imageDirectory.mkdirs()) {
            "Could not prepare the private image directory."
        }
        val canonicalRoot = runtimeRoot.canonicalFile
        val canonicalDirectory = imageDirectory.canonicalFile
        check(canonicalDirectory.path.startsWith(canonicalRoot.path + File.separator)) {
            "Image directory escaped the private runtime root."
        }

        val archivePart = File(imageDirectory, "chr-import.zip.part")
        val imagePart = File(imageDirectory, "chr-import.img.part")
        check(archivePart.canonicalFile.parentFile == canonicalDirectory &&
            imagePart.canonicalFile.parentFile == canonicalDirectory) {
            "Unsafe staging path."
        }

        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var archiveBytes = 0L
            val source = appContext.contentResolver.openInputStream(uri)
                ?: error("The selected archive could not be opened.")
            source.use { input ->
                FileOutputStream(archivePart, false).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        archiveBytes += count
                        check(archiveBytes <= maxArchiveBytes) {
                            "CHR archive exceeds the 512 MiB import limit."
                        }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                    output.fd.sync()
                }
            }
            check(archiveBytes > 0L) { "The selected archive is empty." }
            val actualHash = digest.digest().joinToString("") {
                "%02x".format(it.toInt() and 0xff)
            }
            check(actualHash == expected) {
                "Archive SHA-256 mismatch. The image was rejected."
            }

            val availableBeforeExtraction = StatFs(appContext.filesDir.absolutePath).availableBytes
            check(availableBeforeExtraction > minimumFreeBytes) {
                "Insufficient free app storage to extract the image safely."
            }

            var imageBytes = 0L
            var detectedVersion: String? = null
            var imageEntryCount = 0
            ZipInputStream(FileInputStream(archivePart)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) {
                        zip.closeEntry()
                        continue
                    }
                    imageEntryCount += 1
                    check(imageEntryCount == 1) {
                        "Archive must contain exactly one disk image."
                    }
                    val name = entry.name
                    check(!name.contains('/') && !name.contains('\\') &&
                        name != "." && name != ".." && !name.contains("..")) {
                        "Nested or unsafe archive paths are not accepted."
                    }
                    val suffix = Regex.escape(architecture.fileSuffix)
                    val match = Regex(
                        "^chr-([0-9]+\\.[0-9]+(?:\\.[0-9]+)?[A-Za-z0-9.-]*)${suffix}\\.img$"
                    ).matchEntire(name)
                    check(match != null) {
                        "Archive image filename does not match the selected CHR architecture."
                    }
                    detectedVersion = match.groupValues[1]
                    FileOutputStream(imagePart, false).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = zip.read(buffer)
                            if (count < 0) break
                            imageBytes += count
                            check(imageBytes <= maxImageBytes) {
                                "Extracted CHR image exceeds the 1.5 GiB safety limit."
                            }
                            check(availableBeforeExtraction - imageBytes > minimumFreeBytes) {
                                "Extraction stopped to preserve the minimum free-storage reserve."
                            }
                            output.write(buffer, 0, count)
                        }
                        output.fd.sync()
                    }
                    zip.closeEntry()
                }
            }

            val version = detectedVersion
                ?: error("No supported CHR disk image was found in the archive.")
            check(imageBytes > 0L) { "The extracted CHR disk image is empty." }
            val architectureTag = if (architecture == RouterOsImageArchitecture.ARM64) "arm64" else "x86_64"
            val destination = File(imageDirectory, "chr-$version-$architectureTag.img")
            check(destination.canonicalFile.parentFile == canonicalDirectory) {
                "Unsafe destination path."
            }
            if (destination.exists()) {
                check(destination.delete()) { "Could not replace the previously staged CHR image." }
            }
            check(imagePart.renameTo(destination)) {
                "Could not finalize the verified CHR image."
            }
            return RouterOsImageArchiveResult(
                destination.canonicalPath, version, architecture, actualHash, imageBytes
            )
        } finally {
            archivePart.delete()
            imagePart.delete()
        }
    }
}
