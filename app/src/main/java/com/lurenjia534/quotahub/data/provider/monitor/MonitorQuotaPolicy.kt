package com.lurenjia534.quotahub.data.provider.monitor

import com.lurenjia534.quotahub.data.model.QuotaResource
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.QuotaWindow
import com.lurenjia534.quotahub.data.model.ResourceRole

internal fun List<QuotaResource>.monitorLimitResources(): List<QuotaResource> {
    return filter { it.role == ResourceRole.Limit }
}

internal fun List<QuotaResource>.monitorResources(
    role: ResourceRole,
    bucket: String
): List<QuotaResource> {
    return filter { it.role == role && it.bucket == bucket }
}

internal val QuotaResource.monitorWindow: QuotaWindow?
    get() = windows.firstOrNull()

internal fun List<QuotaResource>.lowestHeadroomPercent(): Int {
    return mapNotNull { it.monitorWindow?.remaining?.toInt() }.minOrNull() ?: 0
}

internal fun List<QuotaResource>.highestUsedPercent(): Int {
    return mapNotNull { it.monitorWindow?.used?.toInt() }.maxOrNull() ?: 0
}

internal fun List<QuotaResource>.monitorDominantRisk(): QuotaRisk {
    return when {
        any { it.monitorRisk() == QuotaRisk.Critical } -> QuotaRisk.Critical
        any { it.monitorRisk() == QuotaRisk.Watch } -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
    }
}

internal fun QuotaResource.monitorRisk(): QuotaRisk {
    val usedPercent = monitorWindow?.used?.toInt() ?: 0
    return when {
        usedPercent >= 95 -> QuotaRisk.Critical
        usedPercent >= 80 -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
    }
}

internal fun QuotaResource.windowUsedCount(): Long {
    return monitorWindow?.used ?: 0L
}

internal fun QuotaResource.windowUsedPercent(): Int {
    return (monitorWindow?.used ?: 0L).toInt()
}

internal fun QuotaResource.windowRemainingPercent(): Int {
    return (monitorWindow?.remaining ?: 0L).toInt()
}

internal fun QuotaWindow.progressPercent(): Float {
    val totalValue = total ?: return 0f
    val usedValue = used ?: return 0f
    return if (totalValue > 0L) {
        usedValue.toFloat() / totalValue.toFloat()
    } else {
        0f
    }
}
