package com.lurenjia534.quotahub.data.model

enum class SyncState {
    NeverSynced,
    Syncing,
    Active,
    Stale,
    SyncError,
    AuthFailed
}

data class SubscriptionSyncStatus(
    val state: SyncState,
    val lastSuccessAt: Long? = null,
    val lastFailureAt: Long? = null,
    val lastError: String? = null,
    val syncStartedAt: Long? = null
) {
    val isConnected: Boolean
        get() = state != SyncState.AuthFailed

    companion object {
        fun neverSynced(): SubscriptionSyncStatus = SubscriptionSyncStatus(
            state = SyncState.NeverSynced
        )

        fun syncing(
            previous: SubscriptionSyncStatus,
            startedAt: Long = System.currentTimeMillis()
        ): SubscriptionSyncStatus = SubscriptionSyncStatus(
            state = SyncState.Syncing,
            lastSuccessAt = previous.lastSuccessAt,
            lastFailureAt = previous.lastFailureAt,
            lastError = null,
            syncStartedAt = startedAt
        )

        fun active(
            fetchedAt: Long,
            previous: SubscriptionSyncStatus? = null
        ): SubscriptionSyncStatus = SubscriptionSyncStatus(
            state = SyncState.Active,
            lastSuccessAt = fetchedAt,
            lastFailureAt = previous?.lastFailureAt,
            lastError = null,
            syncStartedAt = null
        )

        fun failed(
            state: SyncState,
            failedAt: Long,
            errorMessage: String?,
            previous: SubscriptionSyncStatus
        ): SubscriptionSyncStatus = SubscriptionSyncStatus(
            state = state,
            lastSuccessAt = previous.lastSuccessAt,
            lastFailureAt = failedAt,
            lastError = errorMessage,
            syncStartedAt = null
        )
    }
}
