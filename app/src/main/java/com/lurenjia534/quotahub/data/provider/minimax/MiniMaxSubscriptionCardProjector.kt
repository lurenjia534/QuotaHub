package com.lurenjia534.quotahub.data.provider.minimax

import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.QuotaResource
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.QuotaWindow
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.provider.CardMetric
import com.lurenjia534.quotahub.data.provider.QuotaProgressMetric
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
            risk = snapshot.resources.dominantQuotaRisk(planHasWeeklyQuota),
            hubProgressMetrics = snapshot.resources.toMiniMaxHubProgressMetrics(planHasWeeklyQuota)
        )
    }
}

private fun List<QuotaResource>.toMiniMaxHubProgressMetrics(
    planHasWeeklyQuota: Boolean
): List<QuotaProgressMetric> {
    val resources = this
    return buildList {
        aggregateWindow(
            label = "5h window",
            windows = resources.mapNotNull { it.intervalWindow }
        )?.let(::add)

        if (planHasWeeklyQuota) {
            aggregateWindow(
                label = "Weekly limit",
                windows = resources.filter { it.hasVisibleWeeklyQuota(planHasWeeklyQuota) }
                    .mapNotNull { it.weeklyWindow }
            )?.let(::add)
        }
    }
}

private fun aggregateWindow(
    label: String,
    windows: List<QuotaWindow>
): QuotaProgressMetric? {
    val visibleWindows = windows.filter { (it.total ?: 0L) > 0L }
    if (visibleWindows.isEmpty()) return null

    val totalValue = visibleWindows.sumOf { it.total ?: 0L }
    val usedValue = visibleWindows.sumOf { it.used ?: 0L }.coerceIn(0L, totalValue)
    val remainingValue = visibleWindows
        .mapNotNull { it.remaining }
        .takeIf { it.isNotEmpty() }
        ?.sum()
    return QuotaProgressMetric(
        label = label,
        used = usedValue,
        total = totalValue,
        remaining = remainingValue,
        resetAtEpochMillis = visibleWindows.mapNotNull { it.resetAtEpochMillis }.minOrNull()
    )
}

private fun formatMetricCount(value: Int): String {
    return NumberFormat.getIntegerInstance().format(value)
}
