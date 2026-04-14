package com.lurenjia534.quotahub.data.model

import com.lurenjia534.quotahub.data.local.SubscriptionEntity
import com.lurenjia534.quotahub.ui.screens.home.QuotaProvider

/**
 * 订阅领域模型
 *
 * 代表用户配置的一个AI服务订阅，是业务层的核心实体类。
 * 与SubscriptionEntity的区别：
 * - Entity是数据库持久化对象，包含技术细节（如数据库ID）
 * - Subscription是业务领域对象，包含业务逻辑（如显示标题计算）
 *
 * @param id 订阅的唯一标识符
 * @param provider AI服务提供商
 * @param customTitle 用户自定义显示标题（可选）
 * @param apiKey API密钥
 * @param createdAt 创建时间戳
 */
data class Subscription(
    val id: Long,
    val provider: QuotaProvider,
    val customTitle: String?,
    val apiKey: String,
    val createdAt: Long
) {
    /**
     * 显示标题
     *
     * 如果用户设置了自定义标题则使用自定义标题，
     * 否则使用格式 "{提供商标题} #{订阅ID}"
     * 例如："MiniMax Coding Plan #1"
     */
    val displayTitle: String
        get() = customTitle?.takeIf { it.isNotBlank() }
            ?: "${provider.title} #${id}"

    /**
     * 副标题
     *
     * 用于UI显示，格式为 "{提供商标题} • {提供商副标题}"
     * 例如："MiniMax Coding Plan • minimaxi.com"
     */
    val subtitle: String
        get() = "${provider.title} • ${provider.subtitle}"
}

/**
 * 将数据库实体转换为领域模型
 *
 * 转换过程中会根据providerId查找对应的QuotaProvider。
 * 如果找不到对应的提供商（例如提供商ID无效），则返回null。
 *
 * @return 转换后的Subscription，如果提供商不存在则返回null
 */
fun SubscriptionEntity.toSubscription(): Subscription? {
    // 根据providerId查找提供商枚举
    val provider = QuotaProvider.fromId(providerId) ?: return null
    return Subscription(
        id = id,
        provider = provider,
        customTitle = customTitle,
        apiKey = apiKey,
        createdAt = createdAt
    )
}
