package com.lurenjia534.quotahub.data.repository

import android.util.Log
import com.lurenjia534.quotahub.data.api.MiniMaxApiClient
import com.lurenjia534.quotahub.data.local.ModelRemainDao
import com.lurenjia534.quotahub.data.local.SubscriptionDao
import com.lurenjia534.quotahub.data.local.SubscriptionEntity
import com.lurenjia534.quotahub.data.local.toEntity
import com.lurenjia534.quotahub.data.local.toModelRemain
import com.lurenjia534.quotahub.data.model.ModelRemain
import com.lurenjia534.quotahub.data.model.ModelRemainResponse
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.model.toSubscription
import com.lurenjia534.quotahub.data.security.ApiKeyCipher
import com.lurenjia534.quotahub.ui.screens.home.QuotaProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 订阅仓库类
 *
 * 应用层的数据仓库，负责协调本地数据库和远程API的数据操作。
 * 是数据访问层的统一入口，隔离了数据来源（本地/远程）的复杂性。
 *
 * 核心职责：
 * 1. 管理订阅的CRUD操作
 * 2. 管理模型配额的本地缓存
 * 3. 提供数据转换（Entity <-> Domain Model）
 * 4. 封装远程API调用
 *
 * 设计说明：
 * - 通过组合SubscriptionDao和ModelRemainDao访问本地数据库
 * - 通过MiniMaxApiClient访问远程API
 * - 使用Result类型封装可能失败的操作
 *
 * @param subscriptionDao 订阅数据访问对象
 * @param modelRemainDao 模型配额数据访问对象
 */
