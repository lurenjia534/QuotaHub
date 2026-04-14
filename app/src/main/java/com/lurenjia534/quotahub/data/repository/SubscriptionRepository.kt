package com.lurenjia534.quotahub.data.repository

import com.lurenjia534.quotahub.data.api.MiniMaxApiClient
import com.lurenjia534.quotahub.data.local.ModelRemainDao
import com.lurenjia534.quotahub.data.local.SubscriptionDao
import com.lurenjia534.quotahub.data.local.SubscriptionEntity
import com.lurenjia534.quotahub.data.local.toEntity
import com.lurenjia534.quotahub.data.local.toModelRemain
import com.lurenjia534.quotahub.data.model.ModelRemain
import com.lurenjia534.quotahub.data.model.ModelRemainResponse
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.model.toSubscription
import com.lurenjia534.quotahub.ui.screens.home.QuotaProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SubscriptionRepository(
    private val subscriptionDao: SubscriptionDao,
    private val modelRemainDao: ModelRemainDao
) {
    val subscriptions: Flow<List<Subscription>> = subscriptionDao.getAllSubscriptions().map { entities ->
        entities.mapNotNull { it.toSubscription() }
    }

    fun getSubscription(subscriptionId: Long): Flow<Subscription?> {
        return subscriptionDao.getSubscription(subscriptionId).map { it?.toSubscription() }
    }

    suspend fun getSubscriptionOnce(subscriptionId: Long): Subscription? {
        return subscriptionDao.getSubscriptionOnce(subscriptionId)?.toSubscription()
    }

    suspend fun getSubscriptionCount(): Int {
        return subscriptionDao.getSubscriptionCount()
    }

    suspend fun createSubscription(
        provider: QuotaProvider,
        customTitle: String?,
        apiKey: String
    ): Long {
        val entity = SubscriptionEntity(
            providerId = provider.id,
            customTitle = customTitle?.trim()?.takeIf { it.isNotBlank() },
            apiKey = apiKey.trim()
        )
        return subscriptionDao.insertSubscription(entity)
    }

    suspend fun updateSubscription(subscription: Subscription) {
        val entity = SubscriptionEntity(
            id = subscription.id,
            providerId = subscription.provider.id,
            customTitle = subscription.customTitle,
            apiKey = subscription.apiKey,
            createdAt = subscription.createdAt
        )
        subscriptionDao.updateSubscription(entity)
    }

    suspend fun deleteSubscription(subscriptionId: Long) {
        modelRemainDao.clearModelRemainsBySubscription(subscriptionId)
        subscriptionDao.deleteSubscriptionById(subscriptionId)
    }

    fun getModelRemains(subscriptionId: Long): Flow<List<ModelRemain>> {
        return modelRemainDao.getModelRemainsBySubscription(subscriptionId).map { entities ->
            entities.map { it.toModelRemain() }
        }
    }

    suspend fun refreshQuota(subscriptionId: Long, authorization: String): Result<Unit> {
        return runCatching {
            val response = fetchQuota(authorization)
            cacheQuotaResponse(subscriptionId, response)
        }
    }

    suspend fun validateApiKey(provider: QuotaProvider, apiKey: String): Result<ModelRemainResponse> {
        return runCatching {
            fetchQuota("Bearer ${apiKey.trim()}")
        }
    }

    suspend fun cacheQuotaResponse(subscriptionId: Long, response: ModelRemainResponse) {
        ensureSubscriptionExists(subscriptionId)

        val cachedAt = System.currentTimeMillis()
        modelRemainDao.clearModelRemainsBySubscription(subscriptionId)
        ensureSubscriptionExists(subscriptionId)
        modelRemainDao.upsertModelRemains(
            response.modelRemains.mapIndexed { index, modelRemain ->
                modelRemain.toEntity(
                    subscriptionId = subscriptionId,
                    displayOrder = index,
                    cachedAt = cachedAt
                )
            }
        )
    }

    private suspend fun fetchQuota(authorization: String): ModelRemainResponse {
        return MiniMaxApiClient.apiService.getModelRemains(authorization)
    }

    private suspend fun ensureSubscriptionExists(subscriptionId: Long) {
        if (subscriptionDao.getSubscriptionOnce(subscriptionId) == null) {
            throw IllegalStateException("Subscription no longer exists")
        }
    }
}
