package com.lurenjia534.quotahub.data.provider.zai

import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.provider.CardMetric
import com.lurenjia534.quotahub.data.provider.SubscriptionCardProjection
import com.lurenjia534.quotahub.data.provider.SubscriptionCardProjector
import java.text.NumberFormat

class ZaiSubscriptionCardProjector : SubscriptionCardProjector {
    override fun project(
        subscription: Subscription,
        snapshot: QuotaSnapshot
    ): SubscriptionCardProjection {
        val limitResources = snapshot.resources.zaiLimitResources()
        if (limitResources.isEmpty()) {
            return SubscriptionCardProjection(
                primaryMetric = CardMetric(
                    label = "Headroom",
                    value = "0%"
                ),
                secondaryMetric = CardMetric(
                    label = "Limits",
                    value = formatMetricCount(0)
                ),
                resourceCount = 0,
                nextResetAt = null,
                risk = QuotaRisk.Healthy
            )
        }

        return SubscriptionCardProjection(
            primaryMetric = CardMetric(
                label = "Headroom",
                value = "${limitResources.lowestHeadroomPercent()}%"
            ),
            secondaryMetric = CardMetric(
                label = "Limits",
                value = formatMetricCount(limitResources.size)
            ),
            resourceCount = limitResources.size,
            nextResetAt = limitResources.mapNotNull { it.zaiWindow?.resetAtEpochMillis }.minOrNull(),
            risk = limitResources.zaiDominantRisk()
        )
    }
}

private fun formatMetricCount(value: Int): String {
    return NumberFormat.getIntegerInstance().format(value)
}
