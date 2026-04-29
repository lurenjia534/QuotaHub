package com.lurenjia534.quotahub.data.provider.codex

import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.QuotaWindow
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.model.WindowScope
import com.lurenjia534.quotahub.data.provider.CardMetric
import com.lurenjia534.quotahub.data.provider.QuotaProgressMetric
import com.lurenjia534.quotahub.data.provider.SubscriptionCardProjection
import com.lurenjia534.quotahub.data.provider.SubscriptionCardProjector
import java.text.NumberFormat
import kotlin.math.abs

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
            risk = snapshot.resources.codexDominantRisk(),
            hubProgressMetrics = snapshot.resources.codexHubProgressMetrics()
        )
    }
}

private fun List<com.lurenjia534.quotahub.data.model.QuotaResource>.codexHubProgressMetrics(): List<QuotaProgressMetric> {
    val allWindows = flatMap { it.windows }
    val fiveHourWindow = allWindows
        .filter { it.scope == WindowScope.Rolling && it.durationMillis()?.isAboutFiveHours() == true }
        .maxByOrNull { it.usedPercentValue() }
        ?: allWindows
            .filter { it.scope == WindowScope.Rolling }
            .maxByOrNull { it.usedPercentValue() }
    val weeklyWindow = allWindows
        .filter { it.scope == WindowScope.Weekly }
        .maxByOrNull { it.usedPercentValue() }

    return listOfNotNull(
        fiveHourWindow?.toProgressMetric("5h window"),
        weeklyWindow?.toProgressMetric("Weekly limit")
    )
}

private fun QuotaWindow.toProgressMetric(label: String): QuotaProgressMetric? {
    val totalValue = total?.takeIf { it > 0L } ?: return null
    val usedValue = used?.coerceIn(0L, totalValue) ?: return null
    return QuotaProgressMetric(
        label = label,
        used = usedValue,
        total = totalValue,
        remaining = remaining?.coerceAtLeast(0L),
        resetAtEpochMillis = resetAtEpochMillis
    )
}

private fun QuotaWindow.durationMillis(): Long? {
    val start = startsAt ?: return null
    val end = endsAt ?: return null
    return (end - start).coerceAtLeast(0L)
}

private fun Long.isAboutFiveHours(): Boolean {
    return abs(this - FIVE_HOURS_MILLIS) <= ONE_HOUR_MILLIS
}

private fun formatMetricCount(value: Int): String {
    return NumberFormat.getIntegerInstance().format(value)
}

private const val FIVE_HOURS_MILLIS = 18_000_000L
private const val ONE_HOUR_MILLIS = 3_600_000L
