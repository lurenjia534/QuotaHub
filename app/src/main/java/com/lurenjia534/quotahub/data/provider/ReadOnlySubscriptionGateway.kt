package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.data.model.Subscription
import kotlinx.coroutines.flow.combine

class ReadOnlySubscriptionGateway(
    private val subscriptionData: Subscription,
    private val repository: SubscriptionGatewayStore
) : SubscriptionGateway {
    override val subscription: Subscription = subscriptionData
    override val capabilities: SubscriptionGatewayCapabilities = SubscriptionGatewayCapabilities(
        canRefresh = false,
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
        return Result.failure(
            IllegalStateException(
                subscriptionData.credentialIssue
                    ?: "This provider is unavailable in the current app build. Cached quota remains read-only."
            )
        )
    }

    override suspend fun updateCredentials(credentials: SecretBundle): Result<Unit> {
        return Result.failure(
            IllegalStateException(
                subscriptionData.credentialIssue
                    ?: "This provider is unavailable in the current app build. Cached quota remains read-only."
            )
        )
    }

    override suspend fun rename(customTitle: String?): Result<Unit> {
        return Result.failure(
            IllegalStateException("Read-only subscriptions cannot be renamed while the provider module is unavailable.")
        )
    }

    override suspend fun disconnect() {
        repository.deleteSubscription(subscriptionData.id)
    }
}
