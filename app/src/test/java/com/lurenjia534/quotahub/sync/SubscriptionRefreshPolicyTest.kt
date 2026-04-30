package com.lurenjia534.quotahub.sync

import com.lurenjia534.quotahub.data.model.CredentialState
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.model.SubscriptionProvider
import com.lurenjia534.quotahub.data.model.SyncCause
import com.lurenjia534.quotahub.data.model.SyncFailureKind
import com.lurenjia534.quotahub.data.model.SubscriptionSyncStatus
import com.lurenjia534.quotahub.data.model.SyncState
import com.lurenjia534.quotahub.data.preferences.RefreshCadence
import com.lurenjia534.quotahub.data.provider.CredentialFieldSpec
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionRefreshPolicyTest {
    private val policy = DefaultSubscriptionRefreshPolicy()

    @Test
    fun shouldAutoRefreshOnDetailOpen_allowsNeverSyncedSubscriptions() {
        assertTrue(
            policy.shouldAutoRefreshOnDetailOpen(
                subscription(syncStatus = SubscriptionSyncStatus.neverSynced()),
                now = 1_000L
            )
        )
    }

    @Test
    fun shouldAutoRefreshOnDetailOpen_skipsFreshActiveSubscriptions() {
        assertFalse(
            policy.shouldAutoRefreshOnDetailOpen(
                subscription(
                    syncStatus = SubscriptionSyncStatus.active(
                        fetchedAt = 900L
                    )
                ),
                now = 1_000L
            )
        )
    }

    @Test
    fun shouldAutoRefresh_usesCadenceIntervalForActiveSubscriptions() {
        val subscription = subscription(
            syncStatus = SubscriptionSyncStatus.active(
                fetchedAt = 1_000L
            )
        )

        assertFalse(
            policy.shouldAutoRefresh(
                subscription = subscription,
                refreshCadence = RefreshCadence.Live,
                now = 60_999L
            )
        )
        assertTrue(
            policy.shouldAutoRefresh(
                subscription = subscription,
                refreshCadence = RefreshCadence.Live,
                now = 61_000L
            )
        )
        assertFalse(
            policy.shouldAutoRefresh(
                subscription = subscription,
                refreshCadence = RefreshCadence.Balanced,
                now = 3_600_999L
            )
        )
        assertTrue(
            policy.shouldAutoRefresh(
                subscription = subscription,
                refreshCadence = RefreshCadence.Balanced,
                now = 3_601_000L
            )
        )
    }

    @Test
    fun shouldAutoRefresh_skipsAutomaticRefreshWhenManual() {
        assertFalse(
            policy.shouldAutoRefresh(
                subscription = subscription(syncStatus = SubscriptionSyncStatus.neverSynced()),
                refreshCadence = RefreshCadence.Manual,
                now = 1_000L
            )
        )
    }

    @Test
    fun shouldAutoRefreshOnDetailOpen_retriesStaleAndBackedOffFailures() {
        assertTrue(
            policy.shouldAutoRefreshOnDetailOpen(
                subscription(
                    syncStatus = SubscriptionSyncStatus(
                        state = SyncState.Stale,
                        lastSuccessAt = 1L
                    )
                ),
                now = 1_000L
            )
        )
        assertTrue(
            policy.shouldAutoRefreshOnDetailOpen(
                subscription(
                    syncStatus = SubscriptionSyncStatus(
                        state = SyncState.SyncError,
                        lastFailureAt = 1L
                    )
                ),
                now = 500_000L
            )
        )
    }

    @Test
    fun shouldAutoRefreshOnDetailOpen_respectsNextEligibleRetryWindow() {
        assertFalse(
            policy.shouldAutoRefreshOnDetailOpen(
                subscription(
                    syncStatus = SubscriptionSyncStatus(
                        state = SyncState.SyncError,
                        lastFailureAt = 1_000L,
                        lastFailureKind = SyncFailureKind.RateLimited,
                        nextEligibleSyncAt = 5_000L,
                        lastSyncCause = SyncCause.AutoRefresh
                    )
                ),
                now = 4_999L
            )
        )
        assertTrue(
            policy.shouldAutoRefreshOnDetailOpen(
                subscription(
                    syncStatus = SubscriptionSyncStatus(
                        state = SyncState.SyncError,
                        lastFailureAt = 1_000L,
                        lastFailureKind = SyncFailureKind.RateLimited,
                        nextEligibleSyncAt = 5_000L,
                        lastSyncCause = SyncCause.AutoRefresh
                    )
                ),
                now = 5_000L
            )
        )
    }

    @Test
    fun shouldAutoRefreshOnDetailOpen_pausesSchemaAndValidationFailures() {
        assertFalse(
            policy.shouldAutoRefreshOnDetailOpen(
                subscription(
                    syncStatus = SubscriptionSyncStatus(
                        state = SyncState.SyncError,
                        lastFailureAt = 1L,
                        lastFailureKind = SyncFailureKind.SchemaChanged
                    )
                ),
                now = 500_000L
            )
        )
        assertFalse(
            policy.shouldAutoRefreshOnDetailOpen(
                subscription(
                    syncStatus = SubscriptionSyncStatus(
                        state = SyncState.SyncError,
                        lastFailureAt = 1L,
                        lastFailureKind = SyncFailureKind.Validation
                    )
                ),
                now = 500_000L
            )
        )
    }

    @Test
    fun shouldAutoRefreshOnDetailOpen_skipsAuthFailuresAndInFlightSyncs() {
        assertFalse(
            policy.shouldAutoRefreshOnDetailOpen(
                subscription(
                    syncStatus = SubscriptionSyncStatus(
                        state = SyncState.AuthFailed,
                        lastFailureAt = 1L,
                        lastError = "bad credentials"
                    )
                ),
                now = 10_000L
            )
        )
        assertFalse(
            policy.shouldAutoRefreshOnDetailOpen(
                subscription(
                    syncStatus = SubscriptionSyncStatus.syncing(
                        previous = SubscriptionSyncStatus.active(fetchedAt = 1L),
                        cause = SyncCause.ManualRefresh,
                        startedAt = 2L
                    )
                ),
                now = 10_000L
            )
        )
    }

    @Test
    fun shouldAutoRefreshOnDetailOpen_skipsSubscriptionsWithoutUsableCredentials() {
        assertFalse(
            policy.shouldAutoRefreshOnDetailOpen(
                subscription(
                    credentialState = CredentialState.Broken("keystore unavailable"),
                    syncStatus = SubscriptionSyncStatus(
                        state = SyncState.Stale,
                        lastSuccessAt = 1L
                    )
                ),
                now = 500_000L
            )
        )
    }

    private fun subscription(
        credentialState: CredentialState = CredentialState.Available,
        syncStatus: SubscriptionSyncStatus
    ): Subscription {
        return Subscription(
            id = 1L,
            provider = SubscriptionProvider.Supported(
                ProviderDescriptor(
                    id = "test-provider",
                    displayName = "Test Provider",
                    credentialFields = listOf(
                        CredentialFieldSpec(
                            key = "apiKey",
                            label = "API Key"
                        )
                    )
                )
            ),
            customTitle = null,
            credentialState = credentialState,
            syncStatus = syncStatus,
            createdAt = 0L
        )
    }
}
