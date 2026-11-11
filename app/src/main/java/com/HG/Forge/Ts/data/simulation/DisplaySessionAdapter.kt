package com.HG.Forge.Ts.data.simulation

import android.view.Surface

/**
 * Display is a shared service, not a runtime engine. A PRoot process may be
 * healthy while no compatible display backend is installed or available.
 */
data class DisplayBackendCapability(
    val available: Boolean,
    val backendId: String,
    val minimumAndroidApi: Int,
    val detail: String
)

data class DisplaySessionRequest(
    val environmentId: RuntimeEnvironmentId,
    val instanceId: String? = null,
    val width: Int,
    val height: Int,
    val densityDpi: Int
) {
    init {
        require(width in 1..8192) { "Display width must be between 1 and 8192 pixels." }
        require(height in 1..8192) { "Display height must be between 1 and 8192 pixels." }
        require(densityDpi in 24..1536) { "Display density must be between 24 and 1536 DPI." }
    }
}

data class DisplaySessionStartResult(
    val accepted: Boolean,
    val ready: Boolean,
    val displayName: String? = null,
    val message: String
)

data class DisplayOperationResult(val completed: Boolean, val message: String)

sealed interface DisplayInputEvent {
    data class Pointer(val x: Float, val y: Float, val button: Int = 0, val pressed: Boolean = false) : DisplayInputEvent {
        init {
            require(x.isFinite() && x in 0f..1f && y.isFinite() && y in 0f..1f)
            require(button in 0..9)
        }
    }

    data class Touch(val contactId: Int, val x: Float, val y: Float, val pressure: Float, val phase: Phase) : DisplayInputEvent {
        enum class Phase { BEGIN, UPDATE, END, CANCEL }
        init {
            require(contactId in 0..65535)
            require(x.isFinite() && x in 0f..1f && y.isFinite() && y in 0f..1f)
            require(pressure.isFinite() && pressure in 0f..1f)
        }
    }

    data class Scroll(val horizontal: Float, val vertical: Float) : DisplayInputEvent {
        init { require(horizontal.isFinite() && vertical.isFinite()) }
    }

    data class Key(val keyCode: Int, val pressed: Boolean) : DisplayInputEvent {
        init { require(keyCode in 0..65535) }
    }

    data class Text(val value: String) : DisplayInputEvent {
        init { require(value.isNotEmpty() && value.length <= 4096) }
    }
}

interface DisplaySessionAdapter {
    val backendId: String

    /** Capability must be based on real platform/backend checks, not Android version alone. */
    suspend fun inspect(request: DisplaySessionRequest): DisplayBackendCapability

    /** ready=true is valid only after the X server/display endpoint is confirmed ready. */
    suspend fun start(request: DisplaySessionRequest): DisplaySessionStartResult

    /** The host retains ownership of Surface; implementations retain any native reference they need. */
    suspend fun attachSurface(surface: Surface, width: Int, height: Int): DisplayOperationResult

    suspend fun sendInput(event: DisplayInputEvent): DisplayOperationResult

    suspend fun resize(width: Int, height: Int, densityDpi: Int): DisplayOperationResult

    suspend fun stop(): DisplayOperationResult
}

/** Empty until a reviewed, licensed and device-compatible embedded backend is integrated. */
class DisplaySessionAdapterRegistry(adapters: Collection<DisplaySessionAdapter> = emptyList()) {
    private val adaptersById = adapters.associateBy { it.backendId }

    init { require(adaptersById.size == adapters.size) { "Display backend identifiers must be unique." } }

    fun find(backendId: String): DisplaySessionAdapter? = adaptersById[backendId]
    fun all(): List<DisplaySessionAdapter> = adaptersById.values.toList()

    companion object {
        fun empty(): DisplaySessionAdapterRegistry = DisplaySessionAdapterRegistry()
    }
}
