package com.lurenjia534.quotahub.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.provider.SubscriptionGateway
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
    val error: String? = null,
    val isConnected: Boolean = true,
    val showRenameDialog: Boolean = false,
    val titleInput: String = ""
)

class ProviderQuotaViewModel(
    private val subscriptionGateway: SubscriptionGateway,
    private val detailProjectorRegistry: ProviderQuotaDetailProjectorRegistry
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ProviderQuotaUiState(subscription = subscriptionGateway.subscription)
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
                detail = detailProjectorRegistry.project(
                    subscription = snapshot.subscription,
                    snapshot = snapshot.quotaSnapshot
                )
            )

            refresh()
        }
    }

    private fun observeSnapshot() {
        viewModelScope.launch {
            subscriptionGateway.snapshot.collect { snapshot ->
                _uiState.value = _uiState.value.copy(
                    subscription = snapshot.subscription,
                    isConnected = snapshot.subscription.syncStatus.isConnected,
                    detail = detailProjectorRegistry.project(
                        subscription = snapshot.subscription,
                        snapshot = snapshot.quotaSnapshot
                    ),
                    titleInput = if (_uiState.value.showRenameDialog) {
                        _uiState.value.titleInput
                    } else {
                        snapshot.subscription.customTitle.orEmpty()
                    }
                )
            }
        }
    }

    fun showRenameDialog() {
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

    fun updateTitleInput(title: String) {
        _uiState.value = _uiState.value.copy(titleInput = title)
    }

    fun renameSubscription() {
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

    fun refresh() {
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
        private val detailProjectorRegistry: ProviderQuotaDetailProjectorRegistry
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProviderQuotaViewModel::class.java)) {
                return ProviderQuotaViewModel(
                    subscriptionGateway = subscriptionGateway,
                    detailProjectorRegistry = detailProjectorRegistry
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
