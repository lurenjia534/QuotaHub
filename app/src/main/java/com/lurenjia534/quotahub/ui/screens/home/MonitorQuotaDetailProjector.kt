package com.lurenjia534.quotahub.ui.screens.home

import com.lurenjia534.quotahub.data.model.QuotaBuckets
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.ResourceRole
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.provider.monitor.highestUsedPercent
import com.lurenjia534.quotahub.data.provider.monitor.lowestHeadroomPercent
import com.lurenjia534.quotahub.data.provider.monitor.monitorDominantRisk
import com.lurenjia534.quotahub.data.provider.monitor.monitorLimitResources
import com.lurenjia534.quotahub.data.provider.monitor.monitorResources
import com.lurenjia534.quotahub.data.provider.monitor.monitorRisk
import com.lurenjia534.quotahub.data.provider.monitor.monitorWindow
import com.lurenjia534.quotahub.data.provider.monitor.progressPercent
import com.lurenjia534.quotahub.data.provider.monitor.windowRemainingPercent
import com.lurenjia534.quotahub.data.provider.monitor.windowUsedCount
import com.lurenjia534.quotahub.data.provider.monitor.windowUsedPercent
import java.text.NumberFormat

class MonitorQuotaDetailProjector(
    private val providerName: String
) : ProviderQuotaDetailProjector {
    override fun project(
        subscription: Subscription,
        snapshot: QuotaSnapshot
    ): ProviderQuotaDetailUiModel {
        val limitResources = snapshot.resources.monitorLimitResources()
        val modelResources = snapshot.resources.monitorResources(
            role = ResourceRole.Sampled,
            bucket = QuotaBuckets.Tokens
        )
        val currentToolResources = snapshot.resources.monitorResources(
            role = ResourceRole.Contributor,
            bucket = QuotaBuckets.Mcp
        )
        val sampledToolResources = snapshot.resources.monitorResources(
            role = ResourceRole.Sampled,
            bucket = QuotaBuckets.Mcp
        )

        if (limitResources.isEmpty()) {
            return ProviderQuotaDetailUiModel(
                summary = ProviderQuotaSummaryUiModel(
                    risk = QuotaRisk.Healthy,
                    syncLabel = subscription.syncStatus.label(),
                    syncDescription = subscription.syncStatus.description(),
                    headlineValue = "0%",
                    headlineLabel = "quota headroom visible after the first $providerName snapshot arrives",
                    stateLabel = "Waiting for first snapshot",
                    stateDescription = "Pull down anytime to request the first $providerName quota snapshot and populate token and MCP windows.",
                    primaryMetrics = SummaryMetricRowUiModel(
                        first = LabeledValueUiModel(
                            label = "Sync",
                            value = subscription.syncStatus.label()
                        ),
                        second = LabeledValueUiModel(
                            label = "Limits",
                            value = "0"
                        ),
                        third = LabeledValueUiModel(
                            label = "Peak usage",
                            value = "0%"
                        )
                    )
                ),
                sectionTitle = "Quota limits",
                sectionSubtitle = "Token and MCP limits appear here after the first monitoring snapshot is cached."
            )
        }

        val dominantRisk = limitResources.monitorDominantRisk()
        val soonestResetAt = limitResources.mapNotNull { it.monitorWindow?.resetAtEpochMillis }.minOrNull()
        val totalTokensUsed = modelResources.sumOf { it.windowUsedCount() }
        val visibleToolUsage = (if (currentToolResources.isNotEmpty()) {
            currentToolResources
        } else {
            sampledToolResources
        }).sumOf { it.windowUsedCount() }

        return ProviderQuotaDetailUiModel(
            summary = ProviderQuotaSummaryUiModel(
                risk = dominantRisk,
                syncLabel = subscription.syncStatus.label(),
                syncDescription = subscription.syncStatus.description(),
                headlineValue = "${limitResources.lowestHeadroomPercent()}%",
                headlineLabel = "lowest remaining headroom across ${formatCount(limitResources.size)} tracked quota limits",
                stateLabel = when (dominantRisk) {
                    QuotaRisk.Critical -> "Critical attention"
                    QuotaRisk.Watch -> "Watch list active"
                    QuotaRisk.Healthy -> "Healthy coverage"
                },
                stateDescription = when (dominantRisk) {
                    QuotaRisk.Critical -> "At least one $providerName quota window is close to exhaustion."
                    QuotaRisk.Watch -> "Some $providerName quota windows are trending low and should be watched."
                    QuotaRisk.Healthy -> "Tracked $providerName quota windows still have comfortable headroom."
                },
                primaryMetrics = SummaryMetricRowUiModel(
                    first = LabeledValueUiModel(
                        label = "Limits",
                        value = formatCount(limitResources.size)
                    ),
                    second = LabeledValueUiModel(
                        label = "Soonest reset",
                        value = soonestResetAt?.let(::formatTimeUntil) ?: "Waiting"
                    ),
                    third = LabeledValueUiModel(
                        label = "Peak usage",
                        value = "${limitResources.highestUsedPercent()}%"
                    )
                ),
                secondaryMetrics = SummaryMetricRowUiModel(
                    first = LabeledValueUiModel(
                        label = "Models",
                        value = formatCount(modelResources.size)
                    ),
                    second = LabeledValueUiModel(
                        label = "Tokens used",
                        value = formatLongCount(totalTokensUsed)
                    ),
                    third = LabeledValueUiModel(
                        label = "Tool uses",
                        value = formatLongCount(visibleToolUsage)
                    )
                )
            ),
            sectionTitle = "Quota limits",
            sectionSubtitle = "Token and MCP windows are shown here. Model totals come from the sampled monitoring range, while MCP details prefer the current limit breakdown when available.",
            resources = limitResources.map { limitResource ->
                val relatedSecondaryResources = when (limitResource.bucket) {
                    QuotaBuckets.Tokens -> modelResources
                    QuotaBuckets.Mcp -> if (currentToolResources.isNotEmpty()) {
                        currentToolResources
                    } else {
                        sampledToolResources
                    }
                    else -> emptyList()
                }.sortedByDescending { it.windowUsedCount() }
                    .take(3)

                ProviderQuotaResourceUiModel(
                    key = limitResource.key,
                    title = limitResource.title,
                    resetLabel = limitResource.monitorWindow?.resetAtEpochMillis?.let {
                        "Reset in ${formatTimeUntil(it)}"
                    } ?: "Reset unavailable",
                    risk = limitResource.monitorRisk(),
                    progress = limitResource.monitorWindow?.progressPercent() ?: 0f,
                    primaryMetrics = listOf(
                        LabeledValueUiModel(
                            label = "Headroom",
                            value = "${limitResource.windowRemainingPercent()}%",
                            highlightRisk = true
                        ),
                        LabeledValueUiModel(
                            label = "Used",
                            value = "${limitResource.windowUsedPercent()}%"
                        ),
                        LabeledValueUiModel(
                            label = "Reset",
                            value = limitResource.monitorWindow?.resetAtEpochMillis?.let(::formatTimeUntil) ?: "Waiting"
                        )
                    ),
                    secondaryMetrics = relatedSecondaryResources.map { resource ->
                        val suffix = when (limitResource.bucket) {
                            QuotaBuckets.Tokens -> " tok"
                            else -> " uses"
                        }
                        LabeledValueUiModel(
                            label = resource.title,
                            value = "${formatLongCount(resource.windowUsedCount())}$suffix"
                        )
                    }
                )
            }
        )
    }
}

private fun formatLongCount(value: Long): String {
    return NumberFormat.getIntegerInstance().format(value)
}
