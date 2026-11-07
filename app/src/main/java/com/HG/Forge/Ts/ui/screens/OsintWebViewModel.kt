package com.HG.Forge.Ts.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.HG.Forge.Ts.data.network.DnsLookupRepository
import com.HG.Forge.Ts.data.network.NativeWebCheckRepository
import com.HG.Forge.Ts.data.network.NativeWebReport
import com.HG.Forge.Ts.data.network.DnsLookupResult
import com.HG.Forge.Ts.data.network.DnsResolver
import kotlinx.coroutines.launch

data class OsintDnsCheck(
    val recordType: String,
    val result: DnsLookupResult? = null,
    val error: String? = null
)

class OsintWebViewModel : ViewModel() {
    private val repository = DnsLookupRepository()
    private val nativeRepository = NativeWebCheckRepository()
    private val recordTypes = listOf("A", "AAAA", "NS", "MX", "TXT", "CAA")

    var checks by mutableStateOf<List<OsintDnsCheck>>(emptyList())
        private set
    var target by mutableStateOf("")
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var nativeReport by mutableStateOf<NativeWebReport?>(null)
        private set
    var nativeError by mutableStateOf<String?>(null)
        private set

    fun inspect(input: String) {
        val domain = input.trim()
        if (domain.isBlank()) {
            error = "Enter a fully qualified domain name."
            return
        }
        viewModelScope.launch {
            isLoading = true
            error = null
            checks = emptyList()
            nativeReport = null
            nativeError = null
            target = domain
            val collected = mutableListOf<OsintDnsCheck>()
            for (type in recordTypes) {
                val check = try {
                    OsintDnsCheck(
                        recordType = type,
                        result = repository.lookup(domain, type, DnsResolver.GOOGLE)
                    )
                } catch (exception: Exception) {
                    OsintDnsCheck(
                        recordType = type,
                        error = exception.message ?: "Lookup failed"
                    )
                }
                collected += check
                checks = collected.toList()
            }
            try {
                nativeReport = nativeRepository.inspect(domain)
            } catch (exception: Exception) {
                nativeError = exception.message ?: "Native website inspection failed."
            }
            if (collected.all { it.result == null } && nativeReport == null) {
                error = "No DNS checks completed successfully. Review the domain and network connection."
            }
            isLoading = false
        }
    }
}
