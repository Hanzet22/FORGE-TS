package com.HG.Forge.Ts.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.HG.Forge.Ts.data.network.RepositoryExplorerRepository
import com.HG.Forge.Ts.data.network.RepositorySearchResult
import kotlinx.coroutines.launch

class RepositoryExplorerViewModel : ViewModel() {
    private val repository = RepositoryExplorerRepository()

    var result: RepositorySearchResult? by mutableStateOf(null)
        private set
    var error: String? by mutableStateOf(null)
        private set
    var isLoading: Boolean by mutableStateOf(false)
        private set

    fun search(query: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            result = null
            try {
                result = repository.search(query)
            } catch (exception: Exception) {
                error = exception.message ?: "Repository search failed"
            } finally {
                isLoading = false
            }
        }
    }
}
