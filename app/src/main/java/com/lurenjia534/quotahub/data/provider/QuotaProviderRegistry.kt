package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.ui.screens.home.QuotaProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class QuotaProviderRegistry(
    private val gateways: List<QuotaProviderGateway>
) {
    val providers: List<QuotaProvider>
        get() = gateways.map { it.provider }

    val snapshots: Flow<List<QuotaProviderSnapshot>> =
        combine(gateways.map { it.snapshot }) { snapshots ->
            snapshots.toList()
        }

    fun get(provider: QuotaProvider): QuotaProviderGateway? {
        return gateways.firstOrNull { it.provider == provider }
    }

    fun get(providerId: String): QuotaProviderGateway? {
        return gateways.firstOrNull { it.provider.id == providerId }
    }
}
