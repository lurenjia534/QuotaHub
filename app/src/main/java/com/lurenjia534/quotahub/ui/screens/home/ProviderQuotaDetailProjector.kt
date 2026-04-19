package com.lurenjia534.quotahub.ui.screens.home

import com.lurenjia534.quotahub.data.model.QuotaResource
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.QuotaWindow
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.model.WindowScope
import com.lurenjia534.quotahub.data.provider.minimax.MiniMaxCodingPlanProvider
import kotlin.math.max
import kotlin.math.roundToInt

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
        fun default(): ProviderQuotaDetailProjectorRegistry {
            return ProviderQuotaDetailProjectorRegistry(
                projectors = mapOf(
                    MiniMaxCodingPlanProvider.ID to MiniMaxProviderQuotaDetailProjector()
                )
            )
        }
    }
}

private class MiniMaxProviderQuotaDetailProjector : ProviderQuotaDetailProjector {
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
                    headlineLabel = "usable calls visible until the first snapshot arrives",
                    stateLabel = "Waiting for first snapshot",
                    stateDescription = "Pull down anytime to attempt a provider sync and populate model-level quota detail.",
                    primaryMetrics = SummaryMetricRowUiModel(
                        first = LabeledValueUiModel(
                            label = "Sync",
                            value = subscription.syncStatus.label()
                        ),
                        second = LabeledValueUiModel(
                            label = "Models",
                            value = "0"
                        ),
                        third = LabeledValueUiModel(
                            label = "Reset",
                            value = "Waiting"
                        )
                    )
                ),
                sectionSubtitle = "Interval usage is grouped here for quick scanning. Pull down anytime to refresh remote values."
            )
        }

        val planHasWeeklyQuota = snapshot.resources.hasMiniMaxPlanLevelWeeklyQuota()
        val totalIntervalAllowance = snapshot.resources.sumOf {
            it.intervalWindow?.total?.toInt() ?: 0
        }
        val totalIntervalUsed = snapshot.resources.sumOf {
            it.intervalUsedCount()
        }
        val totalIntervalRemaining = snapshot.resources.sumOf {
            it.intervalRemainingCount()
        }
        val resourcesWithVisibleWeeklyQuota = snapshot.resources.filter {
            it.hasVisibleWeeklyQuota(planHasWeeklyQuota)
        }
        val totalWeeklyAllowance = resourcesWithVisibleWeeklyQuota.sumOf {
            it.weeklyWindow?.total?.toInt() ?: 0
        }
        val totalWeeklyUsed = resourcesWithVisibleWeeklyQuota.sumOf {
            it.weeklyUsedCount()
        }
        val totalWeeklyRemaining = resourcesWithVisibleWeeklyQuota.sumOf {
            it.weeklyRemainingCount()
        }
        val totalEffectiveRemaining = snapshot.resources.sumOf {
            it.effectiveRemainingCount(planHasWeeklyQuota)
        }
        val intervalProgress = if (totalIntervalAllowance > 0) {
            totalIntervalUsed.toFloat() / totalIntervalAllowance.toFloat()
        } else {
            0f
        }
        val weeklyProgress = if (totalWeeklyAllowance > 0) {
            totalWeeklyUsed.toFloat() / totalWeeklyAllowance.toFloat()
        } else {
            0f
        }
        val soonestReset = snapshot.resources.mapNotNull {
            it.relevantResetTime(planHasWeeklyQuota)
        }.minOrNull()
        val soonestIntervalReset = snapshot.resources.mapNotNull {
            it.intervalWindow?.resetsAt
        }.minOrNull()
        val soonestWeeklyReset = resourcesWithVisibleWeeklyQuota.mapNotNull {
            it.weeklyWindow?.resetsAt
        }.minOrNull()
        val watchCount = snapshot.resources.count {
            it.quotaRisk(planHasWeeklyQuota) != QuotaRisk.Healthy
        }
        val dominantRisk = snapshot.resources.dominantQuotaRisk(planHasWeeklyQuota)
        val stateLabel = when {
            dominantRisk == QuotaRisk.Critical -> "Critical attention"
            dominantRisk == QuotaRisk.Watch -> "Watch list active"
            planHasWeeklyQuota -> "Interval and weekly caps active"
            else -> "Healthy coverage"
        }
        val stateDescription = when {
            dominantRisk == QuotaRisk.Critical ->
                "At least one model is close to exhausting its available quota."
            dominantRisk == QuotaRisk.Watch ->
                "Some models are trending low and should be monitored."
            planHasWeeklyQuota ->
                "This plan combines interval and weekly caps. The headline value reflects the tighter limit for each model."
            else ->
                "Quota levels are stable and no model is near its critical threshold."
        }

        return ProviderQuotaDetailUiModel(
            summary = ProviderQuotaSummaryUiModel(
                risk = dominantRisk,
                syncLabel = subscription.syncStatus.label(),
                syncDescription = subscription.syncStatus.description(),
                headlineValue = formatCount(totalEffectiveRemaining),
                headlineLabel = if (planHasWeeklyQuota) {
                    "usable calls left across ${snapshot.resources.size} tracked models"
                } else {
                    "calls left across ${snapshot.resources.size} tracked models"
                },
                stateLabel = stateLabel,
                stateDescription = stateDescription,
                primaryMetrics = SummaryMetricRowUiModel(
                    first = LabeledValueUiModel(
                        label = if (planHasWeeklyQuota) "Interval left" else "Calls left",
                        value = formatCount(totalIntervalRemaining)
                    ),
                    second = LabeledValueUiModel(
                        label = if (planHasWeeklyQuota) "Interval reset" else "Soonest reset",
                        value = (if (planHasWeeklyQuota) soonestIntervalReset else soonestReset)
                            ?.let(::formatTimeRemaining)
                            ?: "Waiting"
                    ),
                    third = LabeledValueUiModel(
                        label = "Models to watch",
                        value = watchCount.toString()
                    )
                ),
                secondaryMetrics = if (planHasWeeklyQuota) {
                    SummaryMetricRowUiModel(
                        first = LabeledValueUiModel(
                            label = "Weekly left",
                            value = formatCount(totalWeeklyRemaining)
                        ),
                        second = LabeledValueUiModel(
                            label = "Weekly reset",
                            value = soonestWeeklyReset?.let(::formatTimeRemaining) ?: "Waiting"
                        ),
                        third = LabeledValueUiModel(
                            label = "Weekly used",
                            value = "${(weeklyProgress * 100).roundToInt()}%"
                        )
                    )
                } else {
                    SummaryMetricRowUiModel(
                        first = LabeledValueUiModel(
                            label = "Used",
                            value = "${(intervalProgress * 100).roundToInt()}%"
                        ),
                        second = LabeledValueUiModel(
                            label = "Interval total",
                            value = formatCount(totalIntervalAllowance)
                        ),
                        third = LabeledValueUiModel(
                            label = "Used calls",
                            value = formatCount(totalIntervalUsed)
                        )
                    )
                }
            ),
            sectionTitle = "Model quota",
            sectionSubtitle = "Interval usage is grouped here for quick scanning. Pull down anytime to refresh remote values.",
            resources = snapshot.resources.map { resource ->
                val showWeeklyQuota = resource.hasVisibleWeeklyQuota(planHasWeeklyQuota)
                val intervalWindow = resource.intervalWindow
                val weeklyWindow = resource.weeklyWindow
                val risk = resource.quotaRisk(planHasWeeklyQuota)
                ProviderQuotaResourceUiModel(
                    key = resource.key,
                    title = resource.title,
                    resetLabel = if (showWeeklyQuota) {
                        "Interval reset in ${formatResetValue(intervalWindow?.resetsAt)}"
                    } else {
                        "Reset in ${formatResetValue(intervalWindow?.resetsAt)}"
                    },
                    risk = risk,
                    progress = resource.effectiveUsageProgress(planHasWeeklyQuota),
                    primaryMetrics = buildList {
                        add(
                            LabeledValueUiModel(
                                label = if (showWeeklyQuota) "Interval left" else "Remaining",
                                value = formatCount(resource.intervalRemainingCount()),
                                highlightRisk = true
                            )
                        )
                        add(
                            LabeledValueUiModel(
                                label = "Used",
                                value = "${formatCount(resource.intervalUsedCount())} / ${formatCount(intervalWindow?.total?.toInt() ?: 0)}"
                            )
                        )
                        add(
                            LabeledValueUiModel(
                                label = if (showWeeklyQuota) "Interval usage" else "Usage",
                                value = "${(resource.intervalUsageProgress() * 100).roundToInt()}%"
                            )
                        )
                    },
                    secondaryMetrics = if (showWeeklyQuota && weeklyWindow != null) {
                        listOf(
                            LabeledValueUiModel(
                                label = "Weekly left",
                                value = formatCount(resource.weeklyRemainingCount()),
                                highlightRisk = true
                            ),
                            LabeledValueUiModel(
                                label = "Weekly used",
                                value = "${formatCount(resource.weeklyUsedCount())} / ${formatCount(weeklyWindow.total?.toInt() ?: 0)}"
                            ),
                            LabeledValueUiModel(
                                label = "Weekly reset",
                                value = formatTimeRemaining(weeklyWindow.resetsAt ?: 0L)
                            )
                        )
                    } else {
                        emptyList()
                    }
                )
            }
        )
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
            resource.windows.minOfOrNull { it.remaining ?: Long.MAX_VALUE }?.takeIf { it != Long.MAX_VALUE } ?: 0L
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
                    resetLabel = primaryWindow?.resetsAt?.takeIf { it > 0L }?.let {
                        "Reset in ${formatTimeRemaining(it)}"
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
                                else -> window.resetsAt?.let(::formatTimeRemaining) ?: "Waiting"
                            },
                            highlightRisk = index == 0
                        )
                    }
                )
            }
        )
    }
}

