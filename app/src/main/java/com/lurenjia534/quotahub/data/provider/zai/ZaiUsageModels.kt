package com.lurenjia534.quotahub.data.provider.zai

import com.lurenjia534.quotahub.data.model.QuotaResource
import com.lurenjia534.quotahub.data.model.QuotaBuckets
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.QuotaUnit
import com.lurenjia534.quotahub.data.model.QuotaWindowLabels
import com.lurenjia534.quotahub.data.model.QuotaWindow
import com.lurenjia534.quotahub.data.model.ResourceRole
import com.lurenjia534.quotahub.data.model.ResourceType
import com.lurenjia534.quotahub.data.model.WindowScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Serializable
data class ZaiApiEnvelope<T>(
    val code: Int? = null,
    val msg: String? = null,
    val data: T? = null,
    val success: Boolean? = null
)

@Serializable
data class ZaiUsageBundle(
    val modelUsage: ZaiApiEnvelope<ZaiModelUsageData>,
    val toolUsage: ZaiApiEnvelope<ZaiToolUsageData>,
    val quotaLimit: ZaiApiEnvelope<ZaiQuotaLimitData>
)

@Serializable
data class ZaiModelUsageData(
    @SerialName("x_time")
    val xTime: List<String> = emptyList(),
    val modelCallCount: List<Long> = emptyList(),
    val tokensUsage: List<Long> = emptyList(),
    val totalUsage: ZaiModelTotalUsage = ZaiModelTotalUsage(),
    val modelDataList: List<ZaiModelData> = emptyList(),
    val modelSummaryList: List<ZaiModelSummary> = emptyList(),
    val granularity: String? = null
)

@Serializable
data class ZaiModelTotalUsage(
    val totalModelCallCount: Long = 0L,
    val totalTokensUsage: Long = 0L,
    val modelSummaryList: List<ZaiModelSummary> = emptyList()
)

@Serializable
data class ZaiModelData(
    val modelName: String,
    val sortOrder: Int? = null,
    val tokensUsage: List<Long> = emptyList(),
    val totalTokens: Long = 0L
)

@Serializable
data class ZaiModelSummary(
    val modelName: String,
    val totalTokens: Long = 0L,
    val sortOrder: Int? = null
)

@Serializable
data class ZaiToolUsageData(
    @SerialName("x_time")
    val xTime: List<String> = emptyList(),
    val networkSearchCount: List<Long> = emptyList(),
    val webReadMcpCount: List<Long> = emptyList(),
    val zreadMcpCount: List<Long> = emptyList(),
    val totalUsage: ZaiToolTotalUsage = ZaiToolTotalUsage(),
    val toolDataList: List<ZaiToolData> = emptyList(),
    val toolSummaryList: List<ZaiToolSummary> = emptyList(),
    val granularity: String? = null
)

@Serializable
data class ZaiToolTotalUsage(
    val totalNetworkSearchCount: Long = 0L,
    val totalWebReadMcpCount: Long = 0L,
    val totalZreadMcpCount: Long = 0L,
    val totalSearchMcpCount: Long = 0L,
    val toolDetails: List<ZaiToolSummary> = emptyList(),
    val toolSummaryList: List<ZaiToolSummary> = emptyList()
)

@Serializable
data class ZaiToolData(
    val toolName: String? = null,
    val sortOrder: Int? = null,
    val usageCount: List<Long> = emptyList(),
    val totalUsage: Long = 0L
)

@Serializable
data class ZaiToolSummary(
    val toolName: String? = null,
    val modelCode: String? = null,
    val usage: Long = 0L,
    val sortOrder: Int? = null
)

@Serializable
data class ZaiQuotaLimitData(
    val limits: List<ZaiQuotaLimit> = emptyList(),
    val level: String? = null
)

@Serializable
data class ZaiQuotaLimit(
    val type: String,
    val unit: Int? = null,
    val number: Int? = null,
    val usage: Long? = null,
    val currentValue: Long? = null,
    val remaining: Long? = null,
    val percentage: Int? = null,
    val nextResetTime: Long? = null,
    val usageDetails: List<ZaiQuotaUsageDetail> = emptyList()
)

@Serializable
data class ZaiQuotaUsageDetail(
    val modelCode: String,
    val usage: Long = 0L
)

fun ZaiUsageBundle.toQuotaSnapshot(
    fetchedAt: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): QuotaSnapshot {
    val modelUsageData = modelUsage.requireData("model usage")
    val toolUsageData = toolUsage.requireData("tool usage")
    val quotaLimitData = quotaLimit.requireData("quota limit")
    val sampleWindow = modelUsageData.sampleWindow(zoneId) ?: toolUsageData.sampleWindow(zoneId)

    return QuotaSnapshot(
        fetchedAt = fetchedAt,
        resources = buildList {
            quotaLimitData.limits.mapIndexedNotNull { index, limit ->
                limit.toLimitResource(index = index, zoneId = zoneId)
            }.forEach(::add)

            modelUsageData.toModelResources(sampleWindow).forEach(::add)
            toolUsageData.toSampledToolResources(sampleWindow).forEach(::add)
            quotaLimitData.limits.flatMap { limit ->
                limit.toLimitDetailResources(zoneId)
            }.forEach(::add)
        }
    )
}

