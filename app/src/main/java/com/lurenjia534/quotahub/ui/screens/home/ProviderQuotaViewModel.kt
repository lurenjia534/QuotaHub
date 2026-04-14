package com.lurenjia534.quotahub.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lurenjia534.quotahub.data.model.ModelRemain
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.provider.SubscriptionGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ProviderQuotaUiState(
    val subscription: Subscription,
    val isBootstrapping: Boolean = true,
    val isLoading: Boolean = false,
    val modelRemains: List<ModelRemain> = emptyList(),
    val error: String? = null,
    val isConnected: Boolean = true
)

class ProviderQuotaViewModel(
    private val subscriptionGateway: SubscriptionGateway
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
                isConnected = true,
                modelRemains = snapshot.modelRemains
            )

            refresh()
        }
    }

    private fun observeSnapshot() {
        viewModelScope.launch {
            subscriptionGateway.snapshot.collect { snapshot ->
                _uiState.value = _uiState.value.copy(
                    subscription = snapshot.subscription,
                    modelRemains = snapshot.modelRemains
                )
            }
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
        private val subscriptionGateway: SubscriptionGateway
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProviderQuotaViewModel::class.java)) {
                return ProviderQuotaViewModel(subscriptionGateway) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}