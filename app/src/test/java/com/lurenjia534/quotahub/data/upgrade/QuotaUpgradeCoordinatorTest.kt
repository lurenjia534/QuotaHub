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
    fun runPendingUpgrades_skipsReplayWhenTrackedFingerprintsAlreadyApplied() = runBlocking {
        val stateDao = FakeQuotaUpgradeStateDao(
            listOf(
                QuotaUpgradeStateEntity(
                    providerId = "minimax",
                    pendingReplay = false,
                    lastAppliedReplayFingerprint = fingerprintFor("minimax", 1)
                )
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
            providerCatalog = providerCatalog(
                ReplayProviderSpec(id = "minimax", normalizerVersion = 1)
            )
        )

        val result = coordinator.runPendingUpgrades()

        assertFalse(result.replayTriggered)
        assertEquals(0, replayRunner.callCount)
        assertEquals(
            fingerprintFor("minimax", 1),
            stateDao.state("minimax")?.lastAppliedReplayFingerprint
        )
        assertFalse(stateDao.state("minimax")?.pendingReplay ?: true)
    }

    @Test
    fun runPendingUpgrades_clearsPendingReplayAfterSuccessfulReplay() = runBlocking {
        val stateDao = FakeQuotaUpgradeStateDao(
            listOf(
                QuotaUpgradeStateEntity(
                    providerId = "minimax",
                    pendingReplay = true,
                    lastAppliedReplayFingerprint = "minimax:v0"
                )
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
            providerCatalog = providerCatalog(
                ReplayProviderSpec(id = "minimax", normalizerVersion = 1)
            )
        )

        val result = coordinator.runPendingUpgrades()

        assertTrue(result.replayTriggered)
        assertEquals(1, replayRunner.callCount)
        assertNotNull(result.replayBatchResult)
        assertEquals(
            fingerprintFor("minimax", 1),
            stateDao.state("minimax")?.lastAppliedReplayFingerprint
        )
        assertFalse(stateDao.state("minimax")?.pendingReplay ?: true)
    }

    @Test
    fun runPendingUpgrades_preservesPreviousFingerprintOnlyForFailingProviders() = runBlocking {
        val previousMinimaxFingerprint = fingerprintFor("minimax", 1)
        val previousCodexFingerprint = fingerprintFor("codex", 1)
        val stateDao = FakeQuotaUpgradeStateDao(
            listOf(
                QuotaUpgradeStateEntity(
                    providerId = "minimax",
                    pendingReplay = false,
                    lastAppliedReplayFingerprint = previousMinimaxFingerprint
                ),
                QuotaUpgradeStateEntity(
                    providerId = "codex",
                    pendingReplay = false,
                    lastAppliedReplayFingerprint = previousCodexFingerprint
                )
            )
        )
        val replayRunner = FakeReplayRunner(
            QuotaSnapshotReplayBatchResult(
                checked = 2,
                replayed = 1,
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
            providerCatalog = providerCatalog(
                ReplayProviderSpec(id = "minimax", normalizerVersion = 2),
                ReplayProviderSpec(id = "codex", normalizerVersion = 2)
            )
        )

        val result = coordinator.runPendingUpgrades()

        assertTrue(result.replayTriggered)
        assertEquals(1, replayRunner.callCount)
        assertEquals(
            previousMinimaxFingerprint,
            stateDao.state("minimax")?.lastAppliedReplayFingerprint
        )
        assertTrue(stateDao.state("minimax")?.pendingReplay ?: false)
        assertEquals(
            fingerprintFor("codex", 2),
            stateDao.state("codex")?.lastAppliedReplayFingerprint
        )
        assertFalse(stateDao.state("codex")?.pendingReplay ?: true)
    }

    @Test
    fun runPendingUpgrades_triggersReplayWhenTrackedProviderStateMissing() = runBlocking {
        val stateDao = FakeQuotaUpgradeStateDao(
            listOf(
                QuotaUpgradeStateEntity(
                    providerId = "minimax",
                    pendingReplay = false,
                    lastAppliedReplayFingerprint = fingerprintFor("minimax", 1)
                )
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
            providerCatalog = providerCatalog(
                ReplayProviderSpec(id = "minimax", normalizerVersion = 1),
                ReplayProviderSpec(id = "codex", normalizerVersion = 1)
            )
        )

        val result = coordinator.runPendingUpgrades()

        assertTrue(result.replayTriggered)
        assertEquals(1, replayRunner.callCount)
        assertEquals(
            fingerprintFor("minimax", 1),
            stateDao.state("minimax")?.lastAppliedReplayFingerprint
        )
        assertEquals(
            fingerprintFor("codex", 1),
            stateDao.state("codex")?.lastAppliedReplayFingerprint
        )
        assertFalse(stateDao.state("codex")?.pendingReplay ?: true)
    }

    private class FakeQuotaUpgradeStateDao(
        initialStates: List<QuotaUpgradeStateEntity> = emptyList()
    ) : QuotaUpgradeStateDao {
        private val statesByProviderId = initialStates
            .associateBy { it.providerId }
            .toMutableMap()

        override suspend fun getStates(): List<QuotaUpgradeStateEntity> {
            return statesByProviderId.values.sortedBy { it.providerId }
        }

        override suspend fun upsert(state: QuotaUpgradeStateEntity) {
            statesByProviderId[state.providerId] = state
        }

        override suspend fun upsertAll(states: List<QuotaUpgradeStateEntity>) {
            for (state in states) {
                upsert(state)
            }
        }

        fun state(providerId: String): QuotaUpgradeStateEntity? {
            return statesByProviderId[providerId]
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

        override suspend fun fetchSnapshot(
            subscription: Subscription,
            credentials: SecretBundle
        ): Result<CapturedQuotaSnapshot> {
            return Result.failure(NotImplementedError())
        }

        override fun replay(payload: ProviderReplayPayload): Result<CapturedQuotaSnapshot> {
            return Result.success(CapturedQuotaSnapshot(QuotaSnapshot.empty()))
        }
    }

    private fun providerCatalog(vararg specs: ReplayProviderSpec): ProviderCatalog {
        return ProviderCatalog(
            providers = specs.map { spec ->
                FakeCodingPlanProvider(
                    descriptor = ProviderDescriptor(
                        id = spec.id,
                        displayName = spec.displayName,
                        credentialFields = listOf(
                            CredentialFieldSpec(
                                key = "apiKey",
                                label = "API Key"
                            )
                        )
                    ),
                    replaySupport = ProviderReplaySupport(
                        payloadFormat = spec.payloadFormat,
                        normalizerVersion = spec.normalizerVersion
                    )
                )
            }
        )
    }

    private data class ReplayProviderSpec(
        val id: String,
        val normalizerVersion: Int,
        val displayName: String = id.replaceFirstChar { it.uppercase() },
        val payloadFormat: String = "$id/raw-quota@v1"
    )

    private fun fingerprintFor(providerId: String, normalizerVersion: Int): String {
        return "$providerId:${providerId}/raw-quota@v1:$normalizerVersion"
    }
}
