package com.lurenjia534.quotahub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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