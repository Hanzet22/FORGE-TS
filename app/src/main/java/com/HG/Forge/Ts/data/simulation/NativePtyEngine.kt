package com.HG.Forge.Ts.data.simulation

/**
 * Low-level Android Bionic PTY bridge.
 *
 * This facade is intentionally not selected by ProotExecutionAdapter yet.
 * The pipe-backed adapter remains the fallback until PTY integration is verified.
 */
internal object NativePtyEngine {
    val available: Boolean by lazy {
        runCatching { System.loadLibrary("forge_pty") }.isSuccess
    }

    external fun nativeStart(
        argv: Array<String>,
        environment: Array<String>,
        workingDirectory: String,
        rows: Int,
        columns: Int
    ): Long

    external fun nativeRead(handle: Long, destination: ByteArray, timeoutMillis: Int): Int
    external fun nativeWrite(handle: Long, source: ByteArray, offset: Int, length: Int): Int
    external fun nativeResize(handle: Long, rows: Int, columns: Int): Boolean
    external fun nativeWait(handle: Long): Int
    external fun nativeSignal(handle: Long, signal: Int): Boolean
    external fun nativeClose(handle: Long, graceful: Boolean): Boolean
}
