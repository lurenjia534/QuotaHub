package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.data.repository.SubscriptionRepository
import com.lurenjia534.quotahub.ui.screens.home.QuotaProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * 订阅卡片数据类
 *
 * 用于UI展示订阅列表的简化数据结构。
 * 包含订阅的核心信息和汇总统计。
 *
 * @param subscription 订阅信息
 * @param modelCount 该订阅包含的模型数量
 * @param remainingCalls 当前周期的剩余调用次数
 * @param remainingTime 最长的剩余时间
 * @param isConnected 是否已连接
 */
data class SubscriptionCard(
    val subscription: com.lurenjia534.quotahub.data.model.Subscription,
    val modelCount: Int,
    val remainingCalls: Int,
    val remainingTime: Long?,
    val isConnected: Boolean
)

/**
 * 订阅注册中心
 *
 * 应用层核心组件，负责创建订阅Gateway并聚合订阅视图数据。
 *
 * 主要职责：
 * 1. 维护可用提供商列表
 * 2. 提供订阅卡片流（用于UI展示列表）
 * 3. 创建和管理Gateway实例
 * 4. 处理订阅的创建、验证和删除
 *
 * 设计模式：工厂模式 + 观察者模式
 * - 工厂模式：通过getGateway创建具体的Gateway实例
 * - 观察者模式：通过Flow暴露订阅状态变化
 *
 * @param repository 数据仓库，提供订阅和配额的数据操作
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionRegistry(
    private val repository: SubscriptionRepository
) {
    /**
     * 获取所有可用的AI服务提供商列表
     */
    val providers: List<QuotaProvider>
        get() = QuotaProvider.values().toList()

    /**
     * 订阅卡片流
     *
     * 这是一个动态更新的Flow，包含了所有订阅的汇总信息。
     * 当订阅列表或任一订阅的配额变化时，会自动推送更新。
     *
     * 实现细节：
     * - 使用flatMapLatest响应订阅列表的变化
     * - 使用combine聚合所有订阅的配额数据
     * - 每个订阅的卡片信息包含：模型数量、总剩余调用数、最大剩余时间
     */
    val snapshots: Flow<List<SubscriptionCard>> = repository.subscriptions.flatMapLatest { subs ->
        if (subs.isEmpty()) {
            // 无订阅时返回空列表
            flowOf(emptyList())
        } else {
            // 为每个订阅创建配额监听流，然后合并
            combine(subs.map { sub ->
                repository.getModelRemains(sub.id).map { modelRemains ->
                    SubscriptionCard(
                        subscription = sub,
                        modelCount = modelRemains.size,
                        remainingCalls = modelRemains.sumOf { it.currentIntervalUsageCount },
                        remainingTime = modelRemains.map { it.remainsTime }.maxOrNull(),
                        isConnected = true
                    )
                }
            }) { it.toList() }
        }
    }

    /**
     * 根据订阅对象获取Gateway实例
     *
     * @param subscription 订阅对象
     * @return 对应的SubscriptionGateway实例
     */
    fun getGateway(subscription: com.lurenjia534.quotahub.data.model.Subscription): SubscriptionGateway {
        return MiniMaxSubscriptionGateway(subscription, repository)
    }

    /**
     * 根据订阅ID获取Gateway实例
     *
     * @param subscriptionId 订阅ID
     * @return Gateway实例，如果订阅不存在则返回null
     */
    suspend fun getGatewayById(subscriptionId: Long): SubscriptionGateway? {
        val subscription = repository.getSubscriptionOnce(subscriptionId) ?: return null
        return getGateway(subscription)
    }

    /**
     * 创建新订阅
     *
     * 直接创建订阅记录，不验证API Key的有效性。
     * 适用于已验证过的API Key或离线创建场景。
     *
     * @param provider 服务提供商
     * @param customTitle 自定义标题（可选）
     * @param apiKey API密钥
     * @return 新创建的订阅ID
     */
    suspend fun createSubscription(
        provider: QuotaProvider,
        customTitle: String?,
        apiKey: String
    ): Long {
        return repository.createSubscription(provider, customTitle, apiKey)
    }

    /**
     * 验证并创建订阅
     *
     * 在创建订阅前先验证API Key的有效性。
     * 验证成功后才真正创建订阅并缓存配额数据。
     *
     * 一致性处理：
     * - 如果配额缓存失败，会自动删除已创建的订阅
     * - 确保不会出现"僵尸订阅"（无数据的订阅）
     *
     * @param provider 服务提供商
     * @param customTitle 自定义标题（可选）
     * @param apiKey API密钥
     * @return 操作结果，成功时返回订阅ID，失败时返回异常
     */
    suspend fun validateAndCreateSubscription(
        provider: QuotaProvider,
        customTitle: String?,
        apiKey: String
    ): Result<Long> {
        return repository.validateApiKey(provider, apiKey).mapCatching { quotaResponse ->
            val subscriptionId = repository.createSubscription(provider, customTitle, apiKey)
            try {
                // 尝试缓存配额数据
                repository.cacheQuotaResponse(subscriptionId, quotaResponse)
                subscriptionId
            } catch (error: Exception) {
                // 缓存失败，撤销创建的订阅
                repository.deleteSubscription(subscriptionId)
                throw error
            }
        }
    }

    /**
     * 删除订阅
     *
     * 同时删除订阅及其关联的配额数据。
     *
     * @param subscriptionId 要删除的订阅ID
     */
    suspend fun deleteSubscription(subscriptionId: Long) {
        repository.deleteSubscription(subscriptionId)
    }
}
