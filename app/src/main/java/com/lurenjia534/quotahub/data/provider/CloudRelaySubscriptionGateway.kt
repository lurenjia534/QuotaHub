package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.data.cloud.CloudSyncRepository
import com.lurenjia534.quotahub.data.model.Subscription
import kotlinx.coroutines.flow.combine

class CloudRelaySubscriptionGateway(
    private val subscriptionData: Subscription,
    private val repository: SubscriptionGatewayStore,
    private val cloudSyncRepository: CloudSyncRepository
) : SubscriptionGateway {
    override val subscription: Subscription = subscriptionData
    override val capabilities: SubscriptionGatewayCapabilities = SubscriptionGatewayCapabilities(
        canRefresh = true,
        canUpdateCredentials = false,
        canRename = false
    )

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
        val remoteSubscriptionId = subscriptionData.cloudRemoteId
            ?: return Result.failure(IllegalStateException("Cloud subscription link is missing."))
        return cloudSyncRepository.refreshRemoteSubscription(remoteSubscriptionId)
    }

    override suspend fun updateCredentials(credentials: SecretBundle): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException("Cloud subscriptions are managed by QuotaHub Relay.")
        )
    }

    override suspend fun rename(customTitle: String?): Result<Unit> {
        return Result.failure(
            UnsupportedOperationException("Rename this subscription in QuotaHub Relay.")
        )
    }

    override suspend fun disconnect() {
        repository.deleteSubscription(subscriptionData.id)
    }
}
