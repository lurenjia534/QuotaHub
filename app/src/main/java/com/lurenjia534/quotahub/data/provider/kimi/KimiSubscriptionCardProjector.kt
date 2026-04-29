package com.lurenjia534.quotahub.data.provider.kimi

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

class KimiSubscriptionCardProjector : SubscriptionCardProjector {
    override fun project(
        subscription: Subscription,
        snapshot: QuotaSnapshot
    ): SubscriptionCardProjection {
        val planResource = snapshot.resources.firstOrNull { it.key == PLAN_RESOURCE_KEY }
        val quotaResources = snapshot.resources.filter {
            it.key == PLAN_RESOURCE_KEY || it.key.startsWith(LIMIT_RESOURCE_KEY_PREFIX)
        }
        val parallelResource = snapshot.resources.firstOrNull { it.key == PARALLEL_RESOURCE_KEY }
        return SubscriptionCardProjection(
            primaryMetric = CardMetric(
                label = "Plan left",
                value = formatMetricCount(planResource?.kimiRemainingCount() ?: 0)
            ),
            secondaryMetric = CardMetric(
                label = "Parallel",
                value = formatMetricCount(parallelResource?.windows?.firstOrNull()?.total?.toInt() ?: 0)
            ),
            resourceCount = snapshot.resources.size,
            nextResetAt = quotaResources.flatMap { it.windows }
                .mapNotNull { it.resetAtEpochMillis }
                .minOrNull(),
            risk = quotaResources.kimiDominantRisk(),
            hubProgressMetrics = buildList {
                planResource?.kimiPlanWindow?.toProgressMetric("Plan quota")?.let(::add)
                quotaResources
                    .filter { it.key.startsWith(LIMIT_RESOURCE_KEY_PREFIX) }
                    .mapNotNull { resource ->
                        resource.windows.firstOrNull { it.scope == WindowScope.Rolling }
                    }
                    .minByOrNull { it.resetAtEpochMillis ?: Long.MAX_VALUE }
                    ?.toProgressMetric("5h window")
                    ?.let(::add)
            }
        )
    }
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

private fun formatMetricCount(value: Int): String {
    return NumberFormat.getIntegerInstance().format(value)
}
