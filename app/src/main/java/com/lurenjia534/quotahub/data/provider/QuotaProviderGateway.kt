package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.data.model.ModelRemain
import com.lurenjia534.quotahub.ui.screens.home.QuotaProvider
import kotlinx.coroutines.flow.Flow

data class QuotaProviderSnapshot(
    val provider: QuotaProvider,
    val credential: String? = null,
    val modelRemains: List<ModelRemain> = emptyList()
) {
    val isConnected: Boolean
        get() = !credential.isNullOrBlank()
}

interface QuotaProviderGateway {
    val provider: QuotaProvider
    val snapshot: Flow<QuotaProviderSnapshot>

    suspend fun connect(credential: String): Result<Unit>

    suspend fun refresh(): Result<Unit>

    suspend fun disconnect()
}
