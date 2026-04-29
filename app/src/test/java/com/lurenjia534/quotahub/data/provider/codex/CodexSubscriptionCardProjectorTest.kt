package com.lurenjia534.quotahub.data.provider.codex

import com.lurenjia534.quotahub.data.model.CredentialState
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.model.SubscriptionProvider
import com.lurenjia534.quotahub.data.model.SubscriptionSyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class CodexSubscriptionCardProjectorTest {
    private val projector = CodexSubscriptionCardProjector()

    @Test
    fun project_usesLowestRemainingWindowAsHeadroom() {
        val projection = projector.project(
            subscription = subscription(),
            snapshot = sampleResponse().toQuotaSnapshot(fetchedAt = 1_000L)
        )

        assertEquals("Headroom", projection.primaryMetric.label)
        assertEquals("12%", projection.primaryMetric.value)
        assertEquals("Buckets", projection.secondaryMetric?.label)
        assertEquals("2", projection.secondaryMetric?.value)
        assertEquals(2, projection.resourceCount)
        assertEquals(1_900_000_000L, projection.nextResetAt)
        assertEquals(QuotaRisk.Watch, projection.risk)
        assertEquals(2, projection.hubProgressMetrics.size)
        assertEquals("5h window", projection.hubProgressMetrics[0].label)
        assertEquals(88L, projection.hubProgressMetrics[0].used)
        assertEquals(100L, projection.hubProgressMetrics[0].total)
        assertEquals(12L, projection.hubProgressMetrics[0].remaining)
        assertEquals(1_900_000_000L, projection.hubProgressMetrics[0].resetAtEpochMillis)
        assertEquals("Weekly limit", projection.hubProgressMetrics[1].label)
        assertEquals(60L, projection.hubProgressMetrics[1].used)
        assertEquals(100L, projection.hubProgressMetrics[1].total)
        assertEquals(40L, projection.hubProgressMetrics[1].remaining)
        assertEquals(2_600_000_000L, projection.hubProgressMetrics[1].resetAtEpochMillis)
    }

    private fun subscription(): Subscription {
        return Subscription(
            id = 1L,
            provider = SubscriptionProvider.Supported(CodexCodingPlanProvider().descriptor),
            customTitle = null,
            credentialState = CredentialState.Available,
            syncStatus = SubscriptionSyncStatus.neverSynced(),
            createdAt = 0L
        )
    }
}
