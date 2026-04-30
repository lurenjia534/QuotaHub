package com.lurenjia534.quotahub.data.provider.monitor

import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.QuotaBuckets
import com.lurenjia534.quotahub.data.model.QuotaResource
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.QuotaWindow
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.model.WindowScope
import com.lurenjia534.quotahub.data.provider.CardMetric
import com.lurenjia534.quotahub.data.provider.QuotaProgressMetric
import com.lurenjia534.quotahub.data.provider.SubscriptionCardProjection
import com.lurenjia534.quotahub.data.provider.SubscriptionCardProjector
import java.text.NumberFormat

class MonitorQuotaSubscriptionCardProjector : SubscriptionCardProjector {
    override fun project(
        subscription: Subscription,
        snapshot: QuotaSnapshot
    ): SubscriptionCardProjection {
        val limitResources = snapshot.resources.monitorLimitResources()
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
            nextResetAt = limitResources.mapNotNull { it.monitorWindow?.resetAtEpochMillis }.minOrNull(),
            risk = limitResources.monitorDominantRisk(),
            hubProgressMetrics = limitResources.monitorHubProgressMetrics()
        )
    }
}

private fun List<QuotaResource>.monitorHubProgressMetrics(): List<QuotaProgressMetric> {
    return mapNotNull { it.toMonitorProgressMetric() }
        .sortedWith(
            compareBy<QuotaProgressMetric> {
                when {
                    it.label.contains("h ", ignoreCase = true) -> 0
                    it.label.contains("daily", ignoreCase = true) -> 1
                    it.label.contains("weekly", ignoreCase = true) -> 2
                    it.label.contains("monthly", ignoreCase = true) -> 3
                    else -> 4
                }
            }.thenBy { it.label }
        )
}

private fun QuotaResource.toMonitorProgressMetric(): QuotaProgressMetric? {
    val window = monitorWindow ?: return null
    val totalValue = window.total?.takeIf { it > 0L } ?: return null
    val usedValue = window.used?.coerceIn(0L, totalValue) ?: return null
    return QuotaProgressMetric(
        label = window.monitorProgressPrefix() + " " + monitorLimitKindLabel(),
        used = usedValue,
        total = totalValue,
        remaining = window.remaining?.coerceAtLeast(0L),
        resetAtEpochMillis = window.resetAtEpochMillis
    )
}

private fun QuotaWindow.monitorProgressPrefix(): String {
    return when (scope) {
        WindowScope.Rolling -> label?.toCompactDurationLabel() ?: "Rolling"
        WindowScope.Daily -> "Daily"
        WindowScope.Weekly -> "Weekly"
        WindowScope.Monthly -> "Monthly"
        WindowScope.Interval -> label?.toCompactDurationLabel() ?: "Interval"
    }
}

private fun QuotaResource.monitorLimitKindLabel(): String {
    return when (bucket) {
        QuotaBuckets.Tokens -> "tokens"
        QuotaBuckets.Mcp -> "MCP"
        else -> title.substringBefore("(")
            .replace("usage", "", ignoreCase = true)
            .trim()
            .ifBlank { "limit" }
            .lowercase()
    }
}

private fun String.toCompactDurationLabel(): String {
    return trim()
        .replace(" hours", "h", ignoreCase = true)
        .replace(" hour", "h", ignoreCase = true)
        .replace(" months", "mo", ignoreCase = true)
        .replace(" month", "mo", ignoreCase = true)
}

private fun formatMetricCount(value: Int): String {
    return NumberFormat.getIntegerInstance().format(value)
}
