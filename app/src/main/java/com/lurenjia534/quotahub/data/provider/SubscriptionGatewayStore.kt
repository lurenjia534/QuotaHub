package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.Subscription
import kotlinx.coroutines.flow.Flow

interface SubscriptionGatewayStore {
    fun getSubscription(subscriptionId: Long): Flow<Subscription?>

    fun getQuotaSnapshot(subscriptionId: Long): Flow<QuotaSnapshot>

    suspend fun getSubscriptionForRefresh(subscriptionId: Long): Result<Subscription>

    suspend fun readCredentials(subscriptionId: Long): Result<SecretBundle>

    suspend fun updateSubscriptionCredentials(
        subscriptionId: Long,
        credentials: SecretBundle
    )

    suspend fun cacheQuotaSnapshot(
        subscriptionId: Long,
        capturedSnapshot: CapturedQuotaSnapshot
    )

    suspend fun markSubscriptionSyncing(subscriptionId: Long)

    suspend fun markSubscriptionSyncSuccess(subscriptionId: Long, fetchedAt: Long)

    suspend fun markSubscriptionSyncFailure(subscriptionId: Long, error: Throwable)

    suspend fun updateSubscriptionTitle(subscriptionId: Long, customTitle: String?): Result<Unit>

    suspend fun deleteSubscription(subscriptionId: Long)
}
