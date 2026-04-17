package com.lurenjia534.quotahub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max

@Serializable
data class ModelRemain(
    @SerialName("start_time")
    val startTime: Long,
    @SerialName("end_time")
    val endTime: Long,
    @SerialName("remains_time")
    val remainsTime: Long,
    @SerialName("current_interval_total_count")
    val currentIntervalTotalCount: Int,
    @SerialName("current_interval_usage_count")
    val currentIntervalUsageCount: Int,
    @SerialName("model_name")
    val modelName: String,
    @SerialName("current_weekly_total_count")
    val currentWeeklyTotalCount: Int,
    @SerialName("current_weekly_usage_count")
    val currentWeeklyUsageCount: Int,
    @SerialName("weekly_start_time")
    val weeklyStartTime: Long,
    @SerialName("weekly_end_time")
    val weeklyEndTime: Long,
    @SerialName("weekly_remains_time")
    val weeklyRemainsTime: Long
)

// MiniMax returns remaining quota counts in the `*_usage_count` fields.
val ModelRemain.hasWeeklyQuota: Boolean
    get() = currentWeeklyTotalCount > 0

val ModelRemain.intervalRemainingCount: Int
    get() = currentIntervalUsageCount.coerceAtLeast(0)

val ModelRemain.intervalUsedCount: Int
    get() = (currentIntervalTotalCount - intervalRemainingCount).coerceAtLeast(0)

val ModelRemain.intervalUsageProgress: Float
    get() = if (currentIntervalTotalCount > 0) {
        intervalUsedCount.toFloat() / currentIntervalTotalCount.toFloat()
    } else {
        0f
    }

val ModelRemain.weeklyRemainingCount: Int
    get() = currentWeeklyUsageCount.coerceAtLeast(0)

val ModelRemain.weeklyUsedCount: Int
    get() = if (hasWeeklyQuota) {
        (currentWeeklyTotalCount - weeklyRemainingCount).coerceAtLeast(0)
    } else {
        0
    }

val ModelRemain.weeklyUsageProgress: Float
    get() = if (hasWeeklyQuota && currentWeeklyTotalCount > 0) {
        weeklyUsedCount.toFloat() / currentWeeklyTotalCount.toFloat()
    } else {
        0f
    }

private val WeeklyPlanAnchorModelNames = setOf(
    "MiniMax-M*",
    "coding-plan-vlm",
    "coding-plan-search"
)

val List<ModelRemain>.hasPlanLevelWeeklyQuota: Boolean
    get() = any { modelRemain ->
        modelRemain.modelName in WeeklyPlanAnchorModelNames &&
            modelRemain.currentWeeklyTotalCount > 0
    }

fun ModelRemain.hasVisibleWeeklyQuota(planHasWeeklyQuota: Boolean): Boolean {
    return planHasWeeklyQuota && hasWeeklyQuota
}

fun ModelRemain.effectiveRemainingCount(planHasWeeklyQuota: Boolean): Int {
    return if (hasVisibleWeeklyQuota(planHasWeeklyQuota)) {
        minOf(intervalRemainingCount, weeklyRemainingCount)
    } else {
        intervalRemainingCount
    }
}

fun ModelRemain.effectiveUsageProgress(planHasWeeklyQuota: Boolean): Float {
    return if (hasVisibleWeeklyQuota(planHasWeeklyQuota)) {
        max(intervalUsageProgress, weeklyUsageProgress)
    } else {
        intervalUsageProgress
    }
}

fun ModelRemain.relevantResetTime(planHasWeeklyQuota: Boolean): Long? {
    return buildList {
        if (remainsTime > 0) {
            add(remainsTime)
        }
        if (hasVisibleWeeklyQuota(planHasWeeklyQuota) && weeklyRemainsTime > 0) {
            add(weeklyRemainsTime)
        }
    }.minOrNull()
}

@Serializable
data class BaseResp(
    @SerialName("status_code")
    val statusCode: Int,
    @SerialName("status_msg")
    val statusMsg: String
)

@Serializable
data class ModelRemainResponse(
    @SerialName("model_remains")
    val modelRemains: List<ModelRemain>,
    @SerialName("base_resp")
    val baseResp: BaseResp
)

enum class QuotaRisk {
    Healthy,
    Watch,
    Critical
}

private const val WatchUsageThreshold = 0.80f
private const val CriticalUsageThreshold = 0.95f

fun ModelRemain.quotaRisk(planHasWeeklyQuota: Boolean): QuotaRisk {
    val usageProgress = effectiveUsageProgress(planHasWeeklyQuota)
    return when {
        usageProgress >= CriticalUsageThreshold -> QuotaRisk.Critical
        usageProgress >= WatchUsageThreshold -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
    }
}

fun List<ModelRemain>.dominantQuotaRisk(
    planHasWeeklyQuota: Boolean = hasPlanLevelWeeklyQuota
): QuotaRisk {
    return when {
        any { it.quotaRisk(planHasWeeklyQuota) == QuotaRisk.Critical } -> QuotaRisk.Critical
        any { it.quotaRisk(planHasWeeklyQuota) == QuotaRisk.Watch } -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
    }
}

fun List<ModelRemain>.atRiskModelCount(
    planHasWeeklyQuota: Boolean = hasPlanLevelWeeklyQuota
): Int {
    return count { it.quotaRisk(planHasWeeklyQuota) != QuotaRisk.Healthy }
}
