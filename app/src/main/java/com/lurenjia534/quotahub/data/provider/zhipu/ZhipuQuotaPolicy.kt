package com.lurenjia534.quotahub.data.provider.zhipu

import com.lurenjia534.quotahub.data.model.QuotaResource
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.QuotaWindow
import com.lurenjia534.quotahub.data.model.ResourceType

internal fun List<QuotaResource>.zhipuLimitResources(): List<QuotaResource> {
    return filter { resource ->
        resource.type == ResourceType.Plan && resource.key.startsWith("limit:")
    }
}

internal fun List<QuotaResource>.zhipuModelResources(): List<QuotaResource> {
    return filter { it.key.startsWith("model:") }
}

internal fun List<QuotaResource>.zhipuSampledToolResources(): List<QuotaResource> {
    return filter { it.key.startsWith("tool-sampled:") }
}

internal fun List<QuotaResource>.zhipuLimitToolResources(): List<QuotaResource> {
    return filter { it.key.startsWith("tool-limit:") }
}

internal val QuotaResource.zhipuWindow: QuotaWindow?
    get() = windows.firstOrNull()

internal fun List<QuotaResource>.lowestHeadroomPercent(): Int {
    return mapNotNull { it.zhipuWindow?.remaining?.toInt() }.minOrNull() ?: 0
}

internal fun List<QuotaResource>.highestUsedPercent(): Int {
    return mapNotNull { it.zhipuWindow?.used?.toInt() }.maxOrNull() ?: 0
}

internal fun List<QuotaResource>.zhipuDominantRisk(): QuotaRisk {
    return when {
        any { it.zhipuRisk() == QuotaRisk.Critical } -> QuotaRisk.Critical
        any { it.zhipuRisk() == QuotaRisk.Watch } -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
    }
}

internal fun QuotaResource.zhipuRisk(): QuotaRisk {
    val usedPercent = zhipuWindow?.used?.toInt() ?: 0
    return when {
        usedPercent >= 95 -> QuotaRisk.Critical
        usedPercent >= 80 -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
    }
}

internal fun QuotaResource.windowUsedCount(): Long {
    return zhipuWindow?.used ?: 0L
}

internal fun QuotaResource.windowUsedPercent(): Int {
    return (zhipuWindow?.used ?: 0L).toInt()
}

internal fun QuotaResource.windowRemainingPercent(): Int {
    return (zhipuWindow?.remaining ?: 0L).toInt()
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
