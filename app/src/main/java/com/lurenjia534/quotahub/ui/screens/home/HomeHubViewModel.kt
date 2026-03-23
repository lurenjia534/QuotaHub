package com.lurenjia534.quotahub.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lurenjia534.quotahub.data.provider.QuotaProviderRegistry
import com.lurenjia534.quotahub.data.provider.QuotaProviderSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ProviderCardUiModel(
    val provider: QuotaProvider,
    val isConnected: Boolean,
    val credential: String,
    val modelCount: Int,
    val remainingCalls: Int,
    val remainingTime: Long?
)

data class HomeHubUiState(
    val isBootstrapping: Boolean = true,
    val isSaving: Boolean = false,
    val providerCards: List<ProviderCardUiModel> = emptyList(),
    val selectedProvider: QuotaProvider? = null,
    val credentialInput: String = "",
    val showCredentialDialog: Boolean = false,
    val error: String? = null
)

class HomeHubViewModel(
    private val providerRegistry: QuotaProviderRegistry
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeHubUiState())
    val uiState: StateFlow<HomeHubUiState> = _uiState.asStateFlow()

    init {
        bootstrap()
        observeProviderSnapshots()
    }

    private fun bootstrap() {
        viewModelScope.launch {
            val snapshots = providerRegistry.snapshots.first()
            _uiState.value = _uiState.value.copy(
                isBootstrapping = false,
                providerCards = snapshots.map { it.toCardUiModel() }
            )
        }
    }

    private fun observeProviderSnapshots() {
        viewModelScope.launch {
            providerRegistry.snapshots.collect { snapshots ->
                val currentSelectedProvider = _uiState.value.selectedProvider
                val selectedSnapshot = snapshots.firstOrNull { it.provider == currentSelectedProvider }

                _uiState.value = _uiState.value.copy(
                    providerCards = snapshots.map { it.toCardUiModel() },
                    credentialInput = if (_uiState.value.showCredentialDialog) {
                        _uiState.value.credentialInput
                    } else {
                        selectedSnapshot?.credential.orEmpty()
                    }
                )
            }
        }
    }

    fun showCredentialDialog(provider: QuotaProvider) {
        val currentCredential = _uiState.value.providerCards
            .firstOrNull { it.provider == provider }
            ?.credential
            .orEmpty()

        _uiState.value = _uiState.value.copy(
            selectedProvider = provider,
            credentialInput = currentCredential,
            showCredentialDialog = true,
            error = null
        )
    }

    fun hideCredentialDialog() {
        _uiState.value = _uiState.value.copy(showCredentialDialog = false)
    }

    fun updateCredentialInput(credential: String) {
        _uiState.value = _uiState.value.copy(credentialInput = credential)
    }

    fun saveSelectedProviderCredential() {
        val provider = _uiState.value.selectedProvider ?: return
        val credential = _uiState.value.credentialInput.trim()
        if (credential.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "${provider.credentialLabel} is required")
            return
        }

        val gateway = providerRegistry.get(provider) ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            gateway.connect(credential).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        showCredentialDialog = false
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

    private fun QuotaProviderSnapshot.toCardUiModel(): ProviderCardUiModel {
        return ProviderCardUiModel(
            provider = provider,
            isConnected = isConnected,
            credential = credential.orEmpty(),
            modelCount = modelRemains.size,
            remainingCalls = modelRemains.sumOf { it.currentIntervalUsageCount },
            remainingTime = modelRemains.maxOfOrNull { it.remainsTime }
        )
    }

    class Factory(
        private val providerRegistry: QuotaProviderRegistry
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeHubViewModel::class.java)) {
                return HomeHubViewModel(providerRegistry) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
