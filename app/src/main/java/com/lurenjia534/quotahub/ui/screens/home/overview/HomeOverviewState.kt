package com.lurenjia534.quotahub.ui.screens.home.overview

import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.SyncState
import com.lurenjia534.quotahub.data.provider.QuotaProgressMetric
import com.lurenjia534.quotahub.ui.screens.home.SubscriptionCardUiModel
import java.text.NumberFormat

internal data class SubscriptionSourceGroup(
    val key: String,
    val title: String,
    val subtitle: String,
    val label: String,
    val isCloud: Boolean,
    val cards: List<SubscriptionCardUiModel>
)

internal fun List<SubscriptionCardUiModel>.sourceGroups(): List<SubscriptionSourceGroup> {
    val cloudCards = filter { it.isCloudSynced }
    val localCards = filterNot { it.isCloudSynced }
    return buildList {
        if (cloudCards.isNotEmpty()) {
            add(
                SubscriptionSourceGroup(
                    key = "cloud",
                    title = "Cloud subscriptions",
                    subtitle = "Managed by QuotaHub Relay. Provider credentials stay on the server.",
                    label = "Relay",
                    isCloud = true,
                    cards = cloudCards
                )
            )
        }
        if (localCards.isNotEmpty()) {
            add(
                SubscriptionSourceGroup(
                    key = "local",
                    title = "Local subscriptions",
                    subtitle = "Added on this device. Credentials are stored in the Android keystore.",
                    label = "Device",
                    isCloud = false,
                    cards = localCards
                )
            )
        }
    }
}

internal fun List<SubscriptionCardUiModel>.dominantRisk(): QuotaRisk {
    return when {
        any { it.risk == QuotaRisk.Critical } -> QuotaRisk.Critical
        any { it.risk == QuotaRisk.Watch } -> QuotaRisk.Watch
        else -> QuotaRisk.Healthy
    }
}

internal fun List<SubscriptionCardUiModel>.dominantSyncState(): SyncState {
    return when {
        any { it.syncState == SyncState.AuthFailed } -> SyncState.AuthFailed
        any { it.syncState == SyncState.SyncError } -> SyncState.SyncError
        any { it.syncState == SyncState.Stale } -> SyncState.Stale
        any { it.syncState == SyncState.Syncing } -> SyncState.Syncing
        any { it.syncState == SyncState.NeverSynced } -> SyncState.NeverSynced
        else -> SyncState.Active
    }
}

internal fun List<QuotaProgressMetric>.landscapeMetricOrder(): List<QuotaProgressMetric> {
    return sortedWith(
        compareBy<QuotaProgressMetric> {
            when {
                it.label.contains("5h", ignoreCase = true) -> 0
                it.label.contains("week", ignoreCase = true) -> 1
                it.label.contains("plan", ignoreCase = true) -> 2
                else -> 3
            }
        }.thenBy { it.label }
    )
}

internal fun List<SubscriptionCardUiModel>.providerStatusLabel(isConnected: Boolean): String {
    return when {
        size > 1 -> "$size linked"
        isConnected -> "Linked"
        else -> "Open"
    }
}

internal fun List<SubscriptionCardUiModel>.providerAccessSummary(): String {
    if (isEmpty()) return "Ready to connect"

    val accountText = if (size == 1) "1 account" else "$size accounts"
    val resourceCount = sumOf { it.resourceCount }
    val resourceText = if (resourceCount == 1) "1 resource" else "$resourceCount resources"
    val syncText = dominantSyncState().providerAccessLabel()
    return "$accountText / $resourceText / $syncText"
}

internal fun SyncState.providerAccessLabel(): String {
    return when (this) {
        SyncState.AuthFailed -> "auth attention"
        SyncState.SyncError -> "sync error"
        SyncState.Stale -> "stale"
        SyncState.Syncing -> "syncing"
        SyncState.NeverSynced -> "not synced"
        SyncState.Active -> "active"
    }
}

internal fun String.percentValueOrNull(): Float? {
    return trim()
        .removeSuffix("%")
        .toFloatOrNull()
        ?.coerceIn(0f, 100f)
}

internal fun formatLongCount(value: Long): String {
    return NumberFormat.getIntegerInstance().format(value)
}
