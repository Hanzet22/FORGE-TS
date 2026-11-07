package com.HG.Forge.Ts.ui.screens

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

    fun lookup(domain: String, recordType: String, resolver: DnsResolver) {
        viewModelScope.launch {
            isLoading = true
            error = null
            result = null
            try {
                result = repository.lookup(domain, recordType, resolver)
            } catch (exception: Exception) {
                error = exception.message ?: "DNS lookup failed"
            } finally {
                isLoading = false
            }
        }
    }
}
