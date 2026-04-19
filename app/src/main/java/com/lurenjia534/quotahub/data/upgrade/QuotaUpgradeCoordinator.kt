package com.lurenjia534.quotahub.data.upgrade

import com.lurenjia534.quotahub.data.local.QuotaUpgradeStateDao
import com.lurenjia534.quotahub.data.local.QuotaUpgradeStateEntity
import com.lurenjia534.quotahub.data.provider.ProviderCatalog
import com.lurenjia534.quotahub.data.repository.QuotaSnapshotReplayBatchResult

data class QuotaUpgradeExecutionResult(
    val replayTriggered: Boolean,
    val replayBatchResult: QuotaSnapshotReplayBatchResult? = null
)

class QuotaUpgradeCoordinator(
    private val replayRunner: QuotaSnapshotReplayRunner,
    private val upgradeStateDao: QuotaUpgradeStateDao,
    private val providerCatalog: ProviderCatalog
) {
    suspend fun runPendingUpgrades(): QuotaUpgradeExecutionResult {
        val state = upgradeStateDao.getState()
        val currentFingerprint = providerCatalog.replayContractFingerprint
        val shouldReplay = state == null ||
            state.pendingReplay ||
            state.lastAppliedReplayFingerprint != currentFingerprint

        if (!shouldReplay) {
            return QuotaUpgradeExecutionResult(replayTriggered = false)
        }

        val batchResult = replayRunner.replayStoredQuotaSnapshotsNeedingUpgrade()
        val hasFailures = batchResult.failures.isNotEmpty()
        upgradeStateDao.upsert(
            QuotaUpgradeStateEntity(
                pendingReplay = hasFailures,
                lastAppliedReplayFingerprint = if (hasFailures) {
                    state?.lastAppliedReplayFingerprint
                } else {
                    currentFingerprint
                }
            )
        )
        return QuotaUpgradeExecutionResult(
            replayTriggered = true,
            replayBatchResult = batchResult
        )
    }
}