private fun ZaiModelUsageData.toModelResources(
    sampleWindow: ZaiSampleWindow?
): List<QuotaResource> {
    val summaries = when {
        modelSummaryList.isNotEmpty() -> modelSummaryList
        totalUsage.modelSummaryList.isNotEmpty() -> totalUsage.modelSummaryList
        modelDataList.isNotEmpty() -> modelDataList.map { model ->
            ZaiModelSummary(
                modelName = model.modelName,
                totalTokens = model.totalTokens,
                sortOrder = model.sortOrder
            )
        }
        else -> emptyList()
    }
    return summaries
        .sortedBy { it.sortOrder ?: Int.MAX_VALUE }
        .map { summary ->
            QuotaResource(
                key = "model:${summary.modelName}",
                title = summary.modelName,
                type = ResourceType.Model,
                role = ResourceRole.Sampled,
                bucket = QuotaBuckets.Tokens,
                windows = listOf(
                    QuotaWindow(
                        windowKey = "sampled",
                        scope = WindowScope.Rolling,
                        label = QuotaWindowLabels.Sampled,
                        total = null,
                        used = summary.totalTokens,
                        remaining = null,
                        resetAtEpochMillis = sampleWindow?.endsAt,
                        startsAt = sampleWindow?.startsAt,
                        endsAt = sampleWindow?.endsAt,
                        unit = QuotaUnit.Token
                    )
                )
            )
        }
}

private fun ZaiToolUsageData.toSampledToolResources(
    sampleWindow: ZaiSampleWindow?
): List<QuotaResource> {
    val networkSearchTotal = firstNonZeroOf(
        totalUsage.totalNetworkSearchCount,
        summarizedToolUsage("search-prime", "network-search")
    )
    val webReaderTotal = firstNonZeroOf(
        totalUsage.totalWebReadMcpCount,
        summarizedToolUsage("web-reader", "web-read")
    )
    val zreadTotal = firstNonZeroOf(
        totalUsage.totalZreadMcpCount,
        summarizedToolUsage("zread")
    )

    return listOf(
        ZaiSampledToolMetric(
            key = "network-search",
            title = "Network search",
            usage = networkSearchTotal
        ),
        ZaiSampledToolMetric(
            key = "web-reader",
            title = "Web Reader",
            usage = webReaderTotal
        ),
        ZaiSampledToolMetric(
            key = "zread",
            title = "ZRead MCP",
            usage = zreadTotal
        )
    ).filter { it.usage > 0L }
        .map { metric ->
            QuotaResource(
                key = "tool-sampled:${metric.key}",
                title = metric.title,
                type = ResourceType.Feature,
                role = ResourceRole.Sampled,
                bucket = QuotaBuckets.Mcp,
                windows = listOf(
                    QuotaWindow(
                        windowKey = "sampled",
                        scope = WindowScope.Rolling,
                        label = QuotaWindowLabels.Sampled,
                        total = null,
                        used = metric.usage,
                        remaining = null,
                        resetAtEpochMillis = sampleWindow?.endsAt,
                        startsAt = sampleWindow?.startsAt,
                        endsAt = sampleWindow?.endsAt,
                        unit = QuotaUnit.Request
                    )
                )
            )
        }
}

private fun ZaiQuotaLimit.toLimitResource(
    index: Int,
    zoneId: ZoneId
): QuotaResource? {
    val normalizedType = type.trim().takeIf { it.isNotEmpty() } ?: return null
    val usedPercent = (percentage ?: currentValue?.toInt() ?: 0).coerceIn(0, 100)
    val remainingPercent = (remaining?.toInt() ?: (100 - usedPercent)).coerceIn(0, 100)
    val resetAt = nextResetTime?.takeIf { it > 0L }

    return QuotaResource(
        key = "limit:${normalizedType.lowercase()}",
        title = "${normalizedType.limitDisplayTitle()} (${windowLabel()})",
        type = ResourceType.Plan,
        role = ResourceRole.Limit,
        bucket = bucket(),
        windows = listOf(
            QuotaWindow(
                windowKey = "limit-$index",
                scope = windowScope(),
                label = windowLabel(),
                total = 100L,
                used = usedPercent.toLong(),
                remaining = remainingPercent.toLong(),
                resetAtEpochMillis = resetAt,
                startsAt = resetAt?.let { windowStartEpochMillis(it, zoneId) },
                endsAt = resetAt,
                unit = QuotaUnit.Percent
            )
        )
    )
}

