package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.data.repository.SubscriptionRepository
import com.lurenjia534.quotahub.ui.screens.home.QuotaProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class SubscriptionCard(
    val subscription: com.lurenjia534.quotahub.data.model.Subscription,
    val modelCount: Int,
    val remainingCalls: Int,
    val remainingTime: Long?,
    val isConnected: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionRegistry(
    private val repository: SubscriptionRepository
) {
    val providers: List<QuotaProvider>
        get() = QuotaProvider.values().toList()

    val snapshots: Flow<List<SubscriptionCard>> = repository.subscriptions.flatMapLatest { subs ->
        if (subs.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(subs.map { sub ->
                repository.getModelRemains(sub.id).map { modelRemains ->
                    SubscriptionCard(
                        subscription = sub,
                        modelCount = modelRemains.size,
                        remainingCalls = modelRemains.sumOf { it.currentIntervalUsageCount },
                        remainingTime = modelRemains.map { it.remainsTime }.maxOrNull(),
                        isConnected = true
                    )
                }
            }) { it.toList() }
        }
    }

    fun getGateway(subscription: com.lurenjia534.quotahub.data.model.Subscription): SubscriptionGateway {
        return MiniMaxSubscriptionGateway(subscription, repository)
    }

    suspend fun getGatewayById(subscriptionId: Long): SubscriptionGateway? {
        val subscription = repository.getSubscriptionOnce(subscriptionId) ?: return null
        return getGateway(subscription)
    }

    suspend fun createSubscription(
        provider: QuotaProvider,
        customTitle: String?,
        apiKey: String
    ): Long {
        return repository.createSubscription(provider, customTitle, apiKey)
    }

    suspend fun validateAndCreateSubscription(
        provider: QuotaProvider,
        customTitle: String?,
        apiKey: String
    ): Result<Long> {
        return repository.validateApiKey(provider, apiKey).mapCatching { quotaResponse ->
            val subscriptionId = repository.createSubscription(provider, customTitle, apiKey)
            try {
                repository.cacheQuotaResponse(subscriptionId, quotaResponse)
                subscriptionId
            } catch (error: Exception) {
                repository.deleteSubscription(subscriptionId)
                throw error
            }
        }
    }

    suspend fun deleteSubscription(subscriptionId: Long) {
        repository.deleteSubscription(subscriptionId)
    }
}