private val QuotaResource.intervalWindow: QuotaWindow?
    get() = windows.firstOrNull { it.scope == WindowScope.Interval }

private val QuotaResource.weeklyWindow: QuotaWindow?
    get() = windows.firstOrNull { it.scope == WindowScope.Weekly }

private fun List<QuotaResource>.hasMiniMaxPlanLevelWeeklyQuota(): Boolean {
    return any { resource ->
        resource.key in WeeklyPlanAnchorResourceKeys &&
            (resource.weeklyWindow?.total ?: 0L) > 0L
    }
}

private fun QuotaResource.hasVisibleWeeklyQuota(planHasWeeklyQuota: Boolean): Boolean {
    return planHasWeeklyQuota && (weeklyWindow?.total ?: 0L) > 0L
}

private fun QuotaResource.intervalRemainingCount(): Int {
    return (intervalWindow?.remaining ?: 0L).toInt()
}

private fun QuotaResource.intervalUsedCount(): Int {
    return (intervalWindow?.used ?: 0L).toInt()
}

private fun QuotaResource.intervalUsageProgress(): Float {
    return intervalWindow?.usageProgress() ?: 0f
}

private fun QuotaResource.weeklyRemainingCount(): Int {
    return (weeklyWindow?.remaining ?: 0L).toInt()
}

