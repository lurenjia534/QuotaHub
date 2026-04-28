package com.lurenjia534.quotahub.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lurenjia534.quotahub.data.model.QuotaResource
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.QuotaUnit
import com.lurenjia534.quotahub.data.model.QuotaWindow
import com.lurenjia534.quotahub.data.model.ResourceRole
import com.lurenjia534.quotahub.data.model.ResourceType
import com.lurenjia534.quotahub.data.model.WindowScope

/**
 * 配额快照实体类
 *
 * 存储从API获取的配额快照的元数据信息。
 * 每个订阅(Subscription)对应一个快照记录。
 *
 * @param subscriptionId 订阅ID，作为主键，用于关联到SubscriptionEntity
 * @param fetchedAt 快照抓取时间，Unix时间戳（毫秒）
 * @param rawPayloadJson 原始API响应的JSON字符串，用于调试和重放
 * @param rawPayloadFormat 原始数据的格式版本（如"v1"、"v2"等），便于数据迁移
 * @param normalizerVersion 规范化处理器的版本号，用于确保数据转换的一致性
 */
@Entity(
    tableName = "quota_snapshot",
    foreignKeys = [
        ForeignKey(
            entity = SubscriptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["subscriptionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subscriptionId"])]
)
data class QuotaSnapshotEntity(
    @PrimaryKey
    val subscriptionId: Long,
    val fetchedAt: Long,
    val rawPayloadJson: String? = null,
    val rawPayloadFormat: String? = null,
    val normalizerVersion: Int? = null
)

/**
 * 配额资源实体类
 *
 * 表示一个具体的配额资源项目，如模型额度、套餐额度、功能额度等。
 * 资源隶属于某个快照，通过subscriptionId关联。
 *
 * @param subscriptionId 所属快照的订阅ID
 * @param resourceKey 资源的唯一标识键（如"gpt-4-tokens"、"dall-e-images"等）
 * @param title 资源的显示名称，用于UI展示
 * @param type 资源类型，字符串形式存储枚举值（如"Model"、"Plan"、"Feature"等）
 * @param role 资源角色，标识该资源在订阅中的角色（如"Limit"、"Contributor"等）
 * @param bucket 资源的存储桶/分组名称，用于组织和归类
 * @param displayOrder 资源在列表中的显示顺序，数值越小越靠前
 */
@Entity(
    tableName = "quota_resource",
    primaryKeys = ["subscriptionId", "resourceKey"],
    foreignKeys = [
        ForeignKey(
            entity = QuotaSnapshotEntity::class,
            parentColumns = ["subscriptionId"],
            childColumns = ["subscriptionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subscriptionId"])]
)
data class QuotaResourceEntity(
    val subscriptionId: Long,
    val resourceKey: String,
    val title: String,
    val type: String,
    val role: String?,
    val bucket: String?,
    val displayOrder: Int
)

/**
 * 配额窗口实体类
 *
 * 表示资源的时间窗口配额数据。每个资源可能有多个时间窗口，
 * 如"monthly"、"daily"、"rolling-24h"等。
 *
 * @param subscriptionId 所属快照的订阅ID
 * @param resourceKey 所属资源的唯一标识键
 * @param windowKey 窗口的唯一标识键（如"monthly"、"daily"等）
 * @param scope 窗口作用域类型（如"Interval"、"Daily"、"Weekly"、"Monthly"、"Rolling"等）
 * @param label 窗口的显示标签（如"本月"、"本周"等），用于UI展示
 * @param displayOrder 窗口在列表中的显示顺序
 * @param total 窗口周期内的总配额数量
 * @param used 已使用的配额数量
 * @param remaining 剩余可用配额数量
 * @param resetAtEpochMillis 配额重置时间的Unix时间戳（毫秒）
 * @param startsAt 窗口开始时间的Unix时间戳（毫秒）
 * @param endsAt 窗口结束时间的Unix时间戳（毫秒）
 * @param unit 配额单位（如"Request"表示请求次数，"Token"表示Token数量）
 */
