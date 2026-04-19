package com.lurenjia534.quotahub.data.upgrade

import com.lurenjia534.quotahub.data.local.QuotaUpgradeStateDao
import com.lurenjia534.quotahub.data.local.QuotaUpgradeStateEntity
import com.lurenjia534.quotahub.data.provider.CapturedQuotaSnapshot
import com.lurenjia534.quotahub.data.provider.CodingPlanProvider
import com.lurenjia534.quotahub.data.provider.CredentialFieldSpec
import com.lurenjia534.quotahub.data.provider.ProviderCatalog
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor
import com.lurenjia534.quotahub.data.provider.ProviderReplayPayload
import com.lurenjia534.quotahub.data.provider.ProviderReplaySupport
import com.lurenjia534.quotahub.data.provider.SecretBundle
import com.lurenjia534.quotahub.data.repository.QuotaSnapshotReplayBatchResult
import com.lurenjia534.quotahub.data.repository.QuotaSnapshotReplayFailure
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.Subscription
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuotaUpgradeCoordinatorTest {
    @Test
    fun runPendingUpgrades_skipsReplayWhenFingerprintAlreadyApplied() = runBlocking {
        val stateDao = FakeQuotaUpgradeStateDao(
            QuotaUpgradeStateEntity(
                pendingReplay = false,
                lastAppliedReplayFingerprint = CURRENT_FINGERPRINT
            )
        )
        val replayRunner = FakeReplayRunner(
            QuotaSnapshotReplayBatchResult(
                checked = 0,
                replayed = 0,
                skipped = 0,
                failures = emptyList()
            )
        )
        val coordinator = QuotaUpgradeCoordinator(
            replayRunner = replayRunner,
            upgradeStateDao = stateDao,
            providerCatalog = providerCatalog()
        )

        val result = coordinator.runPendingUpgrades()

        assertFalse(result.replayTriggered)
        assertEquals(0, replayRunner.callCount)
        assertEquals(CURRENT_FINGERPRINT, stateDao.state?.lastAppliedReplayFingerprint)
        assertFalse(stateDao.state?.pendingReplay ?: true)
    }

    @Test
    fun runPendingUpgrades_clearsPendingReplayAfterSuccessfulReplay() = runBlocking {
        val stateDao = FakeQuotaUpgradeStateDao(
            QuotaUpgradeStateEntity(
                pendingReplay = true,
                lastAppliedReplayFingerprint = "minimax:v0"
            )
        )
        val replayRunner = FakeReplayRunner(
            QuotaSnapshotReplayBatchResult(
                checked = 1,
                replayed = 1,
                skipped = 0,
                failures = emptyList()
            )
        )
        val coordinator = QuotaUpgradeCoordinator(
            replayRunner = replayRunner,
            upgradeStateDao = stateDao,
            providerCatalog = providerCatalog()
        )

        val result = coordinator.runPendingUpgrades()

        assertTrue(result.replayTriggered)
        assertEquals(1, replayRunner.callCount)
        assertNotNull(result.replayBatchResult)
        assertEquals(CURRENT_FINGERPRINT, stateDao.state?.lastAppliedReplayFingerprint)
        assertFalse(stateDao.state?.pendingReplay ?: true)
    }

    @Test
    fun runPendingUpgrades_preservesPreviousFingerprintWhenReplayFails() = runBlocking {
        val previousFingerprint = "minimax:minimax/raw-quota@v1:1"
        val stateDao = FakeQuotaUpgradeStateDao(
            QuotaUpgradeStateEntity(
                pendingReplay = false,
                lastAppliedReplayFingerprint = previousFingerprint
            )
        )
        val replayRunner = FakeReplayRunner(
            QuotaSnapshotReplayBatchResult(
                checked = 1,
                replayed = 0,
                skipped = 0,
                failures = listOf(
                    QuotaSnapshotReplayFailure(
                        subscriptionId = 1L,
                        providerId = "minimax",
                        reason = "decode failed"
                    )
                )
            )
        )
        val coordinator = QuotaUpgradeCoordinator(
            replayRunner = replayRunner,
            upgradeStateDao = stateDao,
            providerCatalog = providerCatalog(normalizerVersion = 2)
        )

        val result = coordinator.runPendingUpgrades()

        assertTrue(result.replayTriggered)
        assertEquals(1, replayRunner.callCount)
        assertEquals(previousFingerprint, stateDao.state?.lastAppliedReplayFingerprint)
        assertTrue(stateDao.state?.pendingReplay ?: false)
    }

    @Test
    fun runPendingUpgrades_initializesFingerprintWhenStateMissing() = runBlocking {
        val stateDao = FakeQuotaUpgradeStateDao()
        val replayRunner = FakeReplayRunner(
            QuotaSnapshotReplayBatchResult(
                checked = 0,
                replayed = 0,
                skipped = 0,
                failures = emptyList()
            )
        )
        val coordinator = QuotaUpgradeCoordinator(
            replayRunner = replayRunner,
            upgradeStateDao = stateDao,
            providerCatalog = providerCatalog()
        )

        val result = coordinator.runPendingUpgrades()

        assertTrue(result.replayTriggered)
        assertEquals(1, replayRunner.callCount)
        assertEquals(CURRENT_FINGERPRINT, stateDao.state?.lastAppliedReplayFingerprint)
        assertFalse(stateDao.state?.pendingReplay ?: true)
    }

    private class FakeQuotaUpgradeStateDao(
        initialState: QuotaUpgradeStateEntity? = null
    ) : QuotaUpgradeStateDao {
        var state: QuotaUpgradeStateEntity? = initialState

        override suspend fun getState(): QuotaUpgradeStateEntity? = state

        override suspend fun upsert(state: QuotaUpgradeStateEntity) {
            this.state = state
        }
    }

    private class FakeReplayRunner(
        private val batchResult: QuotaSnapshotReplayBatchResult
    ) : QuotaSnapshotReplayRunner {
        var callCount: Int = 0
            private set

        override suspend fun replayStoredQuotaSnapshotsNeedingUpgrade(): QuotaSnapshotReplayBatchResult {
            callCount += 1
            return batchResult
        }
    }

    private class FakeCodingPlanProvider(
        override val descriptor: ProviderDescriptor,
        override val replaySupport: ProviderReplaySupport?
    ) : CodingPlanProvider {
        override suspend fun validate(credentials: SecretBundle): Result<CapturedQuotaSnapshot> {
            return Result.failure(NotImplementedError())
        }

        override suspend fun fetchSnapshot(subscription: Subscription): Result<CapturedQuotaSnapshot> {
            return Result.failure(NotImplementedError())
        }

        override fun replay(payload: ProviderReplayPayload): Result<CapturedQuotaSnapshot> {
            return Result.success(CapturedQuotaSnapshot(QuotaSnapshot.empty()))
        }
    }

    private fun providerCatalog(normalizerVersion: Int = 1): ProviderCatalog {
        return ProviderCatalog(
            providers = listOf(
                FakeCodingPlanProvider(
                    descriptor = ProviderDescriptor(
                        id = "minimax",
                        displayName = "MiniMax",
                        credentialFields = listOf(
                            CredentialFieldSpec(
                                key = "apiKey",
                                label = "API Key"
                            )
                        )
                    ),
                    replaySupport = ProviderReplaySupport(
                        payloadFormat = "minimax/raw-quota@v1",
                        normalizerVersion = normalizerVersion
                    )
                )
            )
        )
    }

    private companion object {
        const val CURRENT_FINGERPRINT = "minimax:minimax/raw-quota@v1:1"
    }
}
