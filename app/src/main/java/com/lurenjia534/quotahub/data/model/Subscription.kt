package com.lurenjia534.quotahub.data.model

import com.lurenjia534.quotahub.data.local.SubscriptionEntity
import com.lurenjia534.quotahub.ui.screens.home.QuotaProvider

data class Subscription(
    val id: Long,
    val provider: QuotaProvider,
    val customTitle: String?,
    val apiKey: String,
    val createdAt: Long
) {
    val displayTitle: String
        get() = customTitle?.takeIf { it.isNotBlank() }
            ?: "${provider.title} #${id}"

    val subtitle: String
        get() = "${provider.title} • ${provider.subtitle}"
}

fun SubscriptionEntity.toSubscription(): Subscription? {
    val provider = QuotaProvider.fromId(providerId) ?: return null
    return Subscription(
        id = id,
        provider = provider,
        customTitle = customTitle,
        apiKey = apiKey,
        createdAt = createdAt
    )
}