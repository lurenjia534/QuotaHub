package com.lurenjia534.quotahub.data.provider.zai

import com.lurenjia534.quotahub.data.model.QuotaResource
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.QuotaWindow
import com.lurenjia534.quotahub.data.model.ResourceType

internal fun List<QuotaResource>.zaiLimitResources(): List<QuotaResource> {
    return filter { resource ->
        resource.type == ResourceType.Plan && resource.key.startsWith("limit:")
    }
}

internal fun List<QuotaResource>.zaiModelResources(): List<QuotaResource> {
    return filter { it.key.startsWith("model:") }
}

internal fun List<QuotaResource>.zaiSampledToolResources(): List<QuotaResource> {
    return filter { it.key.startsWith("tool-sampled:") }
}

internal fun List<QuotaResource>.zaiLimitToolResources(): List<QuotaResource> {
    return filter { it.key.startsWith("tool-limit:") }
}

internal val QuotaResource.zaiWindow: QuotaWindow?
    get() = windows.firstOrNull()

internal fun List<QuotaResource>.lowestHeadroomPercent(): Int {
    return mapNotNull { it.zaiWindow?.remaining?.toInt() }.minOrNull() ?: 0
}

internal fun List<QuotaResource>.highestUsedPercent(): Int {
    return mapNotNull { it.zaiWindow?.used?.toInt() }.maxOrNull() ?: 0
}

internal fun List<QuotaResource>.zaiDominantRisk(): QuotaRisk {
    return when {
        any { it.zaiRisk() == QuotaRisk.Critical } -> QuotaRisk.Critical
        any { it.zaiRisk() == QuotaRisk.Watch } -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
    }
}

internal fun QuotaResource.zaiRisk(): QuotaRisk {
    val usedPercent = zaiWindow?.used?.toInt() ?: 0
    return when {
        usedPercent >= 95 -> QuotaRisk.Critical
        usedPercent >= 80 -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
    }
}

internal fun QuotaResource.windowUsedCount(): Long {
    return zaiWindow?.used ?: 0L
}

internal fun QuotaResource.windowUsedPercent(): Int {
    return (zaiWindow?.used ?: 0L).toInt()
}

internal fun QuotaResource.windowRemainingPercent(): Int {
    return (zaiWindow?.remaining ?: 0L).toInt()
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
