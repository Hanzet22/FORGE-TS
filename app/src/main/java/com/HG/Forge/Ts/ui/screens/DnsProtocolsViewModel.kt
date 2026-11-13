package com.HG.Forge.Ts.ui.screens

import android.content.Context
import android.os.SystemClock
import com.HG.Forge.Ts.data.sync.ForgeDiagnostics
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.HG.Forge.Ts.data.network.DnsLookupRepository
import com.HG.Forge.Ts.data.network.DnsLookupResult
import com.HG.Forge.Ts.data.network.DnsResolver
import kotlinx.coroutines.launch

class DnsProtocolsViewModel : ViewModel() {
    private val repository = DnsLookupRepository()

    var result: DnsLookupResult? by mutableStateOf(null)
        private set
    var error: String? by mutableStateOf(null)
        private set
    var isLoading: Boolean by mutableStateOf(false)
        private set

    fun lookup(context: Context, domain: String, recordType: String, resolver: DnsResolver) {
        viewModelScope.launch {
            isLoading = true
            error = null
            result = null
            val traceId = ForgeDiagnostics.newTraceId()
            val started = SystemClock.elapsedRealtime()
            ForgeDiagnostics.log(context, "INFO", "Dns", "dns.lookup.started", traceId = traceId)
            try {
                result = repository.lookup(domain, recordType, resolver)
                ForgeDiagnostics.log(context, "INFO", "Dns", "dns.lookup.succeeded", traceId = traceId)
            } catch (exception: Exception) {
                error = exception.message ?: "DNS lookup failed"
                ForgeDiagnostics.log(context, "WARNING", "Dns", "dns.lookup.failed", traceId = traceId)
            } finally {
                val elapsed = SystemClock.elapsedRealtime() - started
                ForgeDiagnostics.log(context, "INFO", "Dns", "dns.lookup.duration_${elapsed.coerceAtMost(600000)}ms", traceId = traceId)
                isLoading = false
            }
        }
    }
}
