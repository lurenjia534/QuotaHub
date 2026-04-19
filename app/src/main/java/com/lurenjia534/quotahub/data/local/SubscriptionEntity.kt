package com.lurenjia534.quotahub.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 订阅实体类 - 代表用户配置的一个AI服务提供商订阅
 *
 * 在重构后，系统不再使用单一的全局API Key，而是支持多个订阅。
 * 每个订阅包含：
 * - providerId: 服务提供商标识符（如 "minimax"）
 * - customTitle: 用户自定义的显示标题（可选）
 * - apiKey: 该订阅对应的API密钥
 * - createdAt: 订阅创建时间戳
 *
 * 当前结构支持用户同时管理多个订阅，并为多服务商扩展预留了空间。
 */
@Entity(
    tableName = "subscription",
    indices = [Index(value = ["providerId"])]
)
data class SubscriptionEntity(
    /** 主键，自增生成 */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 服务提供商ID，用于关联到具体的AI服务商 */
    val providerId: String,
    /** 用户自定义的显示标题，如果为空则使用默认值 */
    val customTitle: String?,
    /** 加密后的API密钥载荷，用于调用服务提供商的接口 */
    val apiKey: String,
    /** 订阅创建时间，用于排序和显示 */
    val createdAt: Long = System.currentTimeMillis()
)
