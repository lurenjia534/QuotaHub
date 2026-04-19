package com.lurenjia534.quotahub.data.provider.codex

import com.lurenjia534.quotahub.data.model.QuotaResource
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.QuotaUnit
import com.lurenjia534.quotahub.data.model.QuotaWindow
import com.lurenjia534.quotahub.data.model.ResourceType
import com.lurenjia534.quotahub.data.model.WindowScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.roundToInt

@Serializable
data class CodexUsageResponse(
    @SerialName("plan_type")
    val planType: String? = null,
    @SerialName("rate_limit")
    val rateLimit: CodexRateLimitStatus? = null,
    @SerialName("additional_rate_limits")
    val additionalRateLimits: List<CodexAdditionalRateLimit> = emptyList(),
    @SerialName("rate_limit_reached_type")
    val rateLimitReachedType: CodexRateLimitReachedDetails? = null,
    val credits: CodexCreditsStatus? = null
)

@Serializable
data class CodexRateLimitStatus(
    val allowed: Boolean? = null,
    @SerialName("limit_reached")
    val limitReached: Boolean? = null,
    @SerialName("primary_window")
    val primaryWindow: CodexRateLimitWindow? = null,
    @SerialName("secondary_window")
    val secondaryWindow: CodexRateLimitWindow? = null
)

@Serializable
data class CodexRateLimitWindow(
    @SerialName("used_percent")
    val usedPercent: Double? = null,
    @SerialName("limit_window_seconds")
    val limitWindowSeconds: Long? = null,
    @SerialName("reset_after_seconds")
    val resetAfterSeconds: Long? = null,
    @SerialName("reset_at")
    val resetAt: Long? = null
)

@Serializable
data class CodexAdditionalRateLimit(
    @SerialName("limit_name")
    val limitName: String? = null,
    @SerialName("metered_feature")
    val meteredFeature: String,
    @SerialName("rate_limit")
    val rateLimit: CodexRateLimitStatus? = null
)

@Serializable
data class CodexRateLimitReachedDetails(
    @SerialName("type")
    val type: String? = null
)

@Serializable
data class CodexCreditsStatus(
    @SerialName("has_credits")
    val hasCredits: Boolean? = null,
    val unlimited: Boolean? = null,
    val balance: String? = null
)

fun CodexUsageResponse.toQuotaSnapshot(
    fetchedAt: Long = System.currentTimeMillis()
): QuotaSnapshot {
    return QuotaSnapshot(
        fetchedAt = fetchedAt,
        resources = buildList {
            rateLimit?.toQuotaResource(
                key = PRIMARY_LIMIT_KEY,
                title = primaryTitle(planType),
                type = ResourceType.Plan
            )?.let(::add)
            additionalRateLimits.mapIndexedNotNull { index, additional ->
                additional.toQuotaResource(index)
            }.forEach(::add)
        }
    )
}

private fun CodexAdditionalRateLimit.toQuotaResource(index: Int): QuotaResource? {
    val key = meteredFeature.trim().takeIf { it.isNotEmpty() }
        ?: "codex-feature-${index + 1}"
    return rateLimit?.toQuotaResource(
        key = key,
        title = limitName?.trim()?.takeIf { it.isNotEmpty() } ?: key.toDisplayTitle(),
        type = ResourceType.Feature
    )
}

private fun CodexRateLimitStatus.toQuotaResource(
    key: String,
    title: String,
    type: ResourceType
): QuotaResource? {
    val windows = buildList {
        primaryWindow?.toQuotaWindow(PRIMARY_WINDOW_KEY)?.let(::add)
        secondaryWindow?.toQuotaWindow(SECONDARY_WINDOW_KEY)?.let(::add)
    }
    if (windows.isEmpty()) {
        return null
    }
    return QuotaResource(
        key = key,
        title = title,
        type = type,
        windows = windows
    )
}

private fun CodexRateLimitWindow.toQuotaWindow(windowKey: String): QuotaWindow {
    val usedValue = normalizedUsedPercent()
    val resetAtMillis = resetAt?.times(1_000)
    val durationMillis = limitWindowSeconds?.takeIf { it > 0L }?.times(1_000)
    return QuotaWindow(
        windowKey = windowKey,
        scope = limitWindowSeconds.toWindowScope(),
        total = 100L,
        used = usedValue.toLong(),
        remaining = (100 - usedValue).toLong(),
        resetAtEpochMillis = resetAtMillis,
        startsAt = if (resetAtMillis != null && durationMillis != null) {
            (resetAtMillis - durationMillis).coerceAtLeast(0L)
        } else {
            null
        },
        endsAt = resetAtMillis,
        unit = QuotaUnit.Percent
    )
}

private fun CodexRateLimitWindow.normalizedUsedPercent(): Int {
    return (usedPercent ?: 0.0).roundToInt().coerceIn(0, 100)
}

private fun Long?.toWindowScope(): WindowScope {
    val seconds = this ?: return WindowScope.Rolling
    val days = seconds.toDouble() / SECONDS_PER_DAY
    return when {
        abs(days - 1.0) <= 0.25 -> WindowScope.Daily
        abs(days - 7.0) <= 1.5 -> WindowScope.Weekly
        abs(days - 30.0) <= 5.0 -> WindowScope.Monthly
        else -> WindowScope.Rolling
    }
}

private fun primaryTitle(planType: String?): String {
    val normalizedPlan = planType?.trim()?.takeIf { it.isNotEmpty() }?.toDisplayTitle()
    return if (normalizedPlan == null) {
        "OpenAI Codex"
    } else {
        "OpenAI Codex ${normalizedPlan}"
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

private const val PRIMARY_LIMIT_KEY = "codex"
private const val PRIMARY_WINDOW_KEY = "primary"
private const val SECONDARY_WINDOW_KEY = "secondary"
private const val SECONDS_PER_DAY = 86_400.0
