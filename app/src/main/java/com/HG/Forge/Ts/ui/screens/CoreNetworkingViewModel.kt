package com.HG.Forge.Ts.ui.screens

import android.content.Context
import android.os.SystemClock
import com.HG.Forge.Ts.data.sync.ForgeDiagnostics
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.HG.Forge.Ts.data.network.CoreNetworkingRepository
import com.HG.Forge.Ts.data.network.CidrCalculator
import com.HG.Forge.Ts.data.network.CidrResult
import com.HG.Forge.Ts.data.network.HostCheckResult
import com.HG.Forge.Ts.data.network.PublicIpResult
import kotlinx.coroutines.launch

class CoreNetworkingViewModel : ViewModel() {
    private val repository = CoreNetworkingRepository()

    var cidrResult: CidrResult? by mutableStateOf(null)
        private set
    var cidrError: String? by mutableStateOf(null)
        private set
    var publicIp: PublicIpResult? by mutableStateOf(null)
        private set
    var publicIpError: String? by mutableStateOf(null)
        private set
    var isLoadingIp: Boolean by mutableStateOf(false)
        private set
    var hostResult: HostCheckResult? by mutableStateOf(null)
        private set
    var hostError: String? by mutableStateOf(null)
        private set
    var isCheckingHost: Boolean by mutableStateOf(false)
        private set

    fun calculateCidr(input: String) {
        try {
            cidrResult = CidrCalculator.calculate(input)
            cidrError = null
        } catch (error: Exception) {
            cidrResult = null
            cidrError = error.message ?: "Unable to calculate this network"
        }
    }

    fun loadPublicIp(context: Context) {
        viewModelScope.launch {
            isLoadingIp = true
            publicIpError = null
            val traceId = ForgeDiagnostics.newTraceId()
            val started = SystemClock.elapsedRealtime()
            ForgeDiagnostics.log(context, "INFO", "Network", "network.public_ip.started", traceId = traceId)
            try {
                publicIp = repository.fetchPublicIp()
                ForgeDiagnostics.log(context, "INFO", "Network", "network.public_ip.succeeded", traceId = traceId)
            } catch (error: Exception) {
                publicIpError = error.message ?: "Unable to retrieve public IP"
                ForgeDiagnostics.log(context, "WARNING", "Network", "network.public_ip.failed", traceId = traceId)
            } finally {
                val elapsed = SystemClock.elapsedRealtime() - started
                ForgeDiagnostics.log(context, "INFO", "Network", "network.public_ip.duration_${elapsed.coerceAtMost(600000)}ms", traceId = traceId)
                isLoadingIp = false
            }
        }
    }

    fun checkHost(context: Context, host: String, port: String, tls: Boolean) {
        val parsedPort = port.toIntOrNull()
        if (parsedPort == null) {
            hostError = "Enter a valid port number"
            return
        }
        viewModelScope.launch {
            isCheckingHost = true
            hostError = null
            hostResult = null
            val traceId = ForgeDiagnostics.newTraceId()
            val started = SystemClock.elapsedRealtime()
            ForgeDiagnostics.log(context, "INFO", "Network", "network.host_check.started", traceId = traceId)
            try {
                hostResult = repository.checkHost(host, parsedPort, tls)
                ForgeDiagnostics.log(context, "INFO", "Network", "network.host_check.succeeded", traceId = traceId)
            } catch (error: Exception) {
                hostError = error.message ?: "Host check failed"
                ForgeDiagnostics.log(context, "WARNING", "Network", "network.host_check.failed", traceId = traceId)
            } finally {
                val elapsed = SystemClock.elapsedRealtime() - started
                ForgeDiagnostics.log(context, "INFO", "Network", "network.host_check.duration_${elapsed.coerceAtMost(600000)}ms", traceId = traceId)
                isCheckingHost = false
            }
        }
    }
}
