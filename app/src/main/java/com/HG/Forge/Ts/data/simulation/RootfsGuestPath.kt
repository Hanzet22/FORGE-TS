package com.HG.Forge.Ts.data.simulation

import java.io.File
import java.nio.file.Files

/**
 * Resolves Linux guest paths without letting Android's filesystem interpret
 * absolute guest symlinks as absolute host paths.
 */
internal object RootfsGuestPath {
    fun resolve(root: File, guestPath: String, maxLinks: Int = 40): File? {
        val rootDir = runCatching { root.canonicalFile }.getOrNull() ?: return null
        if (!rootDir.isDirectory || !guestPath.startsWith("/") || '\u0000' in guestPath) return null

        val pending = ArrayDeque<String>()
        guestPath.split('/').filter { it.isNotEmpty() }.forEach { pending.addLast(it) }
        var current = rootDir
        var linksFollowed = 0

        while (pending.isNotEmpty()) {
            val segment = pending.removeFirst()
            when (segment) {
                ".", "" -> Unit
                ".." -> {
                    if (current == rootDir) return null
                    current = current.parentFile ?: return null
                }
                else -> {
                    val next = File(current, segment)
                    if (!isLexicallyContained(next, rootDir)) return null
                    if (Files.isSymbolicLink(next.toPath())) {
                        if (++linksFollowed > maxLinks) return null
                        val target = runCatching { Files.readSymbolicLink(next.toPath()) }.getOrNull() ?: return null
                        val targetText = target.toString()
                        if (targetText.isBlank() || '\u0000' in targetText || '\\' in targetText) return null
                        if (target.isAbsolute) current = rootDir
                        target.toString().split('/').filter { it.isNotEmpty() }.asReversed()
                            .forEach { pending.addFirst(it) }
                    } else {
                        current = next
                    }
                }
            }
        }

        val resolved = runCatching { current.canonicalFile }.getOrNull() ?: return null
        if (!isContained(resolved, rootDir)) return null
        return resolved
    }

    fun isExecutableGuestFile(root: File, guestPath: String): Boolean {
        val file = resolve(root, guestPath) ?: return false
        return file.isFile && file.canRead() && file.canExecute()
    }

    private fun isLexicallyContained(file: File, root: File): Boolean {
        val path = file.toPath().toAbsolutePath().normalize().toString()
        return path == root.path || path.startsWith(root.path + File.separator)
    }

    private fun isContained(file: File, root: File): Boolean {
        return file.path == root.path || file.path.startsWith(root.path + File.separator)
    }
}
