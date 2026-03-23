package com.lurenjia534.quotahub.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lurenjia534.quotahub.data.model.ModelRemain
import com.lurenjia534.quotahub.data.repository.QuotaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val modelRemains: List<ModelRemain> = emptyList(),
    val error: String? = null,
    val showApiKeyDialog: Boolean = false,
    val apiKey: String = "",
    val hasApiKey: Boolean = false
)

class HomeViewModel(private val repository: QuotaRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadSavedApiKey()
    }

    private fun loadSavedApiKey() {
        viewModelScope.launch {
            repository.apiKey.collect { entity ->
                if (entity != null) {
                    _uiState.value = _uiState.value.copy(
                        apiKey = entity.key,
                        hasApiKey = true
                    )
                    fetchModelRemains()
                }
            }
        }
    }

    fun showApiKeyDialog() {
        _uiState.value = _uiState.value.copy(showApiKeyDialog = true)
    }

    fun hideApiKeyDialog() {
        _uiState.value = _uiState.value.copy(showApiKeyDialog = false)
    }

    fun updateApiKey(key: String) {
        _uiState.value = _uiState.value.copy(apiKey = key)
    }

    fun fetchModelRemains() {
        val apiKey = _uiState.value.apiKey
        if (apiKey.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "API key is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.getModelRemains("Bearer ${apiKey.trim()}")
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        modelRemains = response.modelRemains,
                        showApiKeyDialog = false,
                        hasApiKey = true
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error"
                    )
                }
            )
        }
    }

    fun saveApiKeyAndFetch() {
        val apiKey = _uiState.value.apiKey
        if (apiKey.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "API key is required")
            return
        }

        viewModelScope.launch {
            repository.saveApiKey(apiKey.trim())
            fetchModelRemains()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    class Factory(private val repository: QuotaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}