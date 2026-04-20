package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.sync.SubscriptionSyncCoordinator
import kotlinx.coroutines.flow.combine

class RepositoryBackedSubscriptionGateway(
    private val subscriptionData: Subscription,
    private val repository: SubscriptionGatewayStore,
    private val syncCoordinator: SubscriptionSyncCoordinator
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
        return syncCoordinator.refresh(subscriptionData.id)
    }

    override suspend fun updateCredentials(credentials: SecretBundle): Result<Unit> {
        return syncCoordinator.reauthenticate(subscriptionData.id, credentials)
    }

    override suspend fun rename(customTitle: String?): Result<Unit> {
        return repository.updateSubscriptionTitle(subscriptionData.id, customTitle)
    }

    override suspend fun disconnect() {
        repository.deleteSubscription(subscriptionData.id)
    }
}
