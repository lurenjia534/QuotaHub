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
 * - apiKey: 加密后的凭证载荷（当前沿用旧列名以避免数据库迁移）
 * - syncState: 最近一次同步的健康状态
 * - lastSuccessAt/lastFailureAt/lastError/syncStartedAt: 用于展示同步历史和恢复中断同步
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
    /** 加密后的凭证载荷，用于调用服务提供商的接口 */
    val apiKey: String,
    /** 最近一次同步状态 */
    val syncState: String = "NeverSynced",
    /** 最近一次同步成功时间 */
    val lastSuccessAt: Long? = null,
    /** 最近一次同步失败时间 */
    val lastFailureAt: Long? = null,
    /** 最近一次同步失败类别 */
    val lastFailureKind: String? = null,
    /** 最近一次同步错误信息 */
    val lastError: String? = null,
    /** provider 指定的 retry-after 截止时间 */
    val retryAfterUntil: Long? = null,
    /** 自动刷新最早允许再次尝试的时间 */
    val nextEligibleSyncAt: Long? = null,
    /** 最近一次同步开始时间，用于回收中断的 Syncing 状态 */
    val syncStartedAt: Long? = null,
    /** 最近一次同步触发原因 */
    val lastSyncCause: String? = null,
    /** 订阅创建时间，用于排序和显示 */
    val createdAt: Long = System.currentTimeMillis()
)
