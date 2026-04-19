package com.lurenjia534.quotahub.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lurenjia534.quotahub.data.model.QuotaResource
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.QuotaUnit
import com.lurenjia534.quotahub.data.model.QuotaWindow
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
    val fetchedAt: Long
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
    val displayOrder: Int
)

@Entity(
    tableName = "quota_window",
    primaryKeys = ["subscriptionId", "resourceKey", "scope"],
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
    val scope: String,
    val displayOrder: Int,
    val total: Long?,
    val used: Long?,
    val remaining: Long?,
    val resetsAt: Long?,
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
    val resourceDisplayOrder: Int?,
    val scope: String?,
    val windowDisplayOrder: Int?,
    val total: Long?,
    val used: Long?,
    val remaining: Long?,
    val resetsAt: Long?,
    val startsAt: Long?,
    val endsAt: Long?,
    val unit: String?
)

data class QuotaSnapshotEntities(
    val snapshot: QuotaSnapshotEntity,
    val resources: List<QuotaResourceEntity>,
    val windows: List<QuotaWindowEntity>
)

fun QuotaSnapshot.toEntities(subscriptionId: Long): QuotaSnapshotEntities {
    val snapshotEntity = QuotaSnapshotEntity(
        subscriptionId = subscriptionId,
        fetchedAt = fetchedAt
    )
    val resourceEntities = resources.mapIndexed { resourceIndex, resource ->
        QuotaResourceEntity(
            subscriptionId = subscriptionId,
            resourceKey = resource.key,
            title = resource.title,
            type = resource.type.name,
            displayOrder = resourceIndex
        )
    }
    val windowEntities = resources.flatMap { resource ->
        resource.windows.mapIndexed { windowIndex, window ->
            QuotaWindowEntity(
                subscriptionId = subscriptionId,
                resourceKey = resource.key,
                scope = window.scope.name,
                displayOrder = windowIndex,
                total = window.total,
                used = window.used,
                remaining = window.remaining,
                resetsAt = window.resetsAt,
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
                displayOrder = row.resourceDisplayOrder ?: Int.MAX_VALUE
            )
        }
        if (row.scope != null) {
            resource.windows += LocalWindowAccumulator(
                displayOrder = row.windowDisplayOrder ?: Int.MAX_VALUE,
                window = QuotaWindow(
                    scope = row.scope.toWindowScope(),
                    total = row.total,
                    used = row.used,
                    remaining = row.remaining,
                    resetsAt = row.resetsAt,
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

private fun String?.toWindowScope(): WindowScope {
    return runCatching { WindowScope.valueOf(this ?: WindowScope.Interval.name) }
        .getOrDefault(WindowScope.Interval)
}

private fun String?.toQuotaUnit(): QuotaUnit {
    return runCatching { QuotaUnit.valueOf(this ?: QuotaUnit.Request.name) }
        .getOrDefault(QuotaUnit.Request)
}
