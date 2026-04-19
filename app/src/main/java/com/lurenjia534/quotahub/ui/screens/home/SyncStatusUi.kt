package com.lurenjia534.quotahub.ui.screens.home

import com.lurenjia534.quotahub.data.model.SubscriptionSyncStatus
import com.lurenjia534.quotahub.data.model.SyncState

internal fun SubscriptionSyncStatus.label(): String {
    return when (state) {
        SyncState.NeverSynced -> "Pending"
        SyncState.Syncing -> "Syncing"
        SyncState.Active -> "Active"
        SyncState.Stale -> "Stale"
        SyncState.SyncError -> "Error"
        SyncState.AuthFailed -> "Auth failed"
    }
}

internal fun SubscriptionSyncStatus.description(): String {
    return when (state) {
        SyncState.NeverSynced -> "Waiting for the first successful provider sync."
        SyncState.Syncing -> {
            lastSuccessAt?.let { "Refreshing now. Last success ${formatTimeAgo(it)}." }
                ?: "Refreshing now."
        }
        SyncState.Active -> {
            lastSuccessAt?.let { "Last success ${formatTimeAgo(it)}." }
                ?: "Provider snapshot is current."
        }
        SyncState.Stale -> {
            lastSuccessAt?.let { "Cached data is stale. Last success ${formatTimeAgo(it)}." }
                ?: "Cached data is stale."
        }
        SyncState.SyncError -> {
            val failure = lastFailureAt?.let { "Last failure ${formatTimeAgo(it)}." } ?: "Recent sync failed."
            listOfNotNull(failure, lastError).joinToString(" ")
        }
        SyncState.AuthFailed -> {
            val failure = lastFailureAt?.let { "Credential check failed ${formatTimeAgo(it)}." }
                ?: "Credential check failed."
            listOfNotNull(failure, lastError).joinToString(" ")
        }
    }
}
