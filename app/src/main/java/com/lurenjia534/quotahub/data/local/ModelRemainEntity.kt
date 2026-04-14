package com.lurenjia534.quotahub.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lurenjia534.quotahub.data.model.ModelRemain

@Entity(
    tableName = "model_remain",
    primaryKeys = ["subscriptionId", "modelName"]
)
data class ModelRemainEntity(
    val subscriptionId: Long,
    val modelName: String,
    val displayOrder: Int,
    val startTime: Long,
    val endTime: Long,
    val remainsTime: Long,
    val currentIntervalTotalCount: Int,
    val currentIntervalUsageCount: Int,
    val currentWeeklyTotalCount: Int,
    val currentWeeklyUsageCount: Int,
    val weeklyStartTime: Long,
    val weeklyEndTime: Long,
    val weeklyRemainsTime: Long,
    val cachedAt: Long = System.currentTimeMillis()
)

fun ModelRemainEntity.toModelRemain(): ModelRemain {
    return ModelRemain(
        startTime = startTime,
        endTime = endTime,
        remainsTime = remainsTime,
        currentIntervalTotalCount = currentIntervalTotalCount,
        currentIntervalUsageCount = currentIntervalUsageCount,
        modelName = modelName,
        currentWeeklyTotalCount = currentWeeklyTotalCount,
        currentWeeklyUsageCount = currentWeeklyUsageCount,
        weeklyStartTime = weeklyStartTime,
        weeklyEndTime = weeklyEndTime,
        weeklyRemainsTime = weeklyRemainsTime
    )
}

fun ModelRemain.toEntity(
    subscriptionId: Long,
    displayOrder: Int,
    cachedAt: Long = System.currentTimeMillis()
): ModelRemainEntity {
    return ModelRemainEntity(
        subscriptionId = subscriptionId,
        modelName = modelName,
        displayOrder = displayOrder,
        startTime = startTime,
        endTime = endTime,
        remainsTime = remainsTime,
        currentIntervalTotalCount = currentIntervalTotalCount,
        currentIntervalUsageCount = currentIntervalUsageCount,
        currentWeeklyTotalCount = currentWeeklyTotalCount,
        currentWeeklyUsageCount = currentWeeklyUsageCount,
        weeklyStartTime = weeklyStartTime,
        weeklyEndTime = weeklyEndTime,
        weeklyRemainsTime = weeklyRemainsTime,
        cachedAt = cachedAt
    )
}
