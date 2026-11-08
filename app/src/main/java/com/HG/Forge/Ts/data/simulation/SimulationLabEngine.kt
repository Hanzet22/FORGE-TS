package com.HG.Forge.Ts.data.simulation

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

data class RedisLabValue(val value: String, val expiresAtMillis: Long? = null)

object RedisOfflineEngine {
    private val values = ConcurrentHashMap<String, RedisLabValue>()

    @Synchronized
    fun set(key: String, value: String, ttlSeconds: Long? = null): String {
        require(key.isNotBlank()) { "Key cannot be empty." }
        require(key.length <= 128) { "Key is limited to 128 characters." }
        require(value.length <= 4096) { "Value is limited to 4096 characters." }
        require(ttlSeconds == null || ttlSeconds in 1..86400) { "TTL must be between 1 and 86400 seconds." }
        val expiresAt = ttlSeconds?.let { System.currentTimeMillis() + it * 1000 }
        values[key] = RedisLabValue(value, expiresAt)
        return "OK"
    }

    @Synchronized
    fun get(key: String): String? {
        val entry = values[key] ?: return null
        if (entry.expiresAtMillis != null && entry.expiresAtMillis <= System.currentTimeMillis()) {
            values.remove(key)
            return null
        }
        return entry.value
    }

    @Synchronized
    fun delete(key: String): Boolean = values.remove(key) != null

    @Synchronized
    fun snapshot(): Map<String, String> {
        val now = System.currentTimeMillis()
        values.entries.removeIf { it.value.expiresAtMillis?.let { expiry -> expiry <= now } == true }
        return values.mapValues { it.value.value }.toSortedMap()
    }

    @Synchronized
    fun clearSession() {
        values.clear()
    }
}

data class SupabaseLabRecord(
    val id: Long,
    val label: String,
    val value: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

object SupabaseOfflineEngine {
    private val records = mutableListOf<SupabaseLabRecord>()
    private var nextId = 1L

    @Synchronized
    fun insert(label: String, value: String): SupabaseLabRecord {
        require(label.isNotBlank()) { "Record label cannot be empty." }
        require(label.length <= 80 && value.length <= 500) { "Record is too long." }
        return SupabaseLabRecord(nextId++, label.trim(), value.trim()).also(records::add)
    }

    @Synchronized
    fun update(id: Long, label: String, value: String): Boolean {
        val index = records.indexOfFirst { it.id == id }
        if (index < 0) return false
        records[index] = records[index].copy(label = label.trim(), value = value.trim())
        return true
    }

    @Synchronized
    fun delete(id: Long): Boolean = records.removeAll { it.id == id }

    @Synchronized
    fun select(): List<SupabaseLabRecord> = records.toList().sortedByDescending { it.id }

    @Synchronized
    fun clearSession() {
        records.clear()
        nextId = 1L
    }
}

/**
 * A deliberately narrow HTTP health endpoint bound only to IPv4 loopback.
 * It accepts one small request at a time and is intended only for the local lab.
 */
class LoopbackLabServer {
    @Volatile private var server: ServerSocket? = null
    private val running = AtomicBoolean(false)

    @Synchronized
    fun start(): Int {
        server?.let { if (!it.isClosed) return it.localPort }
        val socket = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        socket.soTimeout = 1000
        server = socket
        running.set(true)
        Thread({
            while (running.get()) {
                try {
                    socket.accept().use { client ->
                        client.soTimeout = 2000
                        val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.US_ASCII))
                        val requestLine = reader.readLine().orEmpty().take(1024)
                        var headerBytes = 0
                        while (true) {
                            val line = reader.readLine() ?: break
                            headerBytes += line.length
                            if (headerBytes > 8192 || line.isEmpty()) break
                        }
                        val body = if (requestLine.startsWith("GET /health ")) {
                            "{\"status\":\"healthy\",\"source\":\"FORGE-TS loopback\"}"
                        } else {
                            "{\"error\":\"Use GET /health\"}"
                        }
                        val status = if (requestLine.startsWith("GET /health ")) "200 OK" else "404 Not Found"
                        val bytes = body.toByteArray(StandardCharsets.UTF_8)
                        val response = "HTTP/1.1 $status\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
                        client.getOutputStream().apply {
                            write(response.toByteArray(StandardCharsets.US_ASCII))
                            write(bytes)
                            flush()
                        }
                    }
                } catch (_: java.net.SocketTimeoutException) {
                    // Periodically re-check the stop flag.
                } catch (_: java.net.SocketException) {
                    if (running.get()) running.set(false)
                } catch (_: Exception) {
                    // A malformed local request does not terminate the listener.
                }
            }
        }, "forge-loopback-lab").apply { isDaemon = true }.start()
        return socket.localPort
    }

    fun requestHealth(): String {
        val port = server?.takeIf { !it.isClosed }?.localPort
            ?: throw IllegalStateException("Start the local server first.")
        val started = System.nanoTime()
        Socket(InetAddress.getByName("127.0.0.1"), port).use { client ->
            client.soTimeout = 3000
            client.getOutputStream().write("GET /health HTTP/1.1\r\nHost: 127.0.0.1\r\nConnection: close\r\n\r\n".toByteArray(StandardCharsets.US_ASCII))
            val response = client.getInputStream().bufferedReader().readText().take(4096)
            val elapsedMs = (System.nanoTime() - started) / 1_000_000
            return "127.0.0.1:$port • ${elapsedMs} ms\n$response"
        }
    }

    @Synchronized
    fun stop() {
        running.set(false)
        try { server?.close() } catch (_: Exception) { }
        server = null
    }
}