private fun ZaiQuotaLimit.toLimitDetailResources(
    zoneId: ZoneId
): List<QuotaResource> {
    val resetAt = nextResetTime?.takeIf { it > 0L }
    return usageDetails
        .filter { it.usage > 0L }
        .map { detail ->
            QuotaResource(
                key = "tool-limit:${detail.modelCode}",
                title = detail.modelCode.toDisplayTitle(),
                type = ResourceType.Feature,
                role = ResourceRole.Contributor,
                bucket = bucket(),
                windows = listOf(
                    QuotaWindow(
                        windowKey = "current",
                        scope = windowScope(),
                        label = QuotaWindowLabels.Current,
                        total = null,
                        used = detail.usage,
                        remaining = null,
                        resetAtEpochMillis = resetAt,
                        startsAt = resetAt?.let { windowStartEpochMillis(it, zoneId) },
                        endsAt = resetAt,
                        unit = QuotaUnit.Request
                    )
                )
            )
        }
}

private fun ZaiModelUsageData.sampleWindow(
    zoneId: ZoneId
): ZaiSampleWindow? {
    return xTime.toSampleWindow(zoneId)
}

private fun ZaiToolUsageData.sampleWindow(
    zoneId: ZoneId
): ZaiSampleWindow? {
    return xTime.toSampleWindow(zoneId)
}

private fun List<String>.toSampleWindow(zoneId: ZoneId): ZaiSampleWindow? {
    val firstHour = firstOrNull()?.toEpochMillis(HOURLY_FORMATTER, zoneId) ?: return null
    val lastHour = lastOrNull()?.toEpochMillis(HOURLY_FORMATTER, zoneId) ?: return null
    return ZaiSampleWindow(
        startsAt = firstHour,
        endsAt = lastHour + HOURLY_WINDOW_MILLIS - 1
    )
}

private fun String.toEpochMillis(
    formatter: DateTimeFormatter,
    zoneId: ZoneId
): Long {
    return LocalDateTime.parse(this, formatter)
        .atZone(zoneId)
        .toInstant()
        .toEpochMilli()
}

private fun ZaiQuotaLimit.windowScope(): WindowScope {
    return when (unit) {
        UNIT_MONTH -> WindowScope.Monthly
        UNIT_HOUR -> WindowScope.Rolling
        else -> WindowScope.Rolling
    }
}

private fun ZaiQuotaLimit.bucket(): String {
    return when (type.trim().uppercase()) {
        "TOKENS_LIMIT" -> QuotaBuckets.Tokens
        "TIME_LIMIT" -> QuotaBuckets.Mcp
        else -> type.trim().lowercase()
    }
}

private fun ZaiQuotaLimit.windowLabel(): String {
    val durationNumber = number?.takeIf { it > 0 } ?: 1
    return when (unit) {
        UNIT_MONTH -> pluralizedDuration(durationNumber, "month")
        UNIT_HOUR -> pluralizedDuration(durationNumber, "hour")
        else -> "current window"
    }
}

private fun ZaiQuotaLimit.windowStartEpochMillis(
    resetAtEpochMillis: Long,
    zoneId: ZoneId
): Long? {
    val numberValue = number?.takeIf { it > 0 } ?: return null
    val resetAt = Instant.ofEpochMilli(resetAtEpochMillis).atZone(zoneId)
    return when (unit) {
        UNIT_MONTH -> resetAt.minusMonths(numberValue.toLong()).toInstant().toEpochMilli()
        UNIT_HOUR -> resetAt.minusHours(numberValue.toLong()).toInstant().toEpochMilli()
        else -> null
    }
}

private fun String.limitDisplayTitle(): String {
    return when (this) {
        "TOKENS_LIMIT" -> "Token usage"
        "TIME_LIMIT" -> "MCP usage"
        else -> toDisplayTitle()
    }
}

private fun String.toDisplayTitle(): String {
    return split('-', '_', ' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { segment ->
            segment.lowercase().replaceFirstChar { char ->
                char.titlecase()
            }
        }
}

private fun pluralizedDuration(
    value: Int,
    unit: String
): String {
    return if (value == 1) {
        "$value $unit"
    } else {
        "$value ${unit}s"
    }
}

private fun <T> ZaiApiEnvelope<T>.requireData(endpointName: String): T {
    require(success != false) {
        msg ?: "Request to $endpointName failed"
    }
    require(code == null || code == 200) {
        msg ?: "Unexpected response code $code from $endpointName"
    }
    return requireNotNull(data) {
        "Missing data in $endpointName response"
    }
}

private fun ZaiToolUsageData.summarizedToolUsage(vararg keys: String): Long {
    val normalizedKeys = keys.map { it.lowercase() }
    return (toolSummaryList + totalUsage.toolDetails + totalUsage.toolSummaryList)
        .filter { summary ->
            val candidates = listOfNotNull(summary.toolName, summary.modelCode)
                .map { it.lowercase() }
            candidates.any { candidate ->
                normalizedKeys.any { key -> candidate.contains(key) }
            }
        }
        .sumOf { it.usage }
}

private fun firstNonZeroOf(vararg values: Long): Long {
    return values.firstOrNull { it > 0L } ?: 0L
}

private data class ZaiSampledToolMetric(
    val key: String,
    val title: String,
    val usage: Long
)

private data class ZaiSampleWindow(
    val startsAt: Long,
    val endsAt: Long
)

private val HOURLY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private const val HOURLY_WINDOW_MILLIS = 3_600_000L
private const val UNIT_HOUR = 3
private const val UNIT_MONTH = 5
