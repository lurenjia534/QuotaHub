package com.lurenjia534.quotahub.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lurenjia534.quotahub.data.provider.SubscriptionCard
import com.lurenjia534.quotahub.data.provider.SubscriptionRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SubscriptionCardUiModel(
    val subscriptionId: Long,
    val displayTitle: String,
    val subtitle: String,
    val providerIconRes: Int,
    val modelCount: Int,
    val remainingCalls: Int,
    val remainingTime: Long?,
    val isConnected: Boolean
)

data class HomeHubUiState(
    val isBootstrapping: Boolean = true,
    val isSaving: Boolean = false,
    val subscriptionCards: List<SubscriptionCardUiModel> = emptyList(),
    val selectedProvider: QuotaProvider? = null,
    val customTitleInput: String = "",
    val credentialInput: String = "",
    val showCredentialDialog: Boolean = false,
    val error: String? = null
)

class HomeHubViewModel(
    private val subscriptionRegistry: SubscriptionRegistry
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeHubUiState())
    val uiState: StateFlow<HomeHubUiState> = _uiState.asStateFlow()

    init {
        bootstrap()
        observeSubscriptionSnapshots()
    }

    private fun bootstrap() {
        viewModelScope.launch {
            val snapshots = subscriptionRegistry.snapshots.first()
            _uiState.value = _uiState.value.copy(
                isBootstrapping = false,
                subscriptionCards = snapshots.map { it.toCardUiModel() }
            )
        }
    }

    private fun observeSubscriptionSnapshots() {
        viewModelScope.launch {
            subscriptionRegistry.snapshots.collect { snapshots ->
                _uiState.value = _uiState.value.copy(
                    subscriptionCards = snapshots.map { it.toCardUiModel() }
                )
            }
        }
    }

    fun showCredentialDialog(provider: QuotaProvider) {
        _uiState.value = _uiState.value.copy(
            selectedProvider = provider,
            customTitleInput = "",
            credentialInput = "",
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

    fun updateCustomTitleInput(title: String) {
        _uiState.value = _uiState.value.copy(customTitleInput = title)
    }

    fun saveSelectedProviderCredential() {
        val provider = _uiState.value.selectedProvider ?: return
        val credential = _uiState.value.credentialInput.trim()
        if (credential.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "${provider.credentialLabel} is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            subscriptionRegistry.validateAndCreateSubscription(
                provider = provider,
                customTitle = _uiState.value.customTitleInput.takeIf { it.isNotBlank() },
                apiKey = credential
            ).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        showCredentialDialog = false
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = error.message ?: "Invalid API key"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun SubscriptionCard.toCardUiModel(): SubscriptionCardUiModel {
        return SubscriptionCardUiModel(
            subscriptionId = subscription.id,
            displayTitle = subscription.displayTitle,
            subtitle = subscription.subtitle,
            providerIconRes = subscription.provider.iconRes,
            modelCount = modelCount,
            remainingCalls = remainingCalls,
            remainingTime = remainingTime,
            isConnected = isConnected
        )
    }

    class Factory(
        private val subscriptionRegistry: SubscriptionRegistry
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeHubViewModel::class.java)) {
                return HomeHubViewModel(subscriptionRegistry) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
