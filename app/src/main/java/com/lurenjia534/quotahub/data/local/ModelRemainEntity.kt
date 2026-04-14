package com.lurenjia534.quotahub.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lurenjia534.quotahub.data.model.ModelRemain

/**
 * 模型剩余配额实体类
 *
 * 存储从AI服务提供商获取的各模型配额信息。
 * 采用(subscriptionId, modelName)作为复合主键，确保每个订阅的每个模型只有一条记录。
 *
 * 字段说明：
 * - subscriptionId: 关联的订阅ID
 * - modelName: 模型名称
 * - displayOrder: 显示顺序，用于UI中排序
 * - startTime/endTime: 当前配额周期的开始和结束时间
 * - remainsTime: 剩余时间（毫秒）
 * - currentIntervalTotalCount: 当前周期总调用次数
 * - currentIntervalUsageCount: 当前周期剩余调用次数（沿用接口字段名）
 * - currentWeeklyTotalCount: 本周总调用次数
 * - currentWeeklyUsageCount: 本周剩余调用次数（沿用接口字段名）
 * - weeklyStartTime/weeklyEndTime: 本周的开始和结束时间
 * - weeklyRemainsTime: 本周剩余时间
 * - cachedAt: 数据缓存时间，用于判断数据新鲜度
 */
@Entity(
    tableName = "model_remain",
    primaryKeys = ["subscriptionId", "modelName"]
)
data class ModelRemainEntity(
    /** 关联的订阅ID */
    val subscriptionId: Long,
    /** 模型名称 */
    val modelName: String,
    /** 显示顺序，用于UI排序 */
    val displayOrder: Int,
    /** 当前配额周期的开始时间戳 */
    val startTime: Long,
    /** 当前配额周期的结束时间戳 */
    val endTime: Long,
    /** 剩余时间（毫秒） */
    val remainsTime: Long,
    /** 当前周期总调用次数上限 */
    val currentIntervalTotalCount: Int,
    /** 当前周期剩余的调用次数（沿用接口字段名） */
    val currentIntervalUsageCount: Int,
    /** 本周总调用次数上限 */
    val currentWeeklyTotalCount: Int,
    /** 本周剩余的调用次数（沿用接口字段名） */
    val currentWeeklyUsageCount: Int,
    /** 本周开始时间戳 */
    val weeklyStartTime: Long,
    /** 本周结束时间戳 */
    val weeklyEndTime: Long,
    /** 本周剩余时间（毫秒） */
    val weeklyRemainsTime: Long,
    /** 数据缓存时间，用于判断数据是否过期 */
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * 将数据库实体转换为领域模型
 *
 * 用于从持久层读取数据后，转换为业务层可用的ModelRemain对象。
 * 转换过程中会丢弃subscriptionId等存储细节，仅保留业务相关字段。
 */
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

/**
 * 将领域模型转换为数据库实体
 *
 * 用于将业务层数据写入持久层。
 * 需要额外提供subscriptionId（关联的订阅）和displayOrder（显示顺序）。
 *
 * @param subscriptionId 关联的订阅ID
 * @param displayOrder 显示顺序
 * @param cachedAt 缓存时间，默认为当前时间
 */
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
