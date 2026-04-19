package com.lurenjia534.quotahub.ui.screens.home

import com.lurenjia534.quotahub.bootstrap.provider.ProviderModule
import com.lurenjia534.quotahub.bootstrap.provider.requireValidProviderModules
import com.lurenjia534.quotahub.data.model.QuotaResource
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.QuotaWindow
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.model.WindowScope

interface ProviderQuotaDetailProjector {
    fun project(
        subscription: Subscription,
        snapshot: QuotaSnapshot
    ): ProviderQuotaDetailUiModel
}

class ProviderQuotaDetailProjectorRegistry(
    private val projectors: Map<String, ProviderQuotaDetailProjector>,
    private val fallback: ProviderQuotaDetailProjector = DefaultProviderQuotaDetailProjector()
) {
    fun project(
        subscription: Subscription,
        snapshot: QuotaSnapshot
    ): ProviderQuotaDetailUiModel {
        return (projectors[subscription.provider.id] ?: fallback).project(subscription, snapshot)
    }

    companion object {
        fun fromModules(modules: List<ProviderModule>): ProviderQuotaDetailProjectorRegistry {
            val validatedModules = requireValidProviderModules(modules)
            return ProviderQuotaDetailProjectorRegistry(
                projectors = validatedModules.associate { module ->
                    module.provider.descriptor.id to module.detailProjector
                }
            )
        }
    }
}

private class DefaultProviderQuotaDetailProjector : ProviderQuotaDetailProjector {
    override fun project(
        subscription: Subscription,
        snapshot: QuotaSnapshot
    ): ProviderQuotaDetailUiModel {
        if (snapshot.resources.isEmpty()) {
            return ProviderQuotaDetailUiModel(
                summary = ProviderQuotaSummaryUiModel(
                    risk = QuotaRisk.Healthy,
                    syncLabel = subscription.syncStatus.label(),
                    syncDescription = subscription.syncStatus.description(),
                    headlineValue = "0",
                    headlineLabel = "remaining quota visible until the first provider snapshot arrives",
                    stateLabel = "Waiting for first snapshot",
                    stateDescription = "Pull down to request the first provider snapshot and populate resource-level usage data.",
                    primaryMetrics = SummaryMetricRowUiModel(
                        first = LabeledValueUiModel(
                            label = "Sync",
                            value = subscription.syncStatus.label()
                        ),
                        second = LabeledValueUiModel(
                            label = "Resources",
                            value = "0"
                        ),
                        third = LabeledValueUiModel(
                            label = "Reset",
                            value = "Waiting"
                        )
                    )
                ),
                sectionSubtitle = "Pull down to request the first provider snapshot and populate resource-level usage data."
            )
        }

        val remaining = snapshot.resources.sumOf { resource ->
            resource.windows.minOfOrNull { it.remaining ?: Long.MAX_VALUE }
                ?.takeIf { it != Long.MAX_VALUE }
                ?: 0L
        }
        val dominantRisk = snapshot.resources.map { resource ->
            resource.windows.maxOfOrNull { window ->
                when {
                    window.usageProgress() >= 0.95f -> 2
                    window.usageProgress() >= 0.80f -> 1
                    else -> 0
                }
            } ?: 0
        }.maxOrNull()?.toQuotaRisk() ?: QuotaRisk.Healthy

        return ProviderQuotaDetailUiModel(
            summary = ProviderQuotaSummaryUiModel(
                risk = dominantRisk,
                syncLabel = subscription.syncStatus.label(),
                syncDescription = subscription.syncStatus.description(),
                headlineValue = formatCount(remaining.toInt()),
                headlineLabel = "remaining quota across ${snapshot.resources.size} tracked resources",
                stateLabel = when (dominantRisk) {
                    QuotaRisk.Critical -> "Critical attention"
                    QuotaRisk.Watch -> "Watch list active"
                    QuotaRisk.Healthy -> "Healthy coverage"
                },
                stateDescription = "Quota values are projected from the provider snapshot without provider-specific rules.",
                primaryMetrics = SummaryMetricRowUiModel(
                    first = LabeledValueUiModel(
                        label = "Resources",
                        value = formatCount(snapshot.resources.size)
                    ),
                    second = LabeledValueUiModel(
                        label = "Snapshot",
                        value = if (snapshot.fetchedAt > 0) "Ready" else "Waiting"
                    ),
                    third = LabeledValueUiModel(
                        label = "State",
                        value = when (dominantRisk) {
                            QuotaRisk.Critical -> "Critical"
                            QuotaRisk.Watch -> "Watch"
                            QuotaRisk.Healthy -> "Healthy"
                        }
                    )
                )
            ),
            sectionSubtitle = "Resource windows are shown without provider-specific normalization.",
            resources = snapshot.resources.map { resource ->
                val primaryWindow = resource.windows.firstOrNull()
                val risk = resource.genericRisk()
                ProviderQuotaResourceUiModel(
                    key = resource.key,
                    title = resource.title,
                    resetLabel = primaryWindow?.resetAtEpochMillis?.takeIf { it > 0L }?.let {
                        "Reset in ${formatTimeUntil(it)}"
                    } ?: "Reset unavailable",
                    risk = risk,
                    progress = primaryWindow?.usageProgress() ?: 0f,
                    primaryMetrics = resource.windows.take(3).mapIndexed { index, window ->
                        LabeledValueUiModel(
                            label = when (index) {
                                0 -> "${window.scope.displayName()} left"
                                1 -> "${window.scope.displayName()} used"
                                else -> "${window.scope.displayName()} reset"
                            },
                            value = when (index) {
                                0 -> formatCount((window.remaining ?: 0L).toInt())
                                1 -> "${formatCount((window.used ?: 0L).toInt())} / ${formatCount((window.total ?: 0L).toInt())}"
                                else -> window.resetAtEpochMillis?.let(::formatTimeUntil) ?: "Waiting"
                            },
                            highlightRisk = index == 0
                        )
                    }
                )
            }
        )
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

private fun QuotaResource.genericRisk(): QuotaRisk {
    val maxProgress = windows.maxOfOrNull { it.usageProgress() } ?: 0f
    return when {
        maxProgress >= 0.95f -> QuotaRisk.Critical
        maxProgress >= 0.80f -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
    }
}

private fun WindowScope.displayName(): String {
    return when (this) {
        WindowScope.Interval -> "Interval"
        WindowScope.Daily -> "Daily"
        WindowScope.Weekly -> "Weekly"
        WindowScope.Monthly -> "Monthly"
        WindowScope.Rolling -> "Rolling"
    }
}

private fun Int.toQuotaRisk(): QuotaRisk {
    return when (this) {
        2 -> QuotaRisk.Critical
        1 -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
    }
}
