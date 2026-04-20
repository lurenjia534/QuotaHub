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
        val currentFingerprints = providerCatalog.replayContractFingerprintsByProviderId
        if (currentFingerprints.isEmpty()) {
            return QuotaUpgradeExecutionResult(replayTriggered = false)
        }
        val statesByProviderId = upgradeStateDao.getStates().associateBy { it.providerId }
        val providersNeedingReplay = currentFingerprints.filter { (providerId, fingerprint) ->
            val state = statesByProviderId[providerId]
            state == null ||
                state.pendingReplay ||
                state.lastAppliedReplayFingerprint != fingerprint
        }.keys

        if (providersNeedingReplay.isEmpty()) {
            return QuotaUpgradeExecutionResult(replayTriggered = false)
        }

        val batchResult = replayRunner.replayStoredQuotaSnapshotsNeedingUpgrade()
        val failedProviderIds = batchResult.failures.map { it.providerId }.toSet()
        upgradeStateDao.upsertAll(
            providersNeedingReplay.map { providerId ->
                val previousState = statesByProviderId[providerId]
                QuotaUpgradeStateEntity(
                    providerId = providerId,
                    pendingReplay = providerId in failedProviderIds,
                    lastAppliedReplayFingerprint = if (providerId in failedProviderIds) {
                        previousState?.lastAppliedReplayFingerprint
                    } else {
                        currentFingerprints.getValue(providerId)
                    }
                )
            }
        )
        return QuotaUpgradeExecutionResult(
            replayTriggered = true,
            replayBatchResult = batchResult
        )
    }
}
