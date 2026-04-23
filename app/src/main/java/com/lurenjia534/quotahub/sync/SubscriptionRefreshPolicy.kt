package com.lurenjia534.quotahub.sync

import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.model.SyncFailureKind
import com.lurenjia534.quotahub.data.model.SyncState

interface SubscriptionRefreshPolicy {
    fun shouldAutoRefreshOnDetailOpen(
        subscription: Subscription,
        now: Long = System.currentTimeMillis()
    ): Boolean
}

class DefaultSubscriptionRefreshPolicy : SubscriptionRefreshPolicy {
    override fun shouldAutoRefreshOnDetailOpen(
        subscription: Subscription,
        now: Long
    ): Boolean {
        if (!subscription.hasUsableCredentials) {
            return false
        }

        return when (subscription.syncStatus.state) {
            SyncState.NeverSynced,
            SyncState.Stale -> true

            SyncState.AuthFailed,
            SyncState.Syncing -> false

            SyncState.Active -> {
                val lastSuccessAt = subscription.syncStatus.lastSuccessAt ?: return true
                now - lastSuccessAt >= ActiveRefreshIntervalMillis
            }

            SyncState.SyncError -> {
                when (subscription.syncStatus.lastFailureKind) {
                    SyncFailureKind.SchemaChanged,
                    SyncFailureKind.Validation -> return false
                    else -> Unit
                }
                subscription.syncStatus.nextEligibleSyncAt?.let { nextEligibleSyncAt ->
                    return now >= nextEligibleSyncAt
                }
                val lastFailureAt = subscription.syncStatus.lastFailureAt ?: return true
                now - lastFailureAt >= FailureRetryBackoffMillis
            }
        }
    }

    private companion object {
        private const val ActiveRefreshIntervalMillis = 5 * 60 * 1000L
        private const val FailureRetryBackoffMillis = 2 * 60 * 1000L
    }
}
