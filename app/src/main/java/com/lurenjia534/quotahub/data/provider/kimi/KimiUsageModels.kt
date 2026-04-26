package com.lurenjia534.quotahub.data.provider.kimi

import com.lurenjia534.quotahub.data.model.QuotaBuckets
import com.lurenjia534.quotahub.data.model.QuotaResource
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.QuotaUnit
import com.lurenjia534.quotahub.data.model.QuotaWindow
import com.lurenjia534.quotahub.data.model.QuotaWindowLabels
import com.lurenjia534.quotahub.data.model.ResourceRole
import com.lurenjia534.quotahub.data.model.ResourceType
import com.lurenjia534.quotahub.data.model.WindowScope
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId

@Serializable
data class KimiUsageResponse(
    val user: KimiUser? = null,
    val usage: KimiUsageSummary,
    val limits: List<KimiUsageLimit> = emptyList(),
    val parallel: KimiParallelLimit? = null,
    val totalQuota: KimiQuotaSummary? = null,
    val authentication: KimiAuthentication? = null,
    val subType: String? = null
)

@Serializable
data class KimiUser(
    val userId: String? = null,
    val region: String? = null,
    val membership: KimiMembership? = null,
    val businessId: String? = null
)

@Serializable
data class KimiMembership(
    val level: String? = null
)

@Serializable
data class KimiUsageSummary(
    val limit: String,
    val remaining: String,
    val resetTime: String
)

@Serializable
data class KimiUsageLimit(
    val window: KimiUsageWindow,
    val detail: KimiUsageSummary
)

@Serializable
data class KimiUsageWindow(
    val duration: Long,
    val timeUnit: String
)

@Serializable
data class KimiParallelLimit(
    val limit: String
)

@Serializable
data class KimiQuotaSummary(
    val limit: String,
    val remaining: String
)

@Serializable
data class KimiAuthentication(
    val method: String? = null,
    val scope: String? = null
)

fun KimiUsageResponse.toQuotaSnapshot(
    fetchedAt: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): QuotaSnapshot {
    val totalQuotaSummary = totalQuota?.toUsageSummary(resetTime = usage.resetTime) ?: usage
    return QuotaSnapshot(
        fetchedAt = fetchedAt,
        resources = buildList {
            add(totalQuotaSummary.toPlanResource())
            limits.mapIndexed { index, limit ->
                limit.toQuotaResource(index = index, zoneId = zoneId)
            }.forEach(::add)
            parallel?.toQuotaResource()?.let(::add)
        }
    )
}

private fun KimiQuotaSummary.toUsageSummary(resetTime: String): KimiUsageSummary {
    return KimiUsageSummary(
        limit = limit,
        remaining = remaining,
        resetTime = resetTime
    )
}

private fun KimiUsageSummary.toPlanResource(): QuotaResource {
    return QuotaResource(
        key = PLAN_RESOURCE_KEY,
        title = "Total plan quota",
        type = ResourceType.Plan,
        role = ResourceRole.Anchor,
        bucket = QuotaBuckets.ModelCalls,
        windows = listOf(
            toQuotaWindow(
                windowKey = "total",
                scope = WindowScope.Rolling,
                label = "Purchased plan",
                startsAt = null
            )
        )
    )
}

private fun KimiUsageLimit.toQuotaResource(
    index: Int,
    zoneId: ZoneId
): QuotaResource {
    return QuotaResource(
        key = "$LIMIT_RESOURCE_KEY_PREFIX$index",
        title = "${window.displayLabel()} window",
        type = ResourceType.Plan,
        role = ResourceRole.Limit,
        bucket = QuotaBuckets.ModelCalls,
        windows = listOf(toQuotaWindow(index = index, zoneId = zoneId))
    )
}

private fun KimiUsageLimit.toQuotaWindow(
    index: Int,
    zoneId: ZoneId
): QuotaWindow {
    val resetAt = detail.resetAtEpochMillis()
    return detail.toQuotaWindow(
        windowKey = "window-$index",
        scope = window.scope(),
        label = window.displayLabel(),
        startsAt = resetAt?.let { window.startEpochMillis(resetAtEpochMillis = it, zoneId = zoneId) }
    )
}

