package com.lurenjia534.quotahub.data.provider.zhipu

import com.lurenjia534.quotahub.data.model.CredentialState
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.model.SubscriptionProvider
import com.lurenjia534.quotahub.data.model.SubscriptionSyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ZhipuSubscriptionCardProjectorTest {
    private val projector = ZhipuSubscriptionCardProjector()

    @Test
    fun project_usesLowestRemainingLimitAsHeadroom() {
        val projection = projector.project(
            subscription = subscription(),
            snapshot = sampleUsageBundle().toQuotaSnapshot(fetchedAt = 1_000L)
        )

        assertEquals("Headroom", projection.primaryMetric.label)
        assertEquals("86%", projection.primaryMetric.value)
        assertEquals("Limits", projection.secondaryMetric?.label)
        assertEquals("2", projection.secondaryMetric?.value)
        assertEquals(2, projection.resourceCount)
        assertEquals(1_776_610_539_070L, projection.nextResetAt)
        assertEquals(QuotaRisk.Healthy, projection.risk)
    }

    private fun subscription(): Subscription {
        return Subscription(
            id = 1L,
            provider = SubscriptionProvider.Supported(
                ZhipuCodingPlanProvider(
                    apiServiceFactory = { error("No network call expected") }
                ).descriptor
            ),
            customTitle = null,
            credentialState = CredentialState.Available,
            syncStatus = SubscriptionSyncStatus.neverSynced(),
            createdAt = 0L
        )
    }
}
