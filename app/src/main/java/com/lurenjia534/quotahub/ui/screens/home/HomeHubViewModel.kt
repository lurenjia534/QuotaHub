package com.lurenjia534.quotahub.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.SyncState
import com.lurenjia534.quotahub.data.provider.CardMetric
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor
import com.lurenjia534.quotahub.data.provider.SecretBundle
import com.lurenjia534.quotahub.data.provider.SubscriptionCard
import com.lurenjia534.quotahub.data.provider.SubscriptionRegistry
import com.lurenjia534.quotahub.ui.provider.ProviderUiRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SubscriptionCardUiModel(
    val subscriptionId: Long,
    val providerId: String,
    val displayTitle: String,
    val subtitle: String,
    val providerIconRes: Int,
    val primaryMetric: CardMetric,
    val secondaryMetric: CardMetric?,
    val resourceCount: Int,
    val nextResetAt: Long?,
    val risk: QuotaRisk,
    val syncState: SyncState,
    val syncLabel: String,
    val syncDescription: String,
    val isConnected: Boolean
)

data class HomeHubUiState(
    val isBootstrapping: Boolean = true,
    val isSaving: Boolean = false,
    val subscriptionCards: List<SubscriptionCardUiModel> = emptyList(),
    val selectedProvider: ProviderDescriptor? = null,
    val customTitleInput: String = "",
    val credentialInputs: Map<String, String> = emptyMap(),
    val showCredentialDialog: Boolean = false,
    val error: String? = null
)

class HomeHubViewModel(
    private val subscriptionRegistry: SubscriptionRegistry,
    private val providerUiRegistry: ProviderUiRegistry
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

    fun showCredentialDialog(provider: ProviderDescriptor) {
        _uiState.value = _uiState.value.copy(
            selectedProvider = provider,
            customTitleInput = "",
            credentialInputs = provider.credentialFields.associate { it.key to "" },
            showCredentialDialog = true,
            error = null
        )
    }

    fun hideCredentialDialog() {
        _uiState.value = _uiState.value.copy(showCredentialDialog = false)
    }

    fun updateCredentialInput(fieldKey: String, credential: String) {
        _uiState.value = _uiState.value.copy(
            credentialInputs = _uiState.value.credentialInputs + (fieldKey to credential)
        )
    }

    fun updateCustomTitleInput(title: String) {
        _uiState.value = _uiState.value.copy(customTitleInput = title)
    }

    fun saveSelectedProviderCredential() {
        val provider = _uiState.value.selectedProvider ?: return
        val missingField = provider.credentialFields.firstOrNull { field ->
            _uiState.value.credentialInputs[field.key].isNullOrBlank()
        }
        if (missingField != null) {
            _uiState.value = _uiState.value.copy(error = "${missingField.label} is required")
            return
        }

        val credentials = SecretBundle.of(_uiState.value.credentialInputs)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            subscriptionRegistry.validateAndCreateSubscription(
                provider = provider,
                customTitle = _uiState.value.customTitleInput.takeIf { it.isNotBlank() },
                credentials = credentials
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
                        error = error.message ?: "Invalid credentials"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun SubscriptionCard.toCardUiModel(): SubscriptionCardUiModel {
        val providerUi = providerUiRegistry.require(subscription.provider)
        return SubscriptionCardUiModel(
            subscriptionId = subscription.id,
            providerId = subscription.provider.id,
            displayTitle = subscription.displayTitle,
            subtitle = "${subscription.provider.displayName} • ${providerUi.subtitle}",
            providerIconRes = providerUi.iconRes,
            primaryMetric = primaryMetric,
            secondaryMetric = secondaryMetric,
            resourceCount = resourceCount,
            nextResetAt = nextResetAt,
            risk = risk,
            syncState = subscription.syncStatus.state,
            syncLabel = subscription.syncStatus.label(),
            syncDescription = subscription.syncStatus.description(),
            isConnected = subscription.syncStatus.isConnected
        )
    }

    class Factory(
        private val subscriptionRegistry: SubscriptionRegistry,
        private val providerUiRegistry: ProviderUiRegistry
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeHubViewModel::class.java)) {
                return HomeHubViewModel(
                    subscriptionRegistry = subscriptionRegistry,
                    providerUiRegistry = providerUiRegistry
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
