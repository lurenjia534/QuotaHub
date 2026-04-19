package com.lurenjia534.quotahub.ui.screens.home

import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.provider.codex.codexDominantRisk
import com.lurenjia534.quotahub.data.provider.codex.codexRisk
import com.lurenjia534.quotahub.data.provider.codex.displayTitle
import com.lurenjia534.quotahub.data.provider.codex.displayName
import com.lurenjia534.quotahub.data.provider.codex.highestUsedPercent
import com.lurenjia534.quotahub.data.provider.codex.lowestHeadroomPercent
import com.lurenjia534.quotahub.data.provider.codex.planLabel
import com.lurenjia534.quotahub.data.provider.codex.primaryWindow
import com.lurenjia534.quotahub.data.provider.codex.progress
import com.lurenjia534.quotahub.data.provider.codex.remainingPercentValue
import com.lurenjia534.quotahub.data.provider.codex.secondaryWindow
import com.lurenjia534.quotahub.data.provider.codex.soonestResetAt
import com.lurenjia534.quotahub.data.provider.codex.usedPercentValue

class CodexProviderQuotaDetailProjector : ProviderQuotaDetailProjector {
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
                    headlineValue = "0%",
                    headlineLabel = "quota headroom visible after the first Codex snapshot arrives",
                    stateLabel = "Waiting for first snapshot",
                    stateDescription = "Pull down anytime to request the first Codex quota snapshot and populate budget windows.",
                    primaryMetrics = SummaryMetricRowUiModel(
                        first = LabeledValueUiModel(
                            label = "Sync",
                            value = subscription.syncStatus.label()
                        ),
                        second = LabeledValueUiModel(
                            label = "Buckets",
                            value = "0"
                        ),
                        third = LabeledValueUiModel(
                            label = "Peak usage",
                            value = "0%"
                        )
                    )
                ),
                sectionTitle = "Quota buckets",
                sectionSubtitle = "Codex exposes quota windows as budget percentages. Pull down anytime to refresh remote values."
            )
        }

        val totalWindowCount = snapshot.resources.sumOf { it.windows.size }
        val dominantRisk = snapshot.resources.codexDominantRisk()
        val soonestResetAt = snapshot.resources.mapNotNull { it.soonestResetAt() }.minOrNull()
        val planLabel = snapshot.resources.planLabel()

        return ProviderQuotaDetailUiModel(
            summary = ProviderQuotaSummaryUiModel(
                risk = dominantRisk,
                syncLabel = subscription.syncStatus.label(),
                syncDescription = subscription.syncStatus.description(),
                headlineValue = "${snapshot.resources.lowestHeadroomPercent()}%",
                headlineLabel = "lowest remaining headroom across ${formatCount(totalWindowCount)} tracked quota windows",
                stateLabel = when (dominantRisk) {
                    QuotaRisk.Critical -> "Critical attention"
                    QuotaRisk.Watch -> "Watch list active"
                    QuotaRisk.Healthy -> "Healthy coverage"
                },
                stateDescription = when (dominantRisk) {
                    QuotaRisk.Critical -> buildStateDescription(
                        planLabel,
                        "At least one Codex window is almost exhausted and needs attention."
                    )
                    QuotaRisk.Watch -> buildStateDescription(
                        planLabel,
                        "Some Codex windows are trending low and should be monitored."
                    )
                    QuotaRisk.Healthy -> buildStateDescription(
                        planLabel,
                        "Tracked Codex quota windows still have comfortable headroom."
                    )
                },
                primaryMetrics = SummaryMetricRowUiModel(
                    first = LabeledValueUiModel(
                        label = "Buckets",
                        value = formatCount(snapshot.resources.size)
                    ),
                    second = LabeledValueUiModel(
                        label = "Soonest reset",
                        value = soonestResetAt?.let(::formatTimeUntil) ?: "Waiting"
                    ),
                    third = LabeledValueUiModel(
                        label = "Peak usage",
                        value = "${snapshot.resources.highestUsedPercent()}%"
                    )
                )
            ),
            sectionTitle = "Quota buckets",
            sectionSubtitle = buildSectionSubtitle(planLabel),
            resources = snapshot.resources.map { resource ->
                val primaryWindow = resource.primaryWindow
                val secondaryWindow = resource.secondaryWindow
                ProviderQuotaResourceUiModel(
                    key = resource.key,
                    title = resource.displayTitle(planLabel),
                    resetLabel = resource.soonestResetAt()?.let {
                        "Reset in ${formatTimeUntil(it)}"
                    } ?: "Reset unavailable",
                    risk = resource.codexRisk(),
                    progress = resource.windows.maxOfOrNull { it.progress() } ?: 0f,
                    primaryMetrics = listOfNotNull(
                        primaryWindow?.let { window ->
                            LabeledValueUiModel(
                                label = "${window.displayName()} left",
                                value = "${window.remainingPercentValue()}%",
                                highlightRisk = true
                            )
                        },
                        primaryWindow?.let { window ->
                            LabeledValueUiModel(
                                label = "${window.displayName()} used",
                                value = "${window.usedPercentValue()}%"
                            )
                        },
                        primaryWindow?.let { window ->
                            LabeledValueUiModel(
                                label = "${window.displayName()} reset",
                                value = window.resetAtEpochMillis?.let(::formatTimeUntil) ?: "Waiting"
                            )
                        }
                    ),
                    secondaryMetrics = listOfNotNull(
                        secondaryWindow?.let { window ->
                            LabeledValueUiModel(
                                label = "${window.displayName()} left",
                                value = "${window.remainingPercentValue()}%",
                                highlightRisk = true
                            )
                        },
                        secondaryWindow?.let { window ->
                            LabeledValueUiModel(
                                label = "${window.displayName()} used",
                                value = "${window.usedPercentValue()}%"
                            )
                        },
                        secondaryWindow?.let { window ->
                            LabeledValueUiModel(
                                label = "${window.displayName()} reset",
                                value = window.resetAtEpochMillis?.let(::formatTimeUntil) ?: "Waiting"
                            )
                        }
                    )
                )
            }
        )
    }
}

private fun buildSectionSubtitle(planLabel: String?): String {
    val planPrefix = planLabel?.let { "Plan: $it. " }.orEmpty()
    return "${planPrefix}Codex exposes quota windows as budget percentages. Pull down anytime to refresh remote values."
}

private fun buildStateDescription(
    planLabel: String?,
    baseDescription: String
): String {
    return planLabel?.let { "Plan: $it. $baseDescription" } ?: baseDescription
}
