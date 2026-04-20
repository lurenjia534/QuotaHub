package com.lurenjia534.quotahub.data.provider.codex

import com.lurenjia534.quotahub.data.model.QuotaResource
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.QuotaWindow
import com.lurenjia534.quotahub.data.model.WindowScope
import com.lurenjia534.quotahub.format.formatTimeRemaining

internal val QuotaResource.primaryWindow: QuotaWindow?
    get() = windows.firstOrNull()

internal val QuotaResource.secondaryWindow: QuotaWindow?
    get() = windows.getOrNull(1)

internal fun List<QuotaResource>.planLabel(): String? {
    return firstOrNull { it.key == "codex" }
        ?.title
        ?.removePrefix("OpenAI Codex")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}

internal fun QuotaResource.displayTitle(planLabel: String?): String {
    return if (key == "codex" && planLabel != null && title == "OpenAI Codex $planLabel") {
        "OpenAI Codex"
    } else {
        title
    }
}

internal fun List<QuotaResource>.lowestHeadroomPercent(): Int {
    return flatMap { it.windows }
        .map(QuotaWindow::remainingPercentValue)
        .minOrNull()
        ?: 0
}

internal fun List<QuotaResource>.highestUsedPercent(): Int {
    return flatMap { it.windows }
        .map(QuotaWindow::usedPercentValue)
        .maxOrNull()
        ?: 0
}

internal fun List<QuotaResource>.codexDominantRisk(): QuotaRisk {
    return highestUsedPercent().toQuotaRisk()
}

internal fun QuotaResource.codexRisk(): QuotaRisk {
    return windows.maxOfOrNull(QuotaWindow::usedPercentValue)?.toQuotaRisk() ?: QuotaRisk.Healthy
}

internal fun QuotaResource.soonestResetAt(): Long? {
    return windows.mapNotNull { it.resetAtEpochMillis }.minOrNull()
}

internal fun QuotaWindow.usedPercentValue(): Int {
    return used?.toInt()?.coerceIn(0, 100) ?: 0
}

internal fun QuotaWindow.remainingPercentValue(): Int {
    return remaining?.toInt()?.coerceIn(0, 100)
        ?: (100 - usedPercentValue()).coerceIn(0, 100)
}

internal fun QuotaWindow.progress(): Float {
    return usedPercentValue() / 100f
}

internal fun QuotaWindow.displayName(): String {
    return when (scope) {
        WindowScope.Daily -> "Daily"
        WindowScope.Weekly -> "Weekly"
        WindowScope.Monthly -> "Monthly"
        WindowScope.Rolling -> rollingWindowName()
        WindowScope.Interval -> "Window"
    }
}

private fun QuotaWindow.rollingWindowName(): String {
    val durationMillis = if (startsAt != null && endsAt != null) {
        (endsAt - startsAt).coerceAtLeast(0L)
    } else {
        0L
    }
    return if (durationMillis > 0L) {
        formatTimeRemaining(durationMillis)
    } else {
        "Rolling"
    }
}

private fun Int.toQuotaRisk(): QuotaRisk {
    return when {
        this >= 95 -> QuotaRisk.Critical
        this >= 80 -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
    }
}
