package com.lurenjia534.quotahub.data.provider.minimax

import com.lurenjia534.quotahub.data.model.QuotaBuckets
import com.lurenjia534.quotahub.data.model.QuotaResource
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.QuotaUnit
import com.lurenjia534.quotahub.data.model.QuotaWindow
import com.lurenjia534.quotahub.data.model.QuotaWindowLabels
import com.lurenjia534.quotahub.data.model.ResourceRole
import com.lurenjia534.quotahub.data.model.ResourceType
import com.lurenjia534.quotahub.data.model.WindowScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

internal class MiniMaxApiException(
    val statusCode: Int,
    statusMsg: String
) : IllegalStateException(
    statusMsg.takeIf { it.isNotBlank() }
        ?: "MiniMax request failed with status code $statusCode."
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
    baseResp.requireSuccess()
    return QuotaSnapshot(
        fetchedAt = fetchedAt,
        resources = modelRemains.map { quota ->
            QuotaResource(
                key = quota.modelName,
                title = quota.modelName,
                type = ResourceType.Model,
                role = if (quota.modelName in MiniMaxWeeklyPlanAnchorResourceKeys) {
                    ResourceRole.Anchor
                } else {
                    ResourceRole.Limit
                },
                bucket = QuotaBuckets.ModelCalls,
                windows = buildList {
                    add(
                        QuotaWindow(
                            windowKey = INTERVAL_WINDOW_KEY,
                            scope = WindowScope.Interval,
                            label = QuotaWindowLabels.Interval,
                            total = quota.currentIntervalTotalCount.toLong(),
                            used = quota.intervalUsedCount.toLong(),
                            remaining = quota.intervalRemainingCount.toLong(),
                            resetAtEpochMillis = quota.intervalResetAtEpochMillis,
                            startsAt = quota.startTime,
                            endsAt = quota.endTime,
                            unit = QuotaUnit.Request
                        )
                    )
                    if (quota.hasWeeklyQuota) {
                        add(
                            QuotaWindow(
                                windowKey = WEEKLY_WINDOW_KEY,
                                scope = WindowScope.Weekly,
                                label = QuotaWindowLabels.Weekly,
                                total = quota.currentWeeklyTotalCount.toLong(),
                                used = quota.weeklyUsedCount.toLong(),
                                remaining = quota.weeklyRemainingCount.toLong(),
                                resetAtEpochMillis = quota.weeklyResetAtEpochMillis,
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

private fun MiniMaxBaseResponse.requireSuccess() {
    if (statusCode != 0) {
        throw MiniMaxApiException(statusCode = statusCode, statusMsg = statusMsg)
    }
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

val MiniMaxModelQuota.intervalResetAtEpochMillis: Long?
    get() = endTime.takeIf { it > 0L }

val MiniMaxModelQuota.weeklyResetAtEpochMillis: Long?
    get() = weeklyEndTime.takeIf { it > 0L }

private const val INTERVAL_WINDOW_KEY = "interval"
private const val WEEKLY_WINDOW_KEY = "weekly"

internal val MiniMaxWeeklyPlanAnchorResourceKeys = setOf(
    "MiniMax-M*",
    "coding-plan-vlm",
    "coding-plan-search"
)
