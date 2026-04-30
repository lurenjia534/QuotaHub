package com.lurenjia534.quotahub.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.preferences.RefreshCadence
import com.lurenjia534.quotahub.data.provider.SecretBundle
import com.lurenjia534.quotahub.data.provider.SubscriptionGateway
import com.lurenjia534.quotahub.sync.SubscriptionRefreshPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ProviderQuotaUiState(
    val subscription: Subscription,
    val detail: ProviderQuotaDetailUiModel = ProviderQuotaDetailUiModel(),
    val isBootstrapping: Boolean = true,
    val isLoading: Boolean = false,
    val isSavingTitle: Boolean = false,
    val isSavingCredentials: Boolean = false,
    val error: String? = null,
    val credentialError: String? = null,
    val isConnected: Boolean = true,
    val canRefresh: Boolean = true,
    val canUpdateCredentials: Boolean = true,
    val canRename: Boolean = true,
    val showRenameDialog: Boolean = false,
    val showCredentialDialog: Boolean = false,
    val titleInput: String = "",
    val credentialInputs: Map<String, String> = emptyMap()
)

class ProviderQuotaViewModel(
    private val subscriptionGateway: SubscriptionGateway,
    private val detailProjectorRegistry: ProviderQuotaDetailProjectorRegistry,
    private val refreshPolicy: SubscriptionRefreshPolicy,
    private val refreshCadence: RefreshCadence
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ProviderQuotaUiState(
            subscription = subscriptionGateway.subscription,
            canRefresh = subscriptionGateway.capabilities.canRefresh,
            canUpdateCredentials = subscriptionGateway.capabilities.canUpdateCredentials,
            canRename = subscriptionGateway.capabilities.canRename
        )
    )
    val uiState: StateFlow<ProviderQuotaUiState> = _uiState.asStateFlow()

    init {
        bootstrap()
        observeSnapshot()
    }

    private fun bootstrap() {
        viewModelScope.launch {
            val snapshot = subscriptionGateway.snapshot.first()

            _uiState.value = _uiState.value.copy(
                isBootstrapping = false,
                isConnected = snapshot.subscription.syncStatus.isConnected,
                error = if (!snapshot.subscription.hasUsableCredentials) {
                    snapshot.subscription.credentialIssue ?: snapshot.subscription.syncStatus.lastError
                } else {
                    null
                },
                    detail = detailProjectorRegistry.project(
                        subscription = snapshot.subscription,
                        snapshot = snapshot.quotaSnapshot
                    ),
                    canRefresh = subscriptionGateway.capabilities.canRefresh,
                    canUpdateCredentials = subscriptionGateway.capabilities.canUpdateCredentials,
                    canRename = subscriptionGateway.capabilities.canRename
                )

            if (subscriptionGateway.capabilities.canRefresh &&
                refreshPolicy.shouldAutoRefreshOnDetailOpen(
                    subscription = snapshot.subscription,
                    refreshCadence = refreshCadence
                )
            ) {
                refresh()
            }
        }
    }

    private fun observeSnapshot() {
        viewModelScope.launch {
            subscriptionGateway.snapshot.collect { snapshot ->
                _uiState.value = _uiState.value.copy(
                    subscription = snapshot.subscription,
                    isConnected = snapshot.subscription.syncStatus.isConnected,
                    error = if (_uiState.value.error == null && !snapshot.subscription.hasUsableCredentials) {
                        snapshot.subscription.credentialIssue ?: snapshot.subscription.syncStatus.lastError
                    } else {
                        _uiState.value.error
                    },
                    detail = detailProjectorRegistry.project(
                        subscription = snapshot.subscription,
                        snapshot = snapshot.quotaSnapshot
                    ),
                    canRefresh = subscriptionGateway.capabilities.canRefresh,
                    canUpdateCredentials = subscriptionGateway.capabilities.canUpdateCredentials,
                    canRename = subscriptionGateway.capabilities.canRename,
                    titleInput = if (_uiState.value.showRenameDialog) {
                        _uiState.value.titleInput
                    } else {
                        snapshot.subscription.customTitle.orEmpty()
                    },
                    credentialInputs = if (_uiState.value.showCredentialDialog) {
                        _uiState.value.credentialInputs
                    } else {
                        snapshot.subscription.supportedProvider?.credentialFields
                            ?.associate { it.key to "" }
                            ?: emptyMap()
                    }
                )
            }
        }
    }

    fun showRenameDialog() {
        if (!_uiState.value.canRename) {
            return
        }
        _uiState.value = _uiState.value.copy(
            showRenameDialog = true,
            titleInput = _uiState.value.subscription.customTitle.orEmpty(),
            error = null
        )
    }

    fun hideRenameDialog() {
        _uiState.value = _uiState.value.copy(
            showRenameDialog = false,
            titleInput = _uiState.value.subscription.customTitle.orEmpty()
        )
    }

    fun showCredentialDialog() {
        val provider = _uiState.value.subscription.supportedProvider ?: return
        if (!_uiState.value.canUpdateCredentials) {
            return
        }
        _uiState.value = _uiState.value.copy(
            showCredentialDialog = true,
            credentialInputs = provider.credentialFields.associate { it.key to "" },
            credentialError = null,
            error = null
        )
    }

    fun hideCredentialDialog() {
        val provider = _uiState.value.subscription.supportedProvider
        _uiState.value = _uiState.value.copy(
            showCredentialDialog = false,
            credentialInputs = provider?.credentialFields?.associate { it.key to "" } ?: emptyMap(),
            credentialError = null
        )
    }

    fun updateTitleInput(title: String) {
        _uiState.value = _uiState.value.copy(titleInput = title)
    }

    fun updateCredentialInput(fieldKey: String, value: String) {
        _uiState.value = _uiState.value.copy(
            credentialInputs = _uiState.value.credentialInputs + (fieldKey to value)
        )
    }

    fun renameSubscription() {
        if (!_uiState.value.canRename) {
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingTitle = true, error = null)
            subscriptionGateway.rename(_uiState.value.titleInput).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isSavingTitle = false,
                        showRenameDialog = false
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSavingTitle = false,
                        error = error.message ?: "Unable to update subscription name"
                    )
                }
            )
        }
    }

    fun saveCredentials() {
        val provider = _uiState.value.subscription.supportedProvider ?: return
        if (!_uiState.value.canUpdateCredentials) {
            return
        }
        val missingField = provider.credentialFields.firstOrNull { field ->
            field.isRequired && _uiState.value.credentialInputs[field.key].isNullOrBlank()
        }
        if (missingField != null) {
            _uiState.value = _uiState.value.copy(
                credentialError = "${missingField.label} is required"
            )
            return
        }

        val credentials = SecretBundle.of(_uiState.value.credentialInputs)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSavingCredentials = true,
                credentialError = null,
                error = null
            )
            subscriptionGateway.updateCredentials(credentials).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isSavingCredentials = false,
                        showCredentialDialog = false,
                        error = null,
                        credentialError = null,
                        credentialInputs = provider.credentialFields.associate { it.key to "" }
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSavingCredentials = false,
                        credentialError = error.message ?: "Unable to update credentials"
                    )
                }
            )
        }
    }

    fun refresh() {
        if (!_uiState.value.canRefresh) {
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            subscriptionGateway.refresh().fold(
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

    suspend fun disconnect() {
        subscriptionGateway.disconnect()
    }

    class Factory(
        private val subscriptionGateway: SubscriptionGateway,
        private val detailProjectorRegistry: ProviderQuotaDetailProjectorRegistry,
        private val refreshPolicy: SubscriptionRefreshPolicy,
        private val refreshCadence: RefreshCadence
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProviderQuotaViewModel::class.java)) {
                return ProviderQuotaViewModel(
                    subscriptionGateway = subscriptionGateway,
                    detailProjectorRegistry = detailProjectorRegistry,
                    refreshPolicy = refreshPolicy,
                    refreshCadence = refreshCadence
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
