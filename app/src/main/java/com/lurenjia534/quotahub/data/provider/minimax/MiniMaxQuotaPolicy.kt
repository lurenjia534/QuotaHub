package com.lurenjia534.quotahub.data.provider.minimax

import com.lurenjia534.quotahub.data.model.QuotaResource
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.QuotaWindow
import com.lurenjia534.quotahub.data.model.WindowScope
import kotlin.math.max

fun List<QuotaResource>.hasMiniMaxPlanLevelWeeklyQuota(): Boolean {
    return any { resource ->
        resource.key in WeeklyPlanAnchorResourceKeys &&
            (resource.weeklyWindow?.total ?: 0L) > 0L
    }
}

fun QuotaResource.hasVisibleWeeklyQuota(planHasWeeklyQuota: Boolean): Boolean {
    return planHasWeeklyQuota && (weeklyWindow?.total ?: 0L) > 0L
}

fun QuotaResource.intervalRemainingCount(): Int {
    return (intervalWindow?.remaining ?: 0L).toInt()
}

fun QuotaResource.intervalUsedCount(): Int {
    return (intervalWindow?.used ?: 0L).toInt()
}

fun QuotaResource.intervalUsageProgress(): Float {
    return intervalWindow?.usageProgress() ?: 0f
}

fun QuotaResource.weeklyRemainingCount(): Int {
    return (weeklyWindow?.remaining ?: 0L).toInt()
}

fun QuotaResource.weeklyUsedCount(): Int {
    return (weeklyWindow?.used ?: 0L).toInt()
}

fun QuotaResource.effectiveRemainingCount(planHasWeeklyQuota: Boolean): Int {
    return if (hasVisibleWeeklyQuota(planHasWeeklyQuota)) {
        minOf(intervalRemainingCount(), weeklyRemainingCount())
    } else {
        intervalRemainingCount()
    }
}

fun QuotaResource.effectiveUsageProgress(planHasWeeklyQuota: Boolean): Float {
    return if (hasVisibleWeeklyQuota(planHasWeeklyQuota)) {
        max(intervalUsageProgress(), weeklyWindow?.usageProgress() ?: 0f)
    } else {
        intervalUsageProgress()
    }
}

fun QuotaResource.relevantResetAt(planHasWeeklyQuota: Boolean): Long? {
    return buildList {
        intervalWindow?.resetAtEpochMillis?.takeIf { it > 0L }?.let(::add)
        if (hasVisibleWeeklyQuota(planHasWeeklyQuota)) {
            weeklyWindow?.resetAtEpochMillis?.takeIf { it > 0L }?.let(::add)
        }
    }.minOrNull()
}

fun QuotaResource.quotaRisk(planHasWeeklyQuota: Boolean): QuotaRisk {
    val usageProgress = effectiveUsageProgress(planHasWeeklyQuota)
    return when {
        usageProgress >= 0.95f -> QuotaRisk.Critical
        usageProgress >= 0.80f -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
    }
}

fun List<QuotaResource>.dominantQuotaRisk(planHasWeeklyQuota: Boolean): QuotaRisk {
    return when {
        any { it.quotaRisk(planHasWeeklyQuota) == QuotaRisk.Critical } -> QuotaRisk.Critical
        any { it.quotaRisk(planHasWeeklyQuota) == QuotaRisk.Watch } -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
    }
}

val QuotaResource.intervalWindow: QuotaWindow?
    get() = windows.firstOrNull { it.scope == WindowScope.Interval }

val QuotaResource.weeklyWindow: QuotaWindow?
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
