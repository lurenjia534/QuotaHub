package com.lurenjia534.quotahub.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lurenjia534.quotahub.data.model.ModelRemain
import com.lurenjia534.quotahub.data.provider.QuotaProviderGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ProviderQuotaUiState(
    val provider: QuotaProvider,
    val isBootstrapping: Boolean = true,
    val isLoading: Boolean = false,
    val modelRemains: List<ModelRemain> = emptyList(),
    val error: String? = null,
    val showCredentialDialog: Boolean = false,
    val credentialInput: String = "",
    val isConnected: Boolean = false
)

class ProviderQuotaViewModel(
    private val providerGateway: QuotaProviderGateway
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ProviderQuotaUiState(provider = providerGateway.provider)
    )
    val uiState: StateFlow<ProviderQuotaUiState> = _uiState.asStateFlow()

    init {
        bootstrap()
        observeSnapshot()
    }

    private fun bootstrap() {
        viewModelScope.launch {
            val snapshot = providerGateway.snapshot.first()

            _uiState.value = _uiState.value.copy(
                isBootstrapping = false,
                credentialInput = snapshot.credential.orEmpty(),
                isConnected = snapshot.isConnected,
                modelRemains = snapshot.modelRemains
            )

            if (snapshot.isConnected) {
                refresh()
            }
        }
    }

    private fun observeSnapshot() {
        viewModelScope.launch {
            providerGateway.snapshot.collect { snapshot ->
                _uiState.value = _uiState.value.copy(
                    modelRemains = snapshot.modelRemains,
                    isConnected = snapshot.isConnected,
                    credentialInput = if (_uiState.value.showCredentialDialog) {
                        _uiState.value.credentialInput
                    } else {
                        snapshot.credential.orEmpty()
                    }
                )
            }
        }
    }

    fun showCredentialDialog() {
        _uiState.value = _uiState.value.copy(showCredentialDialog = true, error = null)
    }

    fun hideCredentialDialog() {
        _uiState.value = _uiState.value.copy(showCredentialDialog = false)
    }

    fun updateCredentialInput(credential: String) {
        _uiState.value = _uiState.value.copy(credentialInput = credential)
    }

    fun refresh() {
        if (!_uiState.value.isConnected) {
            _uiState.value = _uiState.value.copy(
                error = "${_uiState.value.provider.credentialLabel} is required"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            providerGateway.refresh().fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Unknown error"
                    )
                }
            )
        }
    }

    fun saveCredentialAndRefresh() {
        val credential = _uiState.value.credentialInput.trim()
        if (credential.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "${_uiState.value.provider.credentialLabel} is required"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            providerGateway.connect(credential).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showCredentialDialog = false,
                        isConnected = true
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Unknown error"
                    )
                }
            )
        }
    }

    class Factory(
        private val providerGateway: QuotaProviderGateway
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProviderQuotaViewModel::class.java)) {
                return ProviderQuotaViewModel(providerGateway) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
