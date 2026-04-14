package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.repository.SubscriptionRepository
import kotlinx.coroutines.flow.combine

/**
 * MiniMax订阅网关实现
 *
 * 负责与MiniMax AI服务提供商进行交互。
 * 组合了订阅数据和仓库操作，提供统一的配额管理接口。
 *
 * 工作流程：
 * 1. 初始化时持有订阅数据副本
 * 2. snapshot通过组合订阅信息和模型配额实现实时更新
 * 3. refresh()调用Repository从MiniMax API获取最新配额
 * 4. disconnect()删除订阅，清理相关数据
 *
 * @param subscriptionData 初始订阅数据
 * @param repository 数据仓库，用于执行订阅和配额相关的数据库操作
 */
class MiniMaxSubscriptionGateway(
    private val subscriptionData: Subscription,
    private val repository: SubscriptionRepository
) : SubscriptionGateway {
    /** 持有订阅数据的引用 */
    override val subscription: Subscription = subscriptionData

    /**
     * 订阅快照流
     *
     * 使用combine操作符同时监听：
     * - 订阅信息变化（可能由其他途径修改）
     * - 模型配额变化（刷新后的新数据）
     *
     * 当任一数据源变化时，都会生成新的Snapshot
     */
    override val snapshot = combine(
        repository.getSubscription(subscriptionData.id),
        repository.getModelRemains(subscriptionData.id)
    ) { sub, modelRemains ->
        SubscriptionSnapshot(
            subscription = sub ?: subscriptionData,
            modelRemains = modelRemains
        )
    }

    /**
     * 刷新配额数据
     *
     * 使用Bearer Token认证方式调用MiniMax API。
     * 将获取的配额数据缓存到本地数据库。
     *
     * @return 操作结果，成功时Unit，失败时返回包含异常的Result
     */
    override suspend fun refresh(): Result<Unit> {
        return repository.refreshQuota(subscriptionData.id, "Bearer ${subscriptionData.apiKey}")
    }

    /**
     * 重命名订阅
     *
     * 更新订阅的自定义显示名称。
     * 如果传入null，则移除自定义名称。
     *
     * @param customTitle 新的自定义名称，null表示使用默认名称
     * @return 操作结果，成功时Unit，失败时返回包含异常的Result
     */
    override suspend fun rename(customTitle: String?): Result<Unit> {
        return repository.updateSubscriptionTitle(subscriptionData.id, customTitle)
    }

    /**
     * 断开连接
     *
     * 删除该订阅及其关联的所有数据。
     * 包括本地缓存的模型配额信息。
     */
    override suspend fun disconnect() {
        repository.deleteSubscription(subscriptionData.id)
    }
}
