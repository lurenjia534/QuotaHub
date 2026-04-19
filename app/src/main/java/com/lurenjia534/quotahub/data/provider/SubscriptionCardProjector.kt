package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.data.model.QuotaResource
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.QuotaWindow
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.model.WindowScope
import com.lurenjia534.quotahub.data.provider.minimax.MiniMaxCodingPlanProvider
import kotlin.math.max

data class SubscriptionCardProjection(
    val modelCount: Int,
    val remainingCalls: Int,
    val remainingTime: Long?,
    val risk: QuotaRisk
)

interface SubscriptionCardProjector {
    fun project(
        subscription: Subscription,
        snapshot: QuotaSnapshot
    ): SubscriptionCardProjection
}

class SubscriptionCardProjectorRegistry(
    private val projectors: Map<String, SubscriptionCardProjector>,
    private val fallback: SubscriptionCardProjector = DefaultSubscriptionCardProjector()
) {
    fun project(
        subscription: Subscription,
        snapshot: QuotaSnapshot
    ): SubscriptionCardProjection {
        return (projectors[subscription.provider.id] ?: fallback).project(subscription, snapshot)
    }

    companion object {
        fun default(): SubscriptionCardProjectorRegistry {
            return SubscriptionCardProjectorRegistry(
                projectors = mapOf(
                    MiniMaxCodingPlanProvider.ID to MiniMaxSubscriptionCardProjector()
                )
            )
        }
    }
}

private class MiniMaxSubscriptionCardProjector : SubscriptionCardProjector {
    override fun project(
        subscription: Subscription,
        snapshot: QuotaSnapshot
    ): SubscriptionCardProjection {
        if (snapshot.resources.isEmpty()) {
            return SubscriptionCardProjection(
                modelCount = 0,
                remainingCalls = 0,
                remainingTime = null,
                risk = QuotaRisk.Healthy
            )
        }

        val planHasWeeklyQuota = snapshot.resources.hasMiniMaxPlanLevelWeeklyQuota()
        return SubscriptionCardProjection(
            modelCount = snapshot.resources.size,
            remainingCalls = snapshot.resources.sumOf {
                it.effectiveRemainingCount(planHasWeeklyQuota)
            },
            remainingTime = snapshot.resources.mapNotNull {
                it.relevantResetTime(planHasWeeklyQuota)
            }.minOrNull(),
            risk = snapshot.resources.dominantQuotaRisk(planHasWeeklyQuota)
        )
    }
}

private class DefaultSubscriptionCardProjector : SubscriptionCardProjector {
    override fun project(
        subscription: Subscription,
        snapshot: QuotaSnapshot
    ): SubscriptionCardProjection {
        return SubscriptionCardProjection(
            modelCount = snapshot.resources.size,
            remainingCalls = snapshot.resources.sumOf { resource ->
                resource.windows.minOfOrNull { it.remaining ?: Long.MAX_VALUE }
                    ?.takeIf { it != Long.MAX_VALUE }
                    ?.toInt()
                    ?: 0
            },
            remainingTime = snapshot.resources.flatMap { it.windows }
                .mapNotNull { it.resetsAt }
                .minOrNull(),
            risk = snapshot.resources.genericDominantRisk()
        )
    }
}

private fun List<QuotaResource>.hasMiniMaxPlanLevelWeeklyQuota(): Boolean {
    return any { resource ->
        resource.key in WeeklyPlanAnchorResourceKeys &&
            (resource.weeklyWindow?.total ?: 0L) > 0L
    }
}

private fun QuotaResource.effectiveRemainingCount(planHasWeeklyQuota: Boolean): Int {
    return if (hasVisibleWeeklyQuota(planHasWeeklyQuota)) {
        minOf(intervalRemainingCount(), weeklyRemainingCount())
    } else {
        intervalRemainingCount()
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

private fun QuotaResource.effectiveUsageProgress(planHasWeeklyQuota: Boolean): Float {
    return if (hasVisibleWeeklyQuota(planHasWeeklyQuota)) {
        max(intervalUsageProgress(), weeklyWindow?.usageProgress() ?: 0f)
    } else {
        intervalUsageProgress()
    }
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

private fun List<QuotaResource>.genericDominantRisk(): QuotaRisk {
    val maxProgress = flatMap { it.windows }.maxOfOrNull { it.usageProgress() } ?: 0f
    return when {
        maxProgress >= 0.95f -> QuotaRisk.Critical
        maxProgress >= 0.80f -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
    }
}

private val QuotaResource.intervalWindow: QuotaWindow?
    get() = windows.firstOrNull { it.scope == WindowScope.Interval }

private val QuotaResource.weeklyWindow: QuotaWindow?
    get() = windows.firstOrNull { it.scope == WindowScope.Weekly }

private fun QuotaWindow.usageProgress(): Float {
    val totalValue = total ?: return 0f
    val usedValue = used ?: return 0f
    return if (totalValue > 0) {
        usedValue.toFloat() / totalValue.toFloat()
    } else {
        0f
    }
}

private val WeeklyPlanAnchorResourceKeys = setOf(
    "MiniMax-M*",
    "coding-plan-vlm",
    "coding-plan-search"
)
