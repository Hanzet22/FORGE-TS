package com.HG.Forge.Ts.data.simulation

import android.content.Context

/**
 * Contract between HybridRuntimeManager and an environment-specific runtime.
 * Implementations must only confirm RUNNING after independently verifying the session.
 */
data class RuntimeAdapterCapability(val available: Boolean, val detail: String)
data class RuntimeAdapterStartRequest(
    val environmentId: RuntimeEnvironmentId,
    val instanceId: String? = null,
    val workspacePath: String
)
data class RuntimeAdapterStartResult(
    val accepted: Boolean,
    val confirmedRunning: Boolean,
    val message: String
)
data class RuntimeAdapterStopResult(val stopped: Boolean, val message: String)
data class RuntimeCommandResult(
    val completed: Boolean,
    val exitCode: Int?,
    val output: String,
    val message: String
)
interface RuntimeExecutionAdapter {
    val environmentId: RuntimeEnvironmentId
    suspend fun inspect(request: RuntimeAdapterStartRequest): RuntimeAdapterCapability
    suspend fun start(request: RuntimeAdapterStartRequest): RuntimeAdapterStartResult
    suspend fun execute(request: RuntimeAdapterStartRequest, arguments: List<String>, timeoutSeconds: Long = 30): RuntimeCommandResult =
        RuntimeCommandResult(false, null, "", "This adapter does not expose command execution.")
    suspend fun stop(request: RuntimeAdapterStartRequest): RuntimeAdapterStopResult =
        RuntimeAdapterStopResult(false, "This adapter does not expose process shutdown.")
}
/** Empty by default: workspace preparation never implies an installed execution engine. */
class RuntimeExecutionAdapterRegistry(adapters: Collection<RuntimeExecutionAdapter> = emptyList()) {
    private val adaptersByEnvironment = adapters.associateBy { it.environmentId }
    fun find(environmentId: RuntimeEnvironmentId): RuntimeExecutionAdapter? = adaptersByEnvironment[environmentId]
    companion object {
        fun empty(): RuntimeExecutionAdapterRegistry = RuntimeExecutionAdapterRegistry()
        fun androidDefault(context: Context): RuntimeExecutionAdapterRegistry =
            RuntimeExecutionAdapterRegistry(listOf(ProotExecutionAdapter(context)))
    }
}
