package com.lurenjia534.quotahub.data.provider.codex

import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.provider.CardMetric
import com.lurenjia534.quotahub.data.provider.SubscriptionCardProjection
import com.lurenjia534.quotahub.data.provider.SubscriptionCardProjector
import java.text.NumberFormat

class CodexSubscriptionCardProjector : SubscriptionCardProjector {
    override fun project(
        subscription: Subscription,
        snapshot: QuotaSnapshot
    ): SubscriptionCardProjection {
        if (snapshot.resources.isEmpty()) {
            return SubscriptionCardProjection(
                primaryMetric = CardMetric(
                    label = "Headroom",
                    value = "0%"
                ),
                secondaryMetric = CardMetric(
                    label = "Buckets",
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
                value = "${snapshot.resources.lowestHeadroomPercent()}%"
            ),
            secondaryMetric = CardMetric(
                label = "Buckets",
                value = formatMetricCount(snapshot.resources.size)
            ),
            resourceCount = snapshot.resources.size,
            nextResetAt = snapshot.resources.mapNotNull { it.soonestResetAt() }.minOrNull(),
            risk = snapshot.resources.codexDominantRisk()
        )
    }
}

private fun formatMetricCount(value: Int): String {
    return NumberFormat.getIntegerInstance().format(value)
}
