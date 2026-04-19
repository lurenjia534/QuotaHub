package com.lurenjia534.quotahub.data.upgrade

import com.lurenjia534.quotahub.data.repository.QuotaSnapshotReplayBatchResult

interface QuotaSnapshotReplayRunner {
    suspend fun replayStoredQuotaSnapshotsNeedingUpgrade(): QuotaSnapshotReplayBatchResult
}
