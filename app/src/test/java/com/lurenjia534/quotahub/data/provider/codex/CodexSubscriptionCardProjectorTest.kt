package com.lurenjia534.quotahub.data.provider.codex

import com.lurenjia534.quotahub.data.model.CredentialState
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.Subscription
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
    }

    private fun subscription(): Subscription {
        return Subscription(
            id = 1L,
            provider = CodexCodingPlanProvider().descriptor,
            customTitle = null,
            credentialState = CredentialState.Available,
            syncStatus = SubscriptionSyncStatus.neverSynced(),
            createdAt = 0L
        )
    }
}
