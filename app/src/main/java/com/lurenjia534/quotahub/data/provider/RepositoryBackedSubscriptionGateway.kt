package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.data.model.Subscription
import kotlinx.coroutines.flow.combine

class RepositoryBackedSubscriptionGateway(
    private val subscriptionData: Subscription,
    private val provider: CodingPlanProvider,
    private val repository: SubscriptionGatewayStore
) : SubscriptionGateway {
    override val subscription: Subscription = subscriptionData

    override val snapshot = combine(
        repository.getSubscription(subscriptionData.id),
        repository.getQuotaSnapshot(subscriptionData.id)
    ) { sub, quotaSnapshot ->
        SubscriptionSnapshot(
            subscription = sub ?: subscriptionData,
            quotaSnapshot = quotaSnapshot
        )
    }

    override suspend fun refresh(): Result<Unit> {
        repository.markSubscriptionSyncing(subscriptionData.id)
        val currentSubscription = repository.getSubscriptionForRefresh(subscriptionData.id).getOrElse { error ->
            repository.markSubscriptionSyncFailure(subscriptionData.id, error)
            return Result.failure(error)
        }
        return provider.fetchSnapshot(currentSubscription).fold(
            onSuccess = { capturedSnapshot ->
                runCatching {
                    repository.cacheQuotaSnapshot(subscriptionData.id, capturedSnapshot)
                    repository.markSubscriptionSyncSuccess(
                        subscriptionId = subscriptionData.id,
                        fetchedAt = capturedSnapshot.snapshot.fetchedAt
                    )
                }.onFailure { error ->
                    repository.markSubscriptionSyncFailure(subscriptionData.id, error)
                }
            },
            onFailure = { error ->
                repository.markSubscriptionSyncFailure(subscriptionData.id, error)
                Result.failure(error)
            }
        )
    }

    override suspend fun updateCredentials(credentials: SecretBundle): Result<Unit> {
        repository.markSubscriptionSyncing(subscriptionData.id)
        return provider.validate(credentials).fold(
            onSuccess = { capturedSnapshot ->
                runCatching {
                    repository.updateSubscriptionCredentials(subscriptionData.id, credentials)
                    repository.cacheQuotaSnapshot(subscriptionData.id, capturedSnapshot)
                    repository.markSubscriptionSyncSuccess(
                        subscriptionId = subscriptionData.id,
                        fetchedAt = capturedSnapshot.snapshot.fetchedAt
                    )
                }.onFailure { error ->
                    repository.markSubscriptionSyncFailure(subscriptionData.id, error)
                }
            },
            onFailure = { error ->
                repository.markSubscriptionSyncFailure(subscriptionData.id, error)
                Result.failure(error)
            }
        )
    }

    override suspend fun rename(customTitle: String?): Result<Unit> {
        return repository.updateSubscriptionTitle(subscriptionData.id, customTitle)
    }

    override suspend fun disconnect() {
        repository.deleteSubscription(subscriptionData.id)
    }
}
