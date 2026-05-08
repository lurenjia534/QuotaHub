package com.lurenjia534.quotahub.data.cloud

import com.lurenjia534.quotahub.data.model.QuotaResource
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.QuotaUnit
import com.lurenjia534.quotahub.data.model.QuotaWindow
import com.lurenjia534.quotahub.data.model.ResourceRole
import com.lurenjia534.quotahub.data.model.ResourceType
import com.lurenjia534.quotahub.data.model.WindowScope
import kotlinx.serialization.Serializable

@Serializable
data class RelaySubscriptionsResponse(
    val subscriptions: List<RelaySubscriptionDto> = emptyList(),
    val deletedSubscriptions: List<RelayDeletedSubscriptionDto> = emptyList()
)

@Serializable
data class RelayDeletedSubscriptionDto(
    val id: String,
    val deletedAt: Long
)

@Serializable
data class RelaySubscriptionResponse(
    val subscription: RelaySubscriptionDto
)

@Serializable
data class RelayProvidersResponse(
    val providers: List<RelayProviderDescriptorDto> = emptyList()
)

@Serializable
data class RelayProviderDescriptorDto(
    val id: String,
    val displayName: String
)

@Serializable
data class RelaySubscriptionDto(
    val id: String,
    val providerId: String,
    val providerDisplayName: String? = null,
    val customTitle: String? = null,
    val displayTitle: String? = null,
    val syncState: String = "pending",
    val lastSyncedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val snapshot: RelayQuotaSnapshotDto? = null
)

@Serializable
data class RelayQuotaSnapshotDto(
    val fetchedAt: Long,
    val resources: List<RelayQuotaResourceDto> = emptyList()
) {
    fun toDomain(): QuotaSnapshot {
        return QuotaSnapshot(
            fetchedAt = fetchedAt,
            resources = resources.map { it.toDomain() }
        )
    }
}

@Serializable
data class RelayQuotaResourceDto(
    val key: String,
    val title: String,
    val type: String,
    val role: String? = null,
    val bucket: String? = null,
    val windows: List<RelayQuotaWindowDto> = emptyList()
) {
    fun toDomain(): QuotaResource {
        return QuotaResource(
            key = key,
            title = title,
            type = enumValueOrDefault(type, ResourceType.Feature),
            role = role?.let { enumValueOrNull<ResourceRole>(it) },
            bucket = bucket,
            windows = windows.map { it.toDomain() }
        )
    }
}

@Serializable
data class RelayQuotaWindowDto(
    val windowKey: String,
    val scope: String,
    val label: String? = null,
    val total: Long? = null,
    val used: Long? = null,
    val remaining: Long? = null,
    val resetAtEpochMillis: Long? = null,
    val startsAt: Long? = null,
    val endsAt: Long? = null,
    val unit: String = "Request"
) {
    fun toDomain(): QuotaWindow {
        return QuotaWindow(
            windowKey = windowKey,
            scope = enumValueOrDefault(scope, WindowScope.Interval),
            label = label,
            total = total,
            used = used,
            remaining = remaining,
            resetAtEpochMillis = resetAtEpochMillis,
            startsAt = startsAt,
            endsAt = endsAt,
            unit = enumValueOrDefault(unit, QuotaUnit.Request)
        )
    }
}

@Serializable
data class RelayErrorResponse(
    val error: RelayErrorDto? = null
)

@Serializable
data class RelayErrorDto(
    val code: String,
    val message: String
)

private inline fun <reified T : Enum<T>> enumValueOrNull(raw: String): T? {
    return enumValues<T>().firstOrNull { it.name == raw }
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String, fallback: T): T {
    return enumValueOrNull<T>(raw) ?: fallback
}
