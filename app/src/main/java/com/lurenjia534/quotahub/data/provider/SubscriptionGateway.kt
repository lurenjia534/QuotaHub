package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.data.model.ModelRemain
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.ui.screens.home.QuotaProvider
import kotlinx.coroutines.flow.Flow

data class SubscriptionSnapshot(
    val subscription: Subscription,
    val modelRemains: List<ModelRemain> = emptyList()
)

interface SubscriptionGateway {
    val subscription: Subscription
    val snapshot: Flow<SubscriptionSnapshot>

    suspend fun refresh(): Result<Unit>

    suspend fun disconnect()
}