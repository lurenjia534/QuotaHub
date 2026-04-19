package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.SubscriptionSyncStatus
import com.lurenjia534.quotahub.data.repository.SubscriptionRepository
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
    val risk: QuotaRisk
)

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionRegistry(
    private val repository: SubscriptionRepository,
    private val providerCatalog: ProviderCatalog,
    private val cardProjectorRegistry: SubscriptionCardProjectorRegistry
) {
    val providers: List<ProviderDescriptor>
        get() = providerCatalog.descriptors

    val snapshots: Flow<List<SubscriptionCard>> = repository.subscriptions.flatMapLatest { subs ->
        if (subs.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(subs.map { sub ->
                repository.getQuotaSnapshot(sub.id).map { quotaSnapshot ->
                    cardProjectorRegistry.project(
                        subscription = sub,
                        snapshot = quotaSnapshot
                    ).let { projection ->
                        SubscriptionCard(
                            subscription = sub,
                            modelCount = projection.modelCount,
                            remainingCalls = projection.remainingCalls,
                            remainingTime = projection.remainingTime,
                            risk = projection.risk
                        )
                    }
                }
            }) { it.toList() }
        }
    }

    fun getGateway(subscription: com.lurenjia534.quotahub.data.model.Subscription): SubscriptionGateway {
        val provider = providerCatalog.provider(subscription.provider)
            ?: throw IllegalArgumentException("Unsupported provider: ${subscription.provider.id}")
        return RepositoryBackedSubscriptionGateway(
            subscriptionData = subscription,
            provider = provider,
            repository = repository
        )
    }

    suspend fun getGatewayById(subscriptionId: Long): SubscriptionGateway? {
        val subscription = repository.getSubscriptionOnce(subscriptionId) ?: return null
        return getGateway(subscription)
    }

    suspend fun createSubscription(
        provider: ProviderDescriptor,
        customTitle: String?,
        credentials: SecretBundle
    ): Long {
        return repository.createSubscription(provider, customTitle, credentials)
    }

    suspend fun validateAndCreateSubscription(
        provider: ProviderDescriptor,
        customTitle: String?,
        credentials: SecretBundle
    ): Result<Long> {
        val adapter = providerCatalog.provider(provider)
            ?: return Result.failure(
                IllegalArgumentException("Unsupported provider: ${provider.id}")
            )

        return adapter.validate(credentials).mapCatching { snapshot ->
            val subscriptionId = repository.createSubscription(
                provider = provider,
                customTitle = customTitle,
                credentials = credentials,
                syncStatus = SubscriptionSyncStatus.active(snapshot.fetchedAt)
            )
            try {
                repository.cacheQuotaSnapshot(subscriptionId, snapshot)
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
