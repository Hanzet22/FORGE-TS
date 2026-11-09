package com.HG.Forge.Ts.data.simulation

import android.content.Context
import android.net.Uri
import android.os.StatFs
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

data class RouterOsImageImportResult(
    val filePath: String,
    val sha256: String,
    val sizeBytes: Long
)

/**
 * Stages a user-selected official RouterOS CHR ARM64 RAW image in app-private
 * storage after SHA-256 verification. This does not execute or boot the image.
 */
class RouterOsImageImporter(context: Context) {
    private val appContext = context.applicationContext
    private val runtimeRoot = File(appContext.filesDir, "forge-runtime")
    private val imageDirectory = File(runtimeRoot, "images")
    private val maxImageBytes = 1536L * 1024L * 1024L

    @Synchronized
    fun importRawImage(uri: Uri, expectedSha256: String): RouterOsImageImportResult {
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

        val stat = StatFs(appContext.filesDir.absolutePath)
        check(stat.availableBytes > 64L * 1024L * 1024L) {
            "Insufficient free app storage to safely stage an image."
        }

        val staging = File(imageDirectory, "chr-arm64.raw.part")
        val destination = File(imageDirectory, "chr-arm64.raw")
        check(staging.canonicalFile.parentFile == canonicalDirectory) {
            "Unsafe staging path."
        }

        val digest = MessageDigest.getInstance("SHA-256")
        var total = 0L
        try {
            val input = appContext.contentResolver.openInputStream(uri)
                ?: error("The selected document could not be opened.")
            input.use { source ->
                FileOutputStream(staging, false).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        total += count
                        check(total <= maxImageBytes) {
                            "Image exceeds the 1.5 GiB import safety limit."
                        }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                    output.fd.sync()
                }
            }

            check(total > 0L) { "The selected image is empty." }
            val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
            check(actual == expected) {
                "SHA-256 mismatch. The image was rejected and the staged copy will be removed."
            }

            if (destination.exists()) {
                check(destination.delete()) { "Could not replace the previously staged image." }
            }
            check(staging.renameTo(destination)) { "Could not finalize the verified image." }
            return RouterOsImageImportResult(destination.canonicalPath, actual, total)
        } catch (error: Throwable) {
            staging.delete()
            throw error
        }
    }
}
