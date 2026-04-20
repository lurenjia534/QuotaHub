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

data class QuotaSnapshotEntities(
    val snapshot: QuotaSnapshotEntity,
    val resources: List<QuotaResourceEntity>,
    val windows: List<QuotaWindowEntity>
)

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

private data class LocalResourceAccumulator(
    val key: String,
    val title: String,
    val type: ResourceType,
    val role: ResourceRole?,
    val bucket: String?,
    val displayOrder: Int,
    val windows: MutableList<LocalWindowAccumulator> = mutableListOf()
)

private data class LocalWindowAccumulator(
    val displayOrder: Int,
    val window: QuotaWindow
)

private fun String?.toResourceType(): ResourceType {
    return runCatching { ResourceType.valueOf(this ?: ResourceType.Model.name) }
        .getOrDefault(ResourceType.Model)
}

private fun String?.toResourceRole(): ResourceRole? {
    return this?.let {
        runCatching { ResourceRole.valueOf(it) }.getOrNull()
    }
}

private fun String?.toWindowScope(): WindowScope {
    return runCatching { WindowScope.valueOf(this ?: WindowScope.Interval.name) }
        .getOrDefault(WindowScope.Interval)
}

private fun String?.toQuotaUnit(): QuotaUnit {
    return runCatching { QuotaUnit.valueOf(this ?: QuotaUnit.Request.name) }
        .getOrDefault(QuotaUnit.Request)
}