@Entity(
    tableName = "quota_window",
    primaryKeys = ["subscriptionId", "resourceKey", "windowKey"],
    foreignKeys = [
        ForeignKey(
            entity = QuotaResourceEntity::class,
            parentColumns = ["subscriptionId", "resourceKey"],
            childColumns = ["subscriptionId", "resourceKey"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subscriptionId", "resourceKey"])]
)
data class QuotaWindowEntity(
    val subscriptionId: Long,
    val resourceKey: String,
    val windowKey: String,
    val scope: String,
    val label: String?,
    val displayOrder: Int,
    val total: Long?,
    val used: Long?,
    val remaining: Long?,
    val resetAtEpochMillis: Long?,
    val startsAt: Long?,
    val endsAt: Long?,
    val unit: String
)

/**
 * 配额快照行数据类（用于Room查询结果）
 *
 * 这是一个"扁平化"的数据结构，用于接收Room查询返回的关联结果。
 * 当使用JOIN查询时，所有相关表的数据会被合并到这个单一的行对象中。
 *
 * 该类的命名遵循Room的最佳实践，以"Row"结尾表示它是查询结果的载体。
 *
 * @param snapshotSubscriptionId 快照所属的订阅ID
 * @param fetchedAt 快照抓取时间
 * @param resourceKey 资源唯一标识键（可空，因为某些快照可能没有资源）
 * @param title 资源显示名称
 * @param resourceType 资源类型字符串
 * @param resourceRole 资源角色字符串
 * @param resourceBucket 资源存储桶名称
 * @param resourceDisplayOrder 资源显示顺序
 * @param windowKey 窗口唯一标识键
 * @param scope 窗口作用域类型
 * @param windowLabel 窗口显示标签
 * @param windowDisplayOrder 窗口显示顺序
 * @param total 窗口总配额
 * @param used 已使用配额
 * @param remaining 剩余配额
 * @param resetAtEpochMillis 配额重置时间
 * @param startsAt 窗口开始时间
 * @param endsAt 窗口结束时间
 * @param unit 配额单位
 */
data class QuotaSnapshotRow(
    val snapshotSubscriptionId: Long,
    val fetchedAt: Long,
    val resourceKey: String?,
    val title: String?,
    val resourceType: String?,
    val resourceRole: String?,
    val resourceBucket: String?,
    val resourceDisplayOrder: Int?,
    val windowKey: String?,
    val scope: String?,
    val windowLabel: String?,
    val windowDisplayOrder: Int?,
    val total: Long?,
    val used: Long?,
    val remaining: Long?,
    val resetAtEpochMillis: Long?,
    val startsAt: Long?,
    val endsAt: Long?,
    val unit: String?
)

/**
 * 配额快照实体容器类
 *
 * 将三个相关的实体类组合在一起，方便在数据层之间传递完整的快照数据。
 * 这个容器类提供了一种类型安全的方式来传递完整的配额快照信息。
 *
 * @param snapshot 配额快照的元数据实体
 * @param resources 该快照包含的所有资源实体列表
 * @param windows 所有资源包含的所有窗口实体列表
 */
data class QuotaSnapshotEntities(
    val snapshot: QuotaSnapshotEntity,
    val resources: List<QuotaResourceEntity>,
    val windows: List<QuotaWindowEntity>
)

/**
 * 将业务模型转换为数据库实体
 *
 * 这是一个扩展函数，负责将内存中的业务模型(QuotaSnapshot)转换为
 * 可以存储到Room数据库的实体对象。
 *
 * 转换过程：
 * 1. 将QuotaSnapshot转换为QuotaSnapshotEntity（快照元数据）
 * 2. 将每个QuotaResource转换为QuotaResourceEntity（资源信息）
 * 3. 将每个QuotaResource下的所有QuotaWindow转换为QuotaWindowEntity（窗口数据）
 *
 * 注意：此函数只做单向转换，不包含反向转换逻辑。
 *
 * @param subscriptionId 订阅ID，用于关联所有实体
 * @param rawPayloadJson 原始API响应的JSON字符串
 * @param rawPayloadFormat 原始数据的格式版本
 * @param normalizerVersion 规范化处理器的版本号
 * @return 包含所有实体数据的容器对象
 */
fun QuotaSnapshot.toEntities(
    subscriptionId: Long,
    rawPayloadJson: String? = null,
    rawPayloadFormat: String? = null,
    normalizerVersion: Int? = null
): QuotaSnapshotEntities {
    val snapshotEntity = QuotaSnapshotEntity(
        subscriptionId = subscriptionId,
        fetchedAt = fetchedAt,
        rawPayloadJson = rawPayloadJson,
        rawPayloadFormat = rawPayloadFormat,
        normalizerVersion = normalizerVersion
    )
    val resourceEntities = resources.mapIndexed { resourceIndex, resource ->
        QuotaResourceEntity(
            subscriptionId = subscriptionId,
            resourceKey = resource.key,
            title = resource.title,
            type = resource.type.name,
            role = resource.role?.name,
            bucket = resource.bucket,
            displayOrder = resourceIndex
        )
    }
    val windowEntities = resources.flatMap { resource ->
        resource.windows.mapIndexed { windowIndex, window ->
            QuotaWindowEntity(
                subscriptionId = subscriptionId,
                resourceKey = resource.key,
                windowKey = window.windowKey,
                scope = window.scope.name,
                label = window.label,
                displayOrder = windowIndex,
                total = window.total,
                used = window.used,
                remaining = window.remaining,
                resetAtEpochMillis = window.resetAtEpochMillis,
                startsAt = window.startsAt,
                endsAt = window.endsAt,
                unit = window.unit.name
            )
        }
    }
    return QuotaSnapshotEntities(
        snapshot = snapshotEntity,
        resources = resourceEntities,
        windows = windowEntities
    )
}

/**
 * 将扁平化的查询结果转换为业务模型
 *
 * 这是一个扩展函数，负责将Room查询返回的扁平化行数据(QuotaSnapshotRow列表)
 * 重新组装成业务层使用的QuotaSnapshot对象。
 *
 * 转换过程：
 * 1. 验证列表不为空，空则返回空快照对象
 * 2. 提取fetchedAt时间戳（假设同一快照的所有行时间戳一致）
 * 3. 使用linkedMapOf按resourceKey分组聚合资源数据
 * 4. 遍历每行数据，将资源信息和窗口信息分别填充到对应的累加器中
 * 5. 最后将聚合后的数据转换为QuotaSnapshot业务模型
 *
 * 使用linkedMapOf而非普通Map是为了保证：
 * - 键值对的插入顺序（资源显示顺序）
 * - 高效的查找和插入操作
 *
 * @return 重组后的QuotaSnapshot业务模型对象
 */
fun List<QuotaSnapshotRow>.toQuotaSnapshot(): QuotaSnapshot {
    if (isEmpty()) {
        return QuotaSnapshot.empty()
    }

    val fetchedAt = first().fetchedAt
    val resources = linkedMapOf<String, LocalResourceAccumulator>()
    for (row in this) {
        val resourceKey = row.resourceKey ?: continue
        val resource = resources.getOrPut(resourceKey) {
            LocalResourceAccumulator(
                key = resourceKey,
                title = row.title ?: resourceKey,
                type = row.resourceType.toResourceType(),
                role = row.resourceRole.toResourceRole(),
                bucket = row.resourceBucket,
                displayOrder = row.resourceDisplayOrder ?: Int.MAX_VALUE
            )
        }
        if (row.scope != null) {
            resource.windows += LocalWindowAccumulator(
                displayOrder = row.windowDisplayOrder ?: Int.MAX_VALUE,
                window = QuotaWindow(
                    windowKey = row.windowKey ?: row.scope.orEmpty(),
                    scope = row.scope.toWindowScope(),
                    label = row.windowLabel,
                    total = row.total,
                    used = row.used,
                    remaining = row.remaining,
                    resetAtEpochMillis = row.resetAtEpochMillis,
                    startsAt = row.startsAt,
                    endsAt = row.endsAt,
                    unit = row.unit.toQuotaUnit()
                )
            )
        }
    }

    return QuotaSnapshot(
        fetchedAt = fetchedAt,
        resources = resources.values
            .sortedBy { it.displayOrder }
            .map { resource ->
                QuotaResource(
                    key = resource.key,
                    title = resource.title,
                    type = resource.type,
                    role = resource.role,
                    bucket = resource.bucket,
                    windows = resource.windows
                        .sortedBy { it.displayOrder }
                        .map { it.window }
                )
            }
    )
}

/**
 * 本地资源累加器
 *
 * 这是一个私有辅助类，用于在将扁平化数据重组为业务模型的过程中，
 * 临时存储和聚合资源相关的数据。
 *
 * 使用累加器模式的好处是可以在遍历查询结果时逐步构建数据结构，
 * 避免多次查找和插入操作的复杂性。
 *
 * @param key 资源唯一标识键
 * @param title 资源显示名称
 * @param type 资源类型枚举值
 * @param role 资源角色枚举值
 * @param bucket 资源存储桶名称
 * @param displayOrder 资源显示顺序
 * @param windows 该资源包含的所有窗口累加器列表
 */
private data class LocalResourceAccumulator(
    val key: String,
    val title: String,
    val type: ResourceType,
    val role: ResourceRole?,
    val bucket: String?,
    val displayOrder: Int,
    val windows: MutableList<LocalWindowAccumulator> = mutableListOf()
)

/**
 * 本地窗口累加器
 *
 * 这是一个私有辅助类，用于在将扁平化数据重组为业务模型的过程中，
 * 临时存储窗口相关的数据。
 *
 * @param displayOrder 窗口显示顺序
 * @param window 窗口业务模型对象
 */
private data class LocalWindowAccumulator(
    val displayOrder: Int,
    val window: QuotaWindow
)

/**
 * 将字符串转换为ResourceType枚举
 *
 * 安全的类型转换函数，如果字符串无法转换为有效的枚举值，
 * 则返回默认的ResourceType.Model。
 *
 * @return 对应的ResourceType枚举值，转换失败时返回默认值
 */
private fun String?.toResourceType(): ResourceType {
    return runCatching { ResourceType.valueOf(this ?: ResourceType.Model.name) }
        .getOrDefault(ResourceType.Model)
}

/**
 * 将字符串转换为ResourceRole枚举（可空）
 *
 * 安全的类型转换函数，如果字符串无法转换为有效的枚举值，
 * 则返回null。这与ResourceRole的可空性质一致。
 *
 * @return 对应的ResourceRole枚举值，转换失败时返回null
 */
private fun String?.toResourceRole(): ResourceRole? {
    return this?.let {
        runCatching { ResourceRole.valueOf(it) }.getOrNull()
    }
}

/**
 * 将字符串转换为WindowScope枚举
 *
 * 安全的类型转换函数，如果字符串无法转换为有效的枚举值，
 * 则返回默认的WindowScope.Interval。
 *
 * @return 对应的WindowScope枚举值，转换失败时返回默认值
 */
private fun String?.toWindowScope(): WindowScope {
    return runCatching { WindowScope.valueOf(this ?: WindowScope.Interval.name) }
        .getOrDefault(WindowScope.Interval)
}

/**
 * 将字符串转换为QuotaUnit枚举
 *
 * 安全的类型转换函数，如果字符串无法转换为有效的枚举值，
 * 则返回默认的QuotaUnit.Request。
 *
 * @return 对应的QuotaUnit枚举值，转换失败时返回默认值
 */
private fun String?.toQuotaUnit(): QuotaUnit {
    return runCatching { QuotaUnit.valueOf(this ?: QuotaUnit.Request.name) }
        .getOrDefault(QuotaUnit.Request)
}