private fun QuotaResource.weeklyUsedCount(): Int {
    return (weeklyWindow?.used ?: 0L).toInt()
}

private fun QuotaResource.effectiveRemainingCount(planHasWeeklyQuota: Boolean): Int {
    return if (hasVisibleWeeklyQuota(planHasWeeklyQuota)) {
        minOf(intervalRemainingCount(), weeklyRemainingCount())
    } else {
        intervalRemainingCount()
    }
}

private fun QuotaResource.effectiveUsageProgress(planHasWeeklyQuota: Boolean): Float {
    return if (hasVisibleWeeklyQuota(planHasWeeklyQuota)) {
        max(intervalUsageProgress(), weeklyWindow?.usageProgress() ?: 0f)
    } else {
        intervalUsageProgress()
    }
}

private fun QuotaResource.relevantResetTime(planHasWeeklyQuota: Boolean): Long? {
    return buildList {
        intervalWindow?.resetsAt?.takeIf { it > 0L }?.let(::add)
        if (hasVisibleWeeklyQuota(planHasWeeklyQuota)) {
            weeklyWindow?.resetsAt?.takeIf { it > 0L }?.let(::add)
        }
    }.minOrNull()
}

private fun QuotaResource.quotaRisk(planHasWeeklyQuota: Boolean): QuotaRisk {
    val usageProgress = effectiveUsageProgress(planHasWeeklyQuota)
    return when {
        usageProgress >= 0.95f -> QuotaRisk.Critical
        usageProgress >= 0.80f -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
    }
}

private fun List<QuotaResource>.dominantQuotaRisk(planHasWeeklyQuota: Boolean): QuotaRisk {
    return when {
        any { it.quotaRisk(planHasWeeklyQuota) == QuotaRisk.Critical } -> QuotaRisk.Critical
        any { it.quotaRisk(planHasWeeklyQuota) == QuotaRisk.Watch } -> QuotaRisk.Watch
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

private fun formatResetValue(value: Long?): String {
    return value?.takeIf { it > 0L }?.let(::formatTimeRemaining) ?: "Waiting"
}

private val WeeklyPlanAnchorResourceKeys = setOf(
    "MiniMax-M*",
    "coding-plan-vlm",
    "coding-plan-search"
)
