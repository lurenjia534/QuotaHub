package com.lurenjia534.quotahub.ui.screens.home

import com.lurenjia534.quotahub.data.model.QuotaResource
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.provider.minimax.dominantQuotaRisk
import com.lurenjia534.quotahub.data.provider.minimax.effectiveRemainingCount
import com.lurenjia534.quotahub.data.provider.minimax.effectiveUsageProgress
import com.lurenjia534.quotahub.data.provider.minimax.hasMiniMaxPlanLevelWeeklyQuota
import com.lurenjia534.quotahub.data.provider.minimax.hasVisibleWeeklyQuota
import com.lurenjia534.quotahub.data.provider.minimax.intervalRemainingCount
import com.lurenjia534.quotahub.data.provider.minimax.intervalUsedCount
import com.lurenjia534.quotahub.data.provider.minimax.intervalUsageProgress
import com.lurenjia534.quotahub.data.provider.minimax.intervalWindow
import com.lurenjia534.quotahub.data.provider.minimax.quotaRisk
import com.lurenjia534.quotahub.data.provider.minimax.relevantResetAt
import com.lurenjia534.quotahub.data.provider.minimax.weeklyRemainingCount
import com.lurenjia534.quotahub.data.provider.minimax.weeklyUsedCount
import com.lurenjia534.quotahub.data.provider.minimax.weeklyWindow
import kotlin.math.roundToInt

class MiniMaxProviderQuotaDetailProjector : ProviderQuotaDetailProjector {
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
            it.relevantResetAt(planHasWeeklyQuota)
        }.minOrNull()
        val soonestIntervalReset = snapshot.resources.mapNotNull {
            it.intervalWindow?.resetAtEpochMillis
        }.minOrNull()
        val soonestWeeklyReset = resourcesWithVisibleWeeklyQuota.mapNotNull {
            it.weeklyWindow?.resetAtEpochMillis
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
                            ?.let(::formatTimeUntil)
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
                            value = soonestWeeklyReset?.let(::formatTimeUntil) ?: "Waiting"
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
                        "Interval reset in ${formatResetValue(intervalWindow?.resetAtEpochMillis)}"
                    } else {
                        "Reset in ${formatResetValue(intervalWindow?.resetAtEpochMillis)}"
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
                                value = weeklyWindow.resetAtEpochMillis?.let(::formatTimeUntil) ?: "Waiting"
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

private fun QuotaResource.genericRisk(): QuotaRisk {
    val maxProgress = windows.maxOfOrNull { window ->
        val totalValue = window.total ?: return@maxOfOrNull 0f
        val usedValue = window.used ?: return@maxOfOrNull 0f
        if (totalValue > 0) {
            usedValue.toFloat() / totalValue.toFloat()
        } else {
            0f
        }
    } ?: 0f
    return when {
        maxProgress >= 0.95f -> QuotaRisk.Critical
        maxProgress >= 0.80f -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
    }
}

private fun formatResetValue(value: Long?): String {
    return value?.takeIf { it > 0L }?.let(::formatTimeUntil) ?: "Waiting"
}
