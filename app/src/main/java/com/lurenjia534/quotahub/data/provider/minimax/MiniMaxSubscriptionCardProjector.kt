package com.lurenjia534.quotahub.data.provider.minimax

import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.provider.CardMetric
import com.lurenjia534.quotahub.data.provider.SubscriptionCardProjection
import com.lurenjia534.quotahub.data.provider.SubscriptionCardProjector
import java.text.NumberFormat

class MiniMaxSubscriptionCardProjector : SubscriptionCardProjector {
    override fun project(
        subscription: Subscription,
        snapshot: QuotaSnapshot
    ): SubscriptionCardProjection {
        if (snapshot.resources.isEmpty()) {
            return SubscriptionCardProjection(
                primaryMetric = CardMetric(
                    label = "Calls left",
                    value = formatMetricCount(0)
                ),
                secondaryMetric = CardMetric(
                    label = "Models",
                    value = formatMetricCount(0)
                ),
                resourceCount = 0,
                nextResetAt = null,
                risk = QuotaRisk.Healthy
            )
        }

        val planHasWeeklyQuota = snapshot.resources.hasMiniMaxPlanLevelWeeklyQuota()
        return SubscriptionCardProjection(
            primaryMetric = CardMetric(
                label = if (planHasWeeklyQuota) "Usable left" else "Calls left",
                value = formatMetricCount(
                    snapshot.resources.sumOf {
                        it.effectiveRemainingCount(planHasWeeklyQuota)
                    }
                )
            ),
            secondaryMetric = CardMetric(
                label = "Models",
                value = formatMetricCount(snapshot.resources.size)
            ),
            resourceCount = snapshot.resources.size,
            nextResetAt = snapshot.resources.mapNotNull {
                it.relevantResetAt(planHasWeeklyQuota)
            }.minOrNull(),
            risk = snapshot.resources.dominantQuotaRisk(planHasWeeklyQuota)
        )
    }
}

private fun formatMetricCount(value: Int): String {
    return NumberFormat.getIntegerInstance().format(value)
}
