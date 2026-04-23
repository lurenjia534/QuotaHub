package com.lurenjia534.quotahub.data.model

enum class SyncCause {
    ManualRefresh,
    AutoRefresh,
    CredentialsUpdated
}

enum class SyncFailureKind {
    Auth,
    RateLimited,
    Transient,
    SchemaChanged,
    Validation,
    Unknown
}

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
    val syncStartedAt: Long? = null,
    val lastFailureKind: SyncFailureKind? = null,
    val retryAfterUntil: Long? = null,
    val nextEligibleSyncAt: Long? = null,
    val lastSyncCause: SyncCause? = null
) {
    val isConnected: Boolean
        get() = state != SyncState.AuthFailed

    companion object {
        fun neverSynced(): SubscriptionSyncStatus = SubscriptionSyncStatus(
            state = SyncState.NeverSynced
        )

        fun syncing(
            previous: SubscriptionSyncStatus,
            cause: SyncCause,
            startedAt: Long = System.currentTimeMillis()
        ): SubscriptionSyncStatus = SubscriptionSyncStatus(
            state = SyncState.Syncing,
            lastSuccessAt = previous.lastSuccessAt,
            lastFailureAt = previous.lastFailureAt,
            lastError = null,
            syncStartedAt = startedAt,
            lastFailureKind = previous.lastFailureKind,
            retryAfterUntil = previous.retryAfterUntil,
            nextEligibleSyncAt = previous.nextEligibleSyncAt,
            lastSyncCause = cause
        )

        fun active(
            fetchedAt: Long,
            previous: SubscriptionSyncStatus? = null
        ): SubscriptionSyncStatus = SubscriptionSyncStatus(
            state = SyncState.Active,
            lastSuccessAt = fetchedAt,
            lastFailureAt = previous?.lastFailureAt,
            lastError = null,
            syncStartedAt = null,
            lastFailureKind = previous?.lastFailureKind,
            retryAfterUntil = null,
            nextEligibleSyncAt = null,
            lastSyncCause = previous?.lastSyncCause
        )

        fun failed(
            state: SyncState,
            failedAt: Long,
            errorMessage: String?,
            previous: SubscriptionSyncStatus,
            failureKind: SyncFailureKind? = null,
            retryAfterUntil: Long? = null,
            nextEligibleSyncAt: Long? = null
        ): SubscriptionSyncStatus = SubscriptionSyncStatus(
            state = state,
            lastSuccessAt = previous.lastSuccessAt,
            lastFailureAt = failedAt,
            lastError = errorMessage,
            syncStartedAt = null,
            lastFailureKind = failureKind,
            retryAfterUntil = retryAfterUntil,
            nextEligibleSyncAt = nextEligibleSyncAt,
            lastSyncCause = previous.lastSyncCause
        )
    }
}