class SubscriptionRepository(
    private val subscriptionDao: SubscriptionDao,
    private val modelRemainDao: ModelRemainDao,
    private val apiKeyCipher: ApiKeyCipher
) {
    /**
     * 所有订阅的Flow流
     *
     * 监听数据库中所有订阅的变化，自动映射为领域模型。
     * 当providerId无效时，对应记录会在映射阶段被过滤掉。
     */
    val subscriptions: Flow<List<Subscription>> = subscriptionDao.getAllSubscriptions().map { entities ->
        entities.mapNotNull(::toSubscription)
    }

    /**
     * 监听指定订阅的变化
     *
     * @param subscriptionId 订阅ID
     * @return 订阅的Flow流，不存在时流中值为null
     */
    fun getSubscription(subscriptionId: Long): Flow<Subscription?> {
        return subscriptionDao.getSubscription(subscriptionId).map { it?.let(::toSubscription) }
    }

    /**
     * 一次性获取订阅（不监听变化）
     *
     * @param subscriptionId 订阅ID
     * @return 订阅对象，不存在则返回null
     */
    suspend fun getSubscriptionOnce(subscriptionId: Long): Subscription? {
        return subscriptionDao.getSubscriptionOnce(subscriptionId)?.let(::toSubscription)
    }

    /**
     * 获取订阅总数
     *
     * @return 当前订阅数量
     */
    suspend fun getSubscriptionCount(): Int {
        return subscriptionDao.getSubscriptionCount()
    }

    /**
     * 创建新订阅
     *
     * 将API Key和相关配置存储到数据库。
     * 会自动去除输入的空白字符。
     *
     * @param provider 服务提供商
     * @param customTitle 自定义标题（可选，会去除首尾空白）
     * @param apiKey API密钥（会去除首尾空白）
     * @return 新创建的订阅ID
     */
    suspend fun createSubscription(
        provider: QuotaProvider,
        customTitle: String?,
        apiKey: String
    ): Long {
        val entity = SubscriptionEntity(
            providerId = provider.id,
            customTitle = customTitle?.trim()?.takeIf { it.isNotBlank() },
            apiKey = apiKeyCipher.encrypt(apiKey.trim())
        )
        return subscriptionDao.insertSubscription(entity)
    }

    /**
     * 更新订阅信息
     *
     * @param subscription 包含更新后数据的订阅对象
     */
    suspend fun updateSubscription(subscription: Subscription) {
        val entity = SubscriptionEntity(
            id = subscription.id,
            providerId = subscription.provider.id,
            customTitle = subscription.customTitle?.trim()?.takeIf { it.isNotBlank() },
            apiKey = apiKeyCipher.encrypt(subscription.apiKey.trim()),
            createdAt = subscription.createdAt
        )
        subscriptionDao.updateSubscription(entity)
    }

    /**
     * 更新订阅的自定义标题
     *
     * 用于用户自定义订阅的显示名称。
     * 如果传入空字符串或空白字符，会被当作null处理，
     * 即移除自定义标题，使用默认名称。
     *
     * @param subscriptionId 订阅ID
     * @param customTitle 新的自定义标题，null或空白表示移除自定义标题
     * @return 操作结果，成功时Unit，失败时返回包含异常的Result
     */
    suspend fun updateSubscriptionTitle(subscriptionId: Long, customTitle: String?): Result<Unit> {
        return runCatching {
            val currentSubscription = subscriptionDao.getSubscriptionOnce(subscriptionId)
                ?: throw IllegalStateException("Subscription no longer exists")
            subscriptionDao.updateSubscription(
                currentSubscription.copy(
                    customTitle = customTitle?.trim()?.takeIf { it.isNotBlank() }
                )
            )
        }
    }

    /**
     * 删除订阅及其关联数据
     *
     * 先删除模型配额，再删除订阅记录。
     * 确保数据完整性。
     *
     * @param subscriptionId 要删除的订阅ID
     */
    suspend fun deleteSubscription(subscriptionId: Long) {
        // 先清除关联的模型配额数据
        modelRemainDao.clearModelRemainsBySubscription(subscriptionId)
        // 再删除订阅本身
        subscriptionDao.deleteSubscriptionById(subscriptionId)
    }

    /**
     * 获取订阅的模型配额流
     *
     * @param subscriptionId 订阅ID
     * @return 模型配额列表的Flow流
     */
    fun getModelRemains(subscriptionId: Long): Flow<List<ModelRemain>> {
        return modelRemainDao.getModelRemainsBySubscription(subscriptionId).map { entities ->
            entities.map { it.toModelRemain() }
        }
    }

    /**
     * 刷新订阅配额
     *
     * 从远程API获取最新配额，并在成功拿到响应后写入本地缓存。
     * 如果远程获取失败，不会影响已有缓存。
     *
     * @param subscriptionId 订阅ID
     * @param authorization 授权字符串，通常格式为 "Bearer {apiKey}"
     * @return 操作结果
     */
    suspend fun refreshQuota(subscriptionId: Long, authorization: String): Result<Unit> {
        return runCatching {
            val response = fetchQuota(authorization)
            cacheQuotaResponse(subscriptionId, response)
        }
    }

    /**
     * 验证API Key有效性
     *
     * 通过调用远程API验证Key是否有效。
     * 用于用户添加订阅时的预检验。
     *
     * @param provider 服务提供商（当前仅支持MiniMax）
     * @param apiKey 要验证的API密钥
     * @return 验证成功时返回配额响应，失败时返回异常
     */
    suspend fun validateApiKey(provider: QuotaProvider, apiKey: String): Result<ModelRemainResponse> {
        return runCatching {
            fetchQuota("Bearer ${apiKey.trim()}")
        }
    }

    /**
     * 缓存配额响应到本地数据库
     *
     * 先验证订阅存在，再清除旧数据，最后插入新数据。
     * 使用同一时间戳标记所有数据，保证数据一致性。
     *
     * @param subscriptionId 订阅ID
     * @param response 从API获取的配额响应
     */
    suspend fun cacheQuotaResponse(subscriptionId: Long, response: ModelRemainResponse) {
        // 确保订阅仍然存在
        ensureSubscriptionExists(subscriptionId)

        val cachedAt = System.currentTimeMillis()
        // 清除旧数据（为新数据腾出空间）
        modelRemainDao.clearModelRemainsBySubscription(subscriptionId)
        // 再次验证（清除操作后可能存在并发问题）
        ensureSubscriptionExists(subscriptionId)
        // 批量插入新数据，保持显示顺序
        modelRemainDao.upsertModelRemains(
            response.modelRemains.mapIndexed { index, modelRemain ->
                modelRemain.toEntity(
                    subscriptionId = subscriptionId,
                    displayOrder = index,
                    cachedAt = cachedAt
                )
            }
        )
    }

    /**
     * 从远程API获取配额数据
     *
     * @param authorization 授权字符串
     * @return 配额响应数据
     */
    private suspend fun fetchQuota(authorization: String): ModelRemainResponse {
        return MiniMaxApiClient.apiService.getModelRemains(authorization)
    }

    /**
     * 确保订阅仍然存在
     *
     * 用于在关键操作前验证数据完整性。
     * 如果订阅不存在，抛出异常中断操作。
     *
     * @param subscriptionId 要验证的订阅ID
     * @throws IllegalStateException 订阅不存在时抛出
     */
    private suspend fun ensureSubscriptionExists(subscriptionId: Long) {
        if (subscriptionDao.getSubscriptionOnce(subscriptionId) == null) {
            throw IllegalStateException("Subscription no longer exists")
        }
    }

    private fun toSubscription(entity: SubscriptionEntity): Subscription? {
        return runCatching {
            entity.toSubscription(apiKey = apiKeyCipher.decrypt(entity.apiKey))
        }.getOrElse { error ->
            Log.e(TAG, "Failed to decrypt API key for subscription ${entity.id}", error)
            null
        }
    }

    private companion object {
        private const val TAG = "SubscriptionRepository"
    }
}
