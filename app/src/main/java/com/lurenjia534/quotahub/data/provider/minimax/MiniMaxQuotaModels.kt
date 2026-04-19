package com.lurenjia534.quotahub.data.provider.minimax

import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.QuotaResource
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.QuotaUnit
import com.lurenjia534.quotahub.data.model.QuotaWindow
import com.lurenjia534.quotahub.data.model.ResourceType
import com.lurenjia534.quotahub.data.model.WindowScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max

@Serializable
data class MiniMaxModelQuota(
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

@Serializable
data class MiniMaxBaseResponse(
    @SerialName("status_code")
    val statusCode: Int,
    @SerialName("status_msg")
    val statusMsg: String
)

@Serializable
data class MiniMaxQuotaResponse(
    @SerialName("model_remains")
    val modelRemains: List<MiniMaxModelQuota>,
    @SerialName("base_resp")
    val baseResp: MiniMaxBaseResponse
)

fun MiniMaxQuotaResponse.toQuotaSnapshot(
    fetchedAt: Long = System.currentTimeMillis()
): QuotaSnapshot {
    return QuotaSnapshot(
        fetchedAt = fetchedAt,
        resources = modelRemains.map { quota ->
            QuotaResource(
                key = quota.modelName,
                title = quota.modelName,
                type = ResourceType.Model,
                windows = buildList {
                    add(
                        QuotaWindow(
                            scope = WindowScope.Interval,
                            total = quota.currentIntervalTotalCount.toLong(),
                            used = quota.intervalUsedCount.toLong(),
                            remaining = quota.intervalRemainingCount.toLong(),
                            resetsAt = quota.remainsTime,
                            startsAt = quota.startTime,
                            endsAt = quota.endTime,
                            unit = QuotaUnit.Request
                        )
                    )
                    if (quota.hasWeeklyQuota) {
                        add(
                            QuotaWindow(
                                scope = WindowScope.Weekly,
                                total = quota.currentWeeklyTotalCount.toLong(),
                                used = quota.weeklyUsedCount.toLong(),
                                remaining = quota.weeklyRemainingCount.toLong(),
                                resetsAt = quota.weeklyRemainsTime,
                                startsAt = quota.weeklyStartTime,
                                endsAt = quota.weeklyEndTime,
                                unit = QuotaUnit.Request
                            )
                        )
                    }
                }
            )
        }
    )
}

// MiniMax returns remaining quota counts in the `*_usage_count` fields.
val MiniMaxModelQuota.hasWeeklyQuota: Boolean
    get() = currentWeeklyTotalCount > 0

val MiniMaxModelQuota.intervalRemainingCount: Int
    get() = currentIntervalUsageCount.coerceAtLeast(0)

val MiniMaxModelQuota.intervalUsedCount: Int
    get() = (currentIntervalTotalCount - intervalRemainingCount).coerceAtLeast(0)

val MiniMaxModelQuota.intervalUsageProgress: Float
    get() = if (currentIntervalTotalCount > 0) {
        intervalUsedCount.toFloat() / currentIntervalTotalCount.toFloat()
    } else {
        0f
    }

val MiniMaxModelQuota.weeklyRemainingCount: Int
    get() = currentWeeklyUsageCount.coerceAtLeast(0)

val MiniMaxModelQuota.weeklyUsedCount: Int
    get() = if (hasWeeklyQuota) {
        (currentWeeklyTotalCount - weeklyRemainingCount).coerceAtLeast(0)
    } else {
        0
    }

val MiniMaxModelQuota.weeklyUsageProgress: Float
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

val List<MiniMaxModelQuota>.hasPlanLevelWeeklyQuota: Boolean
    get() = any { modelQuota ->
        modelQuota.modelName in WeeklyPlanAnchorModelNames &&
            modelQuota.currentWeeklyTotalCount > 0
    }

fun MiniMaxModelQuota.hasVisibleWeeklyQuota(planHasWeeklyQuota: Boolean): Boolean {
    return planHasWeeklyQuota && hasWeeklyQuota
}

fun MiniMaxModelQuota.effectiveRemainingCount(planHasWeeklyQuota: Boolean): Int {
    return if (hasVisibleWeeklyQuota(planHasWeeklyQuota)) {
        minOf(intervalRemainingCount, weeklyRemainingCount)
    } else {
        intervalRemainingCount
    }
}

fun MiniMaxModelQuota.effectiveUsageProgress(planHasWeeklyQuota: Boolean): Float {
    return if (hasVisibleWeeklyQuota(planHasWeeklyQuota)) {
        max(intervalUsageProgress, weeklyUsageProgress)
    } else {
        intervalUsageProgress
    }
}

fun MiniMaxModelQuota.relevantResetTime(planHasWeeklyQuota: Boolean): Long? {
    return buildList {
        if (remainsTime > 0) {
            add(remainsTime)
        }
        if (hasVisibleWeeklyQuota(planHasWeeklyQuota) && weeklyRemainsTime > 0) {
            add(weeklyRemainsTime)
        }
    }.minOrNull()
}

private const val WatchUsageThreshold = 0.80f
private const val CriticalUsageThreshold = 0.95f

fun MiniMaxModelQuota.quotaRisk(planHasWeeklyQuota: Boolean): QuotaRisk {
    val usageProgress = effectiveUsageProgress(planHasWeeklyQuota)
    return when {
        usageProgress >= CriticalUsageThreshold -> QuotaRisk.Critical
        usageProgress >= WatchUsageThreshold -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
    }
}

fun List<MiniMaxModelQuota>.dominantQuotaRisk(
    planHasWeeklyQuota: Boolean = hasPlanLevelWeeklyQuota
): QuotaRisk {
    return when {
        any { it.quotaRisk(planHasWeeklyQuota) == QuotaRisk.Critical } -> QuotaRisk.Critical
        any { it.quotaRisk(planHasWeeklyQuota) == QuotaRisk.Watch } -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
    }
}

fun List<MiniMaxModelQuota>.atRiskModelCount(
    planHasWeeklyQuota: Boolean = hasPlanLevelWeeklyQuota
): Int {
    return count { it.quotaRisk(planHasWeeklyQuota) != QuotaRisk.Healthy }
}
