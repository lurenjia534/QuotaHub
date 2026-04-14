package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.repository.SubscriptionRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class MiniMaxSubscriptionGateway(
    private val subscriptionData: Subscription,
    private val repository: SubscriptionRepository
) : SubscriptionGateway {
    override val subscription: Subscription = subscriptionData

    override val snapshot = combine(
        repository.getSubscription(subscriptionData.id),
        repository.getModelRemains(subscriptionData.id)
    ) { sub, modelRemains ->
        SubscriptionSnapshot(
            subscription = sub ?: subscriptionData,
            modelRemains = modelRemains
        )
    }

    override suspend fun refresh(): Result<Unit> {
        return repository.refreshQuota(subscriptionData.id, "Bearer ${subscriptionData.apiKey}")
    }

    override suspend fun disconnect() {
        repository.deleteSubscription(subscriptionData.id)
    }
}