package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.SubscriptionSyncStatus
import com.lurenjia534.quotahub.data.repository.SubscriptionRepository
import com.lurenjia534.quotahub.sync.SubscriptionSyncCoordinator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class SubscriptionCard(
    val subscription: com.lurenjia534.quotahub.data.model.Subscription,
    val primaryMetric: CardMetric,
    val secondaryMetric: CardMetric?,
    val resourceCount: Int,
    val nextResetAt: Long?,
    val risk: QuotaRisk,
    val hubProgressMetrics: List<QuotaProgressMetric>
)

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionRegistry(
    private val repository: SubscriptionRepository,
    private val providerCatalog: ProviderCatalog,
    private val cardProjectorRegistry: SubscriptionCardProjectorRegistry,
    private val syncCoordinator: SubscriptionSyncCoordinator
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
                            primaryMetric = projection.primaryMetric,
                            secondaryMetric = projection.secondaryMetric,
                            resourceCount = projection.resourceCount,
                            nextResetAt = projection.nextResetAt,
                            risk = projection.risk,
                            hubProgressMetrics = projection.hubProgressMetrics
                        )
                    }
                }
            }) { it.toList() }
        }
    }

    fun getGateway(subscription: com.lurenjia534.quotahub.data.model.Subscription): SubscriptionGateway {
        return if (subscription.isProviderSupported) {
            RepositoryBackedSubscriptionGateway(
                subscriptionData = subscription,
                repository = repository,
                syncCoordinator = syncCoordinator
            )
        } else {
            ReadOnlySubscriptionGateway(
                subscriptionData = subscription,
                repository = repository
            )
        }
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

        return adapter.validate(credentials).mapCatching { capturedSnapshot ->
            val subscriptionId = repository.createSubscription(
                provider = provider,
                customTitle = customTitle,
                credentials = credentials,
                syncStatus = SubscriptionSyncStatus.active(capturedSnapshot.snapshot.fetchedAt)
            )
            try {
                repository.cacheQuotaSnapshot(subscriptionId, capturedSnapshot)
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
