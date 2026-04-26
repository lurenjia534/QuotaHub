package com.lurenjia534.quotahub.data.provider.kimi

import com.lurenjia534.quotahub.data.model.QuotaResource
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.QuotaWindow

internal val QuotaResource.kimiPlanWindow: QuotaWindow?
    get() = windows.firstOrNull { it.windowKey == "total" }

internal val QuotaResource.kimiPrimaryWindow: QuotaWindow?
    get() = windows.firstOrNull()

internal fun QuotaResource.kimiRemainingCount(): Int {
    return (kimiPrimaryWindow?.remaining ?: 0L).toInt()
}

internal fun QuotaResource.kimiLimitCount(): Int {
    return (kimiPrimaryWindow?.total ?: 0L).toInt()
}

internal fun QuotaResource.kimiUsageProgress(): Float {
    return kimiPrimaryWindow?.usageProgress() ?: 0f
}

internal fun QuotaResource.kimiRisk(): QuotaRisk {
    val progress = kimiUsageProgress()
    return when {
        progress >= 0.95f -> QuotaRisk.Critical
        progress >= 0.80f -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
    }
}

internal fun List<QuotaResource>.kimiDominantRisk(): QuotaRisk {
    return when {
        any { it.kimiRisk() == QuotaRisk.Critical } -> QuotaRisk.Critical
        any { it.kimiRisk() == QuotaRisk.Watch } -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
    }
}

internal fun QuotaWindow.usageProgress(): Float {
    val totalValue = total ?: return 0f
    val usedValue = used ?: return 0f
    return if (totalValue > 0L) {
        usedValue.toFloat() / totalValue.toFloat()
    } else {
        0f
    }
}
