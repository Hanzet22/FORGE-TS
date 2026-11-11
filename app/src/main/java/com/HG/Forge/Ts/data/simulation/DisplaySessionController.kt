package com.HG.Forge.Ts.data.simulation

import android.view.Surface
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class DisplaySessionLifecycle {
    IDLE, INSPECTING, STARTING, RUNNING, STOPPING, UNAVAILABLE, FAILED
}

data class DisplaySessionSnapshot(
    val lifecycle: DisplaySessionLifecycle,
    val backendId: String? = null,
    val environmentId: RuntimeEnvironmentId? = null,
    val surfaceAttached: Boolean = false,
    val displayName: String? = null,
    val detail: String
)

data class DisplaySessionInspection(
    val capability: DisplayBackendCapability?,
    val detail: String
)

/**
 * Host-side coordinator for the shared display service.
 *
 * It deliberately does not create an X server or claim that a display backend
 * exists. A concrete, reviewed DisplaySessionAdapter must be registered before
 * a session can start. The host owns Android Surface lifecycle; stop releases
 * the adapter's session and any native Surface reference it retained.
 */
class DisplaySessionController(
    private val registry: DisplaySessionAdapterRegistry = DisplaySessionAdapterRegistry.empty()
) {
    private val mutex = Mutex()
    private var lifecycle = DisplaySessionLifecycle.IDLE
    private var activeAdapter: DisplaySessionAdapter? = null
    private var activeRequest: DisplaySessionRequest? = null
    private var surfaceAttached = false
    private var displayName: String? = null
    private var detail = "No display session is active."

    suspend fun inspect(
        request: DisplaySessionRequest,
        backendId: String? = null
    ): DisplaySessionInspection = mutex.withLock {
        lifecycle = DisplaySessionLifecycle.INSPECTING
        val adapter = backendId?.let(registry::find)
            ?: if (backendId == null) registry.all().firstOrNull() else null

        if (adapter == null) {
            lifecycle = DisplaySessionLifecycle.UNAVAILABLE
            detail = if (backendId == null) {
                "No embedded display backend is registered. Runtime and display availability are separate."
            } else {
                "Display backend '$backendId' is not registered."
            }
            return@withLock DisplaySessionInspection(null, detail)
        }

        val result = runCatching { adapter.inspect(request) }.getOrElse { error ->
            lifecycle = DisplaySessionLifecycle.FAILED
            detail = "Display capability inspection failed: ${error.message ?: error.javaClass.simpleName}"
            return@withLock DisplaySessionInspection(
                DisplayBackendCapability(false, adapter.backendId, 0, detail),
                detail
            )
        }

        lifecycle = if (result.available) DisplaySessionLifecycle.IDLE else DisplaySessionLifecycle.UNAVAILABLE
        detail = result.detail
        DisplaySessionInspection(result, result.detail)
    }

    suspend fun start(
        request: DisplaySessionRequest,
        backendId: String? = null
    ): DisplaySessionSnapshot = mutex.withLock {
        if (activeAdapter != null) {
            detail = "A display session is already active. Stop it before starting another session."
            return@withLock snapshotUnsafe()
        }

        val adapter = backendId?.let(registry::find)
            ?: if (backendId == null) registry.all().firstOrNull() else null

        if (adapter == null) {
            lifecycle = DisplaySessionLifecycle.UNAVAILABLE
            detail = "No requested embedded display backend is registered."
            return@withLock snapshotUnsafe()
        }

        lifecycle = DisplaySessionLifecycle.INSPECTING
        val capability = runCatching { adapter.inspect(request) }.getOrElse { error ->
            lifecycle = DisplaySessionLifecycle.FAILED
            detail = "Display capability inspection failed: ${error.message ?: error.javaClass.simpleName}"
            return@withLock snapshotUnsafe()
        }
        if (!capability.available) {
            lifecycle = DisplaySessionLifecycle.UNAVAILABLE
            detail = capability.detail
            return@withLock snapshotUnsafe()
        }

        lifecycle = DisplaySessionLifecycle.STARTING
        val result = runCatching { adapter.start(request) }.getOrElse { error ->
            lifecycle = DisplaySessionLifecycle.FAILED
            detail = "Display start failed: ${error.message ?: error.javaClass.simpleName}"
            return@withLock snapshotUnsafe()
        }

        if (!result.accepted || !result.ready) {
            // An accepted-but-not-ready start is not a running session. Ask the
            // backend to clean up any partial state before reporting failure.
            runCatching { adapter.stop() }
            lifecycle = DisplaySessionLifecycle.FAILED
            detail = result.message.ifBlank { "Backend did not confirm display readiness." }
            return@withLock snapshotUnsafe()
        }

        activeAdapter = adapter
        activeRequest = request
        surfaceAttached = false
        displayName = result.displayName
        lifecycle = DisplaySessionLifecycle.RUNNING
        detail = result.message
        snapshotUnsafe()
    }

    suspend fun attachSurface(
        surface: Surface,
        width: Int,
        height: Int
    ): DisplaySessionSnapshot = mutex.withLock {
        if (width !in 1..8192 || height !in 1..8192) {
            detail = "Surface dimensions must be between 1 and 8192 pixels."
            return@withLock snapshotUnsafe()
        }
        val adapter = activeAdapter
        if (adapter == null || lifecycle != DisplaySessionLifecycle.RUNNING) {
            detail = "No confirmed running display session can accept a Surface."
            return@withLock snapshotUnsafe()
        }
        val result = runCatching { adapter.attachSurface(surface, width, height) }.getOrElse { error ->
            detail = "Surface attachment failed: ${error.message ?: error.javaClass.simpleName}"
            return@withLock snapshotUnsafe()
        }
        surfaceAttached = result.completed
        detail = result.message
        snapshotUnsafe()
    }

    suspend fun sendInput(event: DisplayInputEvent): DisplayOperationResult = mutex.withLock {
        val adapter = activeAdapter
        if (adapter == null || lifecycle != DisplaySessionLifecycle.RUNNING) {
            return@withLock DisplayOperationResult(false, "No confirmed running display session.")
        }
        runCatching { adapter.sendInput(event) }.getOrElse { error ->
            DisplayOperationResult(false, "Display input failed: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    suspend fun resize(width: Int, height: Int, densityDpi: Int): DisplayOperationResult = mutex.withLock {
        if (width !in 1..8192 || height !in 1..8192 || densityDpi !in 24..1536) {
            return@withLock DisplayOperationResult(false, "Resize dimensions or density are outside supported bounds.")
        }
        val adapter = activeAdapter
        if (adapter == null || lifecycle != DisplaySessionLifecycle.RUNNING) {
            return@withLock DisplayOperationResult(false, "No confirmed running display session.")
        }
        runCatching { adapter.resize(width, height, densityDpi) }.getOrElse { error ->
            DisplayOperationResult(false, "Display resize failed: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    suspend fun stop(): DisplaySessionSnapshot = mutex.withLock {
        val adapter = activeAdapter
        if (adapter == null) {
            lifecycle = DisplaySessionLifecycle.IDLE
            surfaceAttached = false
            displayName = null
            detail = "No display session was active."
            return@withLock snapshotUnsafe()
        }

        lifecycle = DisplaySessionLifecycle.STOPPING
        val result = runCatching { adapter.stop() }.getOrElse { error ->
            lifecycle = DisplaySessionLifecycle.FAILED
            detail = "Display stop failed: ${error.message ?: error.javaClass.simpleName}"
            return@withLock snapshotUnsafe()
        }

        if (result.completed) {
            activeAdapter = null
            activeRequest = null
            surfaceAttached = false
            displayName = null
            lifecycle = DisplaySessionLifecycle.IDLE
        } else {
            lifecycle = DisplaySessionLifecycle.FAILED
        }
        detail = result.message
        snapshotUnsafe()
    }

    suspend fun snapshot(): DisplaySessionSnapshot = mutex.withLock { snapshotUnsafe() }

    private fun snapshotUnsafe() = DisplaySessionSnapshot(
        lifecycle = lifecycle,
        backendId = activeAdapter?.backendId,
        environmentId = activeRequest?.environmentId,
        surfaceAttached = surfaceAttached,
        displayName = displayName,
        detail = detail
    )
}
