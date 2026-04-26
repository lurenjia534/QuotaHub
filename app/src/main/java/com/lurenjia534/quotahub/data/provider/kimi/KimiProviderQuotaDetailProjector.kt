package com.lurenjia534.quotahub.data.provider.kimi

import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.ui.screens.home.LabeledValueUiModel
import com.lurenjia534.quotahub.ui.screens.home.ProviderQuotaDetailProjector
import com.lurenjia534.quotahub.ui.screens.home.ProviderQuotaDetailUiModel
import com.lurenjia534.quotahub.ui.screens.home.ProviderQuotaResourceUiModel
import com.lurenjia534.quotahub.ui.screens.home.ProviderQuotaSummaryUiModel
import com.lurenjia534.quotahub.ui.screens.home.SummaryMetricRowUiModel
import com.lurenjia534.quotahub.ui.screens.home.description
import com.lurenjia534.quotahub.ui.screens.home.formatCount
import com.lurenjia534.quotahub.ui.screens.home.formatTimeUntil
import com.lurenjia534.quotahub.ui.screens.home.label
import kotlin.math.roundToInt

class KimiProviderQuotaDetailProjector : ProviderQuotaDetailProjector {
    override fun project(
        subscription: Subscription,
        snapshot: QuotaSnapshot
    ): ProviderQuotaDetailUiModel {
        val planResource = snapshot.resources.firstOrNull { it.key == PLAN_RESOURCE_KEY }
        val limitResources = snapshot.resources
            .filter { it.key.startsWith(LIMIT_RESOURCE_KEY_PREFIX) }
        val quotaResources = listOfNotNull(planResource) + limitResources
        if (planResource == null) {
            return ProviderQuotaDetailUiModel(
                summary = ProviderQuotaSummaryUiModel(
                    risk = QuotaRisk.Healthy,
                    syncLabel = subscription.syncStatus.label(),
                    syncDescription = subscription.syncStatus.description(),
                    headlineValue = "0",
                    headlineLabel = "requests visible after the first Kimi snapshot arrives",
                    stateLabel = "Waiting for first snapshot",
                    stateDescription = "Pull down anytime to request the first Kimi Coding Plan snapshot.",
                    primaryMetrics = SummaryMetricRowUiModel(
                        first = LabeledValueUiModel(
                            label = "Sync",
                            value = subscription.syncStatus.label()
                        ),
                        second = LabeledValueUiModel(
                            label = "Limit",
                            value = "0"
                        ),
                        third = LabeledValueUiModel(
                            label = "Reset",
                            value = "Waiting"
                        )
                    )
                ),
                sectionTitle = "Coding quota",
                sectionSubtitle = "Kimi Coding Plan usage appears here after the first snapshot is cached."
            )
        }

        val planWindow = planResource.kimiPlanWindow
        val soonestWindowReset = limitResources
            .mapNotNull { it.kimiPrimaryWindow?.resetAtEpochMillis }
            .minOrNull()
        val parallelLimit = snapshot.resources
            .firstOrNull { it.key == PARALLEL_RESOURCE_KEY }
            ?.windows
            ?.firstOrNull()
            ?.total
            ?.toInt()
            ?: 0
        val risk = quotaResources.kimiDominantRisk()
        val used = planWindow?.used?.toInt() ?: 0
        val remaining = planResource.kimiRemainingCount()
        val total = planResource.kimiLimitCount()

        return ProviderQuotaDetailUiModel(
            summary = ProviderQuotaSummaryUiModel(
                risk = risk,
                syncLabel = subscription.syncStatus.label(),
                syncDescription = subscription.syncStatus.description(),
                headlineValue = formatCount(remaining),
                headlineLabel = "requests left in the current Kimi Coding Plan cycle",
                stateLabel = when (risk) {
                    QuotaRisk.Critical -> "Critical attention"
                    QuotaRisk.Watch -> "Watch list active"
                    QuotaRisk.Healthy -> "Healthy coverage"
                },
                stateDescription = when (risk) {
                    QuotaRisk.Critical -> "Kimi Coding Plan usage is close to its limit."
                    QuotaRisk.Watch -> "Kimi Coding Plan quota is trending low and should be watched."
                    QuotaRisk.Healthy -> "Kimi Coding Plan still has comfortable headroom."
                },
                primaryMetrics = SummaryMetricRowUiModel(
                    first = LabeledValueUiModel(
                        label = "Plan left",
                        value = formatCount(remaining)
                    ),
                    second = LabeledValueUiModel(
                        label = "Window reset",
                        value = soonestWindowReset?.let(::formatTimeUntil)
                            ?: planWindow?.resetAtEpochMillis?.let(::formatTimeUntil)
                            ?: "Waiting"
                    ),
                    third = LabeledValueUiModel(
                        label = "Plan used",
                        value = "${(planResource.kimiUsageProgress() * 100).roundToInt()}%"
                    )
                ),
                secondaryMetrics = SummaryMetricRowUiModel(
                    first = LabeledValueUiModel(
                        label = "Plan limit",
                        value = formatCount(total)
                    ),
                    second = LabeledValueUiModel(
                        label = "Windows",
                        value = formatCount(limitResources.size)
                    ),
                    third = LabeledValueUiModel(
                        label = "Parallel",
                        value = formatCount(parallelLimit)
                    )
                )
            ),
            sectionTitle = "Coding quota",
            sectionSubtitle = "Plan: total purchased quota, rolling windows, and parallel capacity are separated so the effective constraint is easy to scan.",
            resources = buildList {
                add(
                ProviderQuotaResourceUiModel(
                    key = planResource.key,
                    title = planResource.title,
                    resetLabel = "Plan resets in ${formatResetValue(planWindow?.resetAtEpochMillis)}",
                    risk = planResource.kimiRisk(),
                    progress = planResource.kimiUsageProgress(),
                    primaryMetrics = listOf(
                        LabeledValueUiModel(
                            label = "Total remaining",
                            value = formatCount(remaining),
                            highlightRisk = true
                        ),
                        LabeledValueUiModel(
                            label = "Used",
                            value = "${formatCount(used)} / ${formatCount(total)}"
                        ),
                        LabeledValueUiModel(
                            label = "Reset",
                            value = planWindow?.resetAtEpochMillis?.let(::formatTimeUntil) ?: "Waiting"
                        )
                    ),
                    secondaryMetrics = listOf(
                        LabeledValueUiModel(
                            label = "Limit type",
                            value = "Purchased plan"
                        ),
                        LabeledValueUiModel(
                            label = "Cycle",
                            value = "Current"
                        )
                    )
                )
                )
                limitResources.forEach { resource ->
                    val window = resource.kimiPrimaryWindow
                    add(
                        ProviderQuotaResourceUiModel(
                            key = resource.key,
                            title = resource.title,
                            resetLabel = "Window resets in ${formatResetValue(window?.resetAtEpochMillis)}",
                            risk = resource.kimiRisk(),
                            progress = resource.kimiUsageProgress(),
                            primaryMetrics = listOf(
                                LabeledValueUiModel(
                                    label = "Window remaining",
                                    value = formatCount(resource.kimiRemainingCount()),
                                    highlightRisk = true
                                ),
                                LabeledValueUiModel(
                                    label = "Used",
                                    value = "${formatCount((window?.used ?: 0L).toInt())} / ${formatCount(resource.kimiLimitCount())}"
                                ),
                                LabeledValueUiModel(
                                    label = "Reset",
                                    value = window?.resetAtEpochMillis?.let(::formatTimeUntil) ?: "Waiting"
                                )
                            ),
                            secondaryMetrics = listOf(
                                LabeledValueUiModel(
                                    label = "Constraint",
                                    value = window?.label ?: "Window"
                                ),
                                LabeledValueUiModel(
                                    label = "Scope",
                                    value = window?.scope?.kimiDisplayName() ?: "Window"
                                )
                            )
                        )
                    )
                }
                add(
                    ProviderQuotaResourceUiModel(
                        key = PARALLEL_RESOURCE_KEY,
                        title = "Parallel capacity",
                        resetLabel = "No reset window",
                        risk = QuotaRisk.Healthy,
                        progress = 0f,
                        primaryMetrics = listOf(
                            LabeledValueUiModel(
                                label = "Concurrent capacity",
                                value = formatCount(parallelLimit),
                                highlightRisk = true
                            ),
                            LabeledValueUiModel(
                                label = "Capacity type",
                                value = "Concurrent"
                            )
                        ),
                        secondaryMetrics = listOf(
                            LabeledValueUiModel(
                                label = "Meaning",
                                value = "Simultaneous sessions"
                            )
                        )
                    )
                )
            }
        )
    }
}

private fun formatResetValue(value: Long?): String {
    return value?.takeIf { it > 0L }?.let(::formatTimeUntil) ?: "Waiting"
}

private fun com.lurenjia534.quotahub.data.model.WindowScope.kimiDisplayName(): String {
    return when (this) {
        com.lurenjia534.quotahub.data.model.WindowScope.Interval -> "Interval"
        com.lurenjia534.quotahub.data.model.WindowScope.Daily -> "Daily"
        com.lurenjia534.quotahub.data.model.WindowScope.Weekly -> "Weekly"
        com.lurenjia534.quotahub.data.model.WindowScope.Monthly -> "Monthly"
        com.lurenjia534.quotahub.data.model.WindowScope.Rolling -> "Rolling"
    }
}
