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
        observeSavedApiKey()
        observeCachedModelRemains()
        refreshSavedApiKey()
    }

    private fun observeSavedApiKey() {
        viewModelScope.launch {
            repository.apiKey.collect { entity ->
                _uiState.value = _uiState.value.copy(
                    apiKey = entity?.key.orEmpty(),
                    hasApiKey = entity != null
                )
            }
        }
    }

    private fun refreshSavedApiKey() {
        viewModelScope.launch {
            val savedApiKey = repository.apiKey.first()?.key ?: return@launch
            fetchModelRemains(savedApiKey)
        }
    }

    private fun observeCachedModelRemains() {
        viewModelScope.launch {
            repository.modelRemains.collect { modelRemains ->
                _uiState.value = _uiState.value.copy(modelRemains = modelRemains)
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
        fetchModelRemains(_uiState.value.apiKey)
    }

    private fun fetchModelRemains(apiKey: String) {
        if (apiKey.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "API key is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.getModelRemains("Bearer ${apiKey.trim()}")
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
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