private fun KimiUsageSummary.toQuotaWindow(
    windowKey: String,
    scope: WindowScope,
    label: String,
    startsAt: Long?
): QuotaWindow {
    val totalValue = limit.toLongQuota("limit")
    val remainingValue = remaining.toLongQuota("remaining")
    return QuotaWindow(
        windowKey = windowKey,
        scope = scope,
        label = label,
        total = totalValue,
        used = (totalValue - remainingValue).coerceAtLeast(0L),
        remaining = remainingValue,
        resetAtEpochMillis = resetAtEpochMillis(),
        startsAt = startsAt,
        endsAt = resetAtEpochMillis(),
        unit = QuotaUnit.Request
    )
}

private fun KimiParallelLimit.toQuotaResource(): QuotaResource {
    val parallelLimit = limit.toLongQuota("parallel limit")
    return QuotaResource(
        key = PARALLEL_RESOURCE_KEY,
        title = "Parallel capacity",
        type = ResourceType.Feature,
        role = ResourceRole.Anchor,
        bucket = KIMI_PARALLEL_BUCKET,
        windows = listOf(
            QuotaWindow(
                windowKey = "current",
                scope = WindowScope.Rolling,
                label = QuotaWindowLabels.Current,
                total = parallelLimit,
                used = null,
                remaining = parallelLimit,
                resetAtEpochMillis = null,
                unit = QuotaUnit.Request
            )
        )
    )
}

private fun KimiUsageWindow.scope(): WindowScope {
    return when (timeUnit) {
        TIME_UNIT_MINUTE -> WindowScope.Rolling
        TIME_UNIT_HOUR -> WindowScope.Rolling
        TIME_UNIT_DAY -> WindowScope.Daily
        TIME_UNIT_MONTH -> WindowScope.Monthly
        else -> WindowScope.Rolling
    }
}

private fun KimiUsageWindow.displayLabel(): String {
    if (timeUnit == TIME_UNIT_MINUTE && duration >= 60L && duration % 60L == 0L) {
        return pluralizedDuration(duration / 60L, "hour")
    }
    val unit = when (timeUnit) {
        TIME_UNIT_MINUTE -> "minute"
        TIME_UNIT_HOUR -> "hour"
        TIME_UNIT_DAY -> "day"
        TIME_UNIT_MONTH -> "month"
        else -> "window"
    }
    return pluralizedDuration(duration, unit)
}

private fun KimiUsageWindow.startEpochMillis(
    resetAtEpochMillis: Long,
    zoneId: ZoneId
): Long? {
    val resetAt = Instant.ofEpochMilli(resetAtEpochMillis).atZone(zoneId)
    return when (timeUnit) {
        TIME_UNIT_MINUTE -> resetAt.minusMinutes(duration).toInstant().toEpochMilli()
        TIME_UNIT_HOUR -> resetAt.minusHours(duration).toInstant().toEpochMilli()
        TIME_UNIT_DAY -> resetAt.minusDays(duration).toInstant().toEpochMilli()
        TIME_UNIT_MONTH -> resetAt.minusMonths(duration).toInstant().toEpochMilli()
        else -> null
    }
}

private fun KimiUsageSummary.resetAtEpochMillis(): Long? {
    return runCatching {
        Instant.parse(resetTime).toEpochMilli()
    }.getOrNull()
}

private fun String.toLongQuota(fieldName: String): Long {
    return trim().toLongOrNull()?.coerceAtLeast(0L)
        ?: throw IllegalStateException("Invalid Kimi $fieldName value: $this")
}

private fun pluralizedDuration(
    value: Long,
    unit: String
): String {
    return if (value == 1L) {
        "$value $unit"
    } else {
        "$value ${unit}s"
    }
}

internal const val PLAN_RESOURCE_KEY = "coding-plan"
internal const val LIMIT_RESOURCE_KEY_PREFIX = "quota-window:"
internal const val PARALLEL_RESOURCE_KEY = "parallel"
internal const val KIMI_PARALLEL_BUCKET = "parallel"

private const val TIME_UNIT_MINUTE = "TIME_UNIT_MINUTE"
private const val TIME_UNIT_HOUR = "TIME_UNIT_HOUR"
private const val TIME_UNIT_DAY = "TIME_UNIT_DAY"
private const val TIME_UNIT_MONTH = "TIME_UNIT_MONTH"
