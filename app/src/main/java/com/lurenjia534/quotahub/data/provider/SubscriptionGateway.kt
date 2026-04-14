package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.data.model.ModelRemain
import com.lurenjia534.quotahub.data.model.Subscription
import kotlinx.coroutines.flow.Flow

/**
 * 订阅快照数据类
 *
 * 封装了订阅及其模型配额的当前状态。
 * 用于UI层展示订阅的完整信息。
 *
 * @param subscription 订阅信息
 * @param modelRemains 该订阅下各模型的剩余配额列表
 */
data class SubscriptionSnapshot(
    val subscription: Subscription,
    val modelRemains: List<ModelRemain> = emptyList()
)

/**
 * 订阅网关接口
 *
 * 定义与AI服务提供商交互的抽象接口。
 * 不同的服务提供商（如MiniMax、OpenAI等）可以实现此接口。
 *
 * 设计模式：策略模式 - 将不同提供商的实现细节抽象到具体Gateway类
 *
 * 主要功能：
 * 1. 提供订阅的实时快照（包含订阅信息和配额数据）
 * 2. 刷新配额（从服务端拉取最新数据）
 * 3. 断开连接（删除订阅）
 */
interface SubscriptionGateway {
    /**
     * 获取关联的订阅信息
     */
    val subscription: Subscription

    /**
     * 获取订阅快照流
     *
     * 这是一个持续的Flow，当订阅或配额数据变化时会自动推送新值。
     * UI层可以监听此Flow来实现数据的自动更新。
     */
    val snapshot: Flow<SubscriptionSnapshot>

    /**
     * 刷新配额数据
     *
     * 从AI服务提供商获取最新的模型配额信息。
     * @return 操作结果，成功时Unit，失败时包含异常信息
     */
    suspend fun refresh(): Result<Unit>

    /**
     * 断开连接
     *
     * 删除该订阅及其相关数据。
     * 调用后此Gateway实例可能不再可用。
     */
    suspend fun disconnect()
}
