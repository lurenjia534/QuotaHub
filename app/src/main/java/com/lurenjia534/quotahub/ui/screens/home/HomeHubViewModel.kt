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

data class HomeHubUiState(
    val isBootstrapping: Boolean = true,
    val isSaving: Boolean = false,
    val hasMiniMaxProvider: Boolean = false,
    val minimaxModelRemains: List<ModelRemain> = emptyList(),
    val apiKey: String = "",
    val showApiKeyDialog: Boolean = false,
    val error: String? = null
)

class HomeHubViewModel(private val repository: QuotaRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeHubUiState())
    val uiState: StateFlow<HomeHubUiState> = _uiState.asStateFlow()

    init {
        bootstrap()
        observeApiKey()
        observeModelRemains()
    }

    private fun bootstrap() {
        viewModelScope.launch {
            val savedApiKey = repository.apiKey.first()
            val cachedModelRemains = repository.modelRemains.first()

            _uiState.value = _uiState.value.copy(
                isBootstrapping = false,
                hasMiniMaxProvider = savedApiKey != null,
                apiKey = savedApiKey?.key.orEmpty(),
                minimaxModelRemains = cachedModelRemains
            )
        }
    }

    private fun observeApiKey() {
        viewModelScope.launch {
            repository.apiKey.collect { entity ->
                _uiState.value = _uiState.value.copy(
                    hasMiniMaxProvider = entity != null,
                    apiKey = entity?.key.orEmpty()
                )
            }
        }
    }

    private fun observeModelRemains() {
        viewModelScope.launch {
            repository.modelRemains.collect { modelRemains ->
                _uiState.value = _uiState.value.copy(minimaxModelRemains = modelRemains)
            }
        }
    }

    fun showApiKeyDialog() {
        _uiState.value = _uiState.value.copy(showApiKeyDialog = true, error = null)
    }

    fun hideApiKeyDialog() {
        _uiState.value = _uiState.value.copy(showApiKeyDialog = false)
    }

    fun updateApiKey(key: String) {
        _uiState.value = _uiState.value.copy(apiKey = key)
    }

    fun saveMiniMaxProvider() {
        val apiKey = _uiState.value.apiKey.trim()
        if (apiKey.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "API key is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            repository.saveApiKey(apiKey)
            val result = repository.getModelRemains("Bearer $apiKey")
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        showApiKeyDialog = false,
                        hasMiniMaxProvider = true
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = error.message ?: "Unknown error"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    class Factory(private val repository: QuotaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeHubViewModel::class.java)) {
                return HomeHubViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
