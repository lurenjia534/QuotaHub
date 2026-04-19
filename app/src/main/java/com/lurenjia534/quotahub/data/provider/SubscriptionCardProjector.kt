package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.bootstrap.provider.ProviderModule
import com.lurenjia534.quotahub.bootstrap.provider.requireValidProviderModules
import com.lurenjia534.quotahub.data.model.QuotaResource
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.QuotaWindow
import com.lurenjia534.quotahub.data.model.Subscription
import java.text.NumberFormat

data class CardMetric(
    val label: String,
    val value: String
)

data class SubscriptionCardProjection(
    val primaryMetric: CardMetric,
    val secondaryMetric: CardMetric?,
    val resourceCount: Int,
    val nextResetAt: Long?,
    val risk: QuotaRisk
)

interface SubscriptionCardProjector {
    fun project(
        subscription: Subscription,
        snapshot: QuotaSnapshot
    ): SubscriptionCardProjection
}

class SubscriptionCardProjectorRegistry(
    private val projectors: Map<String, SubscriptionCardProjector>,
    private val fallback: SubscriptionCardProjector = DefaultSubscriptionCardProjector()
) {
    fun project(
        subscription: Subscription,
        snapshot: QuotaSnapshot
    ): SubscriptionCardProjection {
        return (projectors[subscription.provider.id] ?: fallback).project(subscription, snapshot)
    }

    companion object {
        fun fromModules(modules: List<ProviderModule>): SubscriptionCardProjectorRegistry {
            val validatedModules = requireValidProviderModules(modules)
            return SubscriptionCardProjectorRegistry(
                projectors = validatedModules.associate { module ->
                    module.provider.descriptor.id to module.cardProjector
                }
            )
        }
    }
}

private class DefaultSubscriptionCardProjector : SubscriptionCardProjector {
    override fun project(
        subscription: Subscription,
        snapshot: QuotaSnapshot
    ): SubscriptionCardProjection {
        val remainingQuota = snapshot.resources.sumOf { resource ->
            resource.windows.minOfOrNull { it.remaining ?: Long.MAX_VALUE }
                ?.takeIf { it != Long.MAX_VALUE }
                ?.toInt()
                ?: 0
        }
        return SubscriptionCardProjection(
            primaryMetric = CardMetric(
                label = "Quota left",
                value = formatMetricCount(remainingQuota)
            ),
            secondaryMetric = CardMetric(
                label = "Resources",
                value = formatMetricCount(snapshot.resources.size)
            ),
            resourceCount = snapshot.resources.size,
            nextResetAt = snapshot.resources.flatMap { it.windows }
                .mapNotNull { it.resetAtEpochMillis }
                .minOrNull(),
            risk = snapshot.resources.genericDominantRisk()
        )
    }
}

private fun List<QuotaResource>.genericDominantRisk(): QuotaRisk {
    val maxProgress = flatMap { it.windows }.maxOfOrNull { it.usageProgress() } ?: 0f
    return when {
        maxProgress >= 0.95f -> QuotaRisk.Critical
        maxProgress >= 0.80f -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
    }
}

private fun QuotaWindow.usageProgress(): Float {
    val totalValue = total ?: return 0f
    val usedValue = used ?: return 0f
    return if (totalValue > 0) {
        usedValue.toFloat() / totalValue.toFloat()
    } else {
        0f
    }
}

private fun formatMetricCount(value: Int): String {
    return NumberFormat.getIntegerInstance().format(value)
}
