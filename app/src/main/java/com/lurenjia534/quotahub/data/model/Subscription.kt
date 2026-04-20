package com.lurenjia534.quotahub.data.model

import com.lurenjia534.quotahub.data.local.SubscriptionEntity
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor

sealed interface CredentialState {
    data object Available : CredentialState

    data class Broken(val reason: String? = null) : CredentialState

    data class Missing(val reason: String? = null) : CredentialState
}

class CredentialUnavailableException(
    message: String
) : IllegalStateException(message)

sealed interface SubscriptionProvider {
    val id: String
    val displayName: String

    data class Supported(
        val descriptor: ProviderDescriptor
    ) : SubscriptionProvider {
        override val id: String
            get() = descriptor.id

        override val displayName: String
            get() = descriptor.displayName
    }

    data class Unsupported(
        override val id: String
    ) : SubscriptionProvider {
        override val displayName: String
            get() = id
    }
}

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
 * @param credentialState 该订阅对应的凭证状态
 * @param syncStatus 当前订阅的同步健康状态
 * @param createdAt 创建时间戳
 */
data class Subscription(
    val id: Long,
    val provider: SubscriptionProvider,
    val customTitle: String?,
    val credentialState: CredentialState,
    val syncStatus: SubscriptionSyncStatus,
    val createdAt: Long
) {
    /**
     * 显示标题
     *
     * 如果用户设置了自定义标题则使用自定义标题，
     * 否则使用格式 "{提供商显示名} #{订阅ID}"
     * 例如："MiniMax Coding Plan #1"
     */
    val displayTitle: String
        get() = customTitle?.takeIf { it.isNotBlank() }
            ?: "${provider.displayName} #${id}"

    val isProviderSupported: Boolean
        get() = provider is SubscriptionProvider.Supported

    val supportedProvider: ProviderDescriptor?
        get() = (provider as? SubscriptionProvider.Supported)?.descriptor

    val hasUsableCredentials: Boolean
        get() = isProviderSupported && credentialState is CredentialState.Available

    val credentialIssue: String?
        get() = when {
            !isProviderSupported ->
                "${provider.displayName} is unavailable in the current app build. Update the app or remove this subscription."
            else -> when (val state = credentialState) {
                is CredentialState.Available -> null
                is CredentialState.Broken -> state.reason
                is CredentialState.Missing -> state.reason
            }
        }

    fun requireSupportedProvider(): ProviderDescriptor {
        return supportedProvider
            ?: throw IllegalStateException("Unsupported provider: ${provider.id}")
    }
}

/**
 * 将数据库实体转换为领域模型
 *
 * 转换过程中会根据providerId解析对应的ProviderDescriptor。
 * 如果找不到对应的提供商（例如提供商ID无效），则返回null。
 *
 * @return 转换后的Subscription，如果提供商不存在则返回null
 */
fun SubscriptionEntity.toSubscription(
    provider: SubscriptionProvider,
    credentialState: CredentialState,
    syncStatus: SubscriptionSyncStatus
): Subscription {
    return Subscription(
        id = id,
        provider = provider,
        customTitle = customTitle,
        credentialState = credentialState,
        syncStatus = syncStatus,
        createdAt = createdAt
    )
}
