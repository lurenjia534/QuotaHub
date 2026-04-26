package com.lurenjia534.quotahub.data.provider.kimi

import com.lurenjia534.quotahub.data.model.CredentialState
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.model.SubscriptionProvider
import com.lurenjia534.quotahub.data.model.SubscriptionSyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class KimiSubscriptionCardProjectorTest {
    @Test
    fun project_surfacesRemainingRequestsAndParallelLimit() {
        val projection = KimiSubscriptionCardProjector().project(
            subscription = subscription(),
            snapshot = sampleKimiUsageResponse().toQuotaSnapshot(fetchedAt = 1_000L)
        )

        assertEquals("Plan left", projection.primaryMetric.label)
        assertEquals("75", projection.primaryMetric.value)
        assertEquals("Parallel", projection.secondaryMetric?.label)
        assertEquals("20", projection.secondaryMetric?.value)
        assertEquals(QuotaRisk.Healthy, projection.risk)
        assertEquals(1_777_219_428_011L, projection.nextResetAt)
    }

    private fun subscription(): Subscription {
        return Subscription(
            id = 1L,
            provider = SubscriptionProvider.Supported(
                KimiCodingPlanProvider(
                    apiService = object : KimiApiService {
                        override suspend fun getCodingUsage(authorization: String): KimiUsageResponse {
                            error("No network call expected")
                        }
                    }
                ).descriptor
            ),
            customTitle = null,
            credentialState = CredentialState.Available,
            syncStatus = SubscriptionSyncStatus.neverSynced(),
            createdAt = 0L
        )
    }
}
