package com.lurenjia534.quotahub.ui.screens.home

import com.lurenjia534.quotahub.data.model.CredentialState
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.model.SubscriptionProvider
import com.lurenjia534.quotahub.data.model.SubscriptionSyncStatus
import com.lurenjia534.quotahub.data.provider.zai.ZaiCodingPlanProvider
import com.lurenjia534.quotahub.data.provider.zai.ZaiUsageBundle
import com.lurenjia534.quotahub.data.provider.zai.sampleModelUsageResponse
import com.lurenjia534.quotahub.data.provider.zai.sampleQuotaLimitResponseWithoutUsageDetails
import com.lurenjia534.quotahub.data.provider.zai.sampleToolUsageResponseWithoutTotals
import com.lurenjia534.quotahub.data.provider.zai.sampleUsageBundle
import com.lurenjia534.quotahub.data.provider.zai.toQuotaSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class ZaiProviderQuotaDetailProjectorTest {
    private val projector = ZaiProviderQuotaDetailProjector()

    @Test
    fun project_surfacesQuotaLimitsAndUsageBreakdown() {
        val detail = projector.project(
            subscription = subscription(),
            snapshot = sampleUsageBundle().toQuotaSnapshot(fetchedAt = 1_000L)
        )

        assertEquals("Quota limits", detail.sectionTitle)
        assertEquals("86%", detail.summary?.headlineValue)
        assertEquals(QuotaRisk.Healthy, detail.summary?.risk)
        assertEquals(
            "Tracked Z.ai quota windows still have comfortable headroom.",
            detail.summary?.stateDescription
        )

        val tokenLimit = detail.resources.first { it.key == "limit:tokens_limit" }
        assertEquals("Token usage (5 hours)", tokenLimit.title)
        assertEquals("GLM-4.7", tokenLimit.secondaryMetrics[0].label)
        assertEquals("5,335,805 tok", tokenLimit.secondaryMetrics[0].value)
    }

    @Test
    fun project_fallsBackToSampledToolUsageWhenCurrentUsageDetailsAreMissing() {
        val detail = projector.project(
            subscription = subscription(),
            snapshot = ZaiUsageBundle(
                modelUsage = sampleModelUsageResponse(),
                toolUsage = sampleToolUsageResponseWithoutTotals(),
                quotaLimit = sampleQuotaLimitResponseWithoutUsageDetails()
            ).toQuotaSnapshot(fetchedAt = 1_000L)
        )

        val mcpLimit = detail.resources.first { it.key == "limit:time_limit" }
        assertEquals("Web Reader", mcpLimit.secondaryMetrics[0].label)
        assertEquals("8 uses", mcpLimit.secondaryMetrics[0].value)
        assertEquals("Network search", mcpLimit.secondaryMetrics[1].label)
        assertEquals("6 uses", mcpLimit.secondaryMetrics[1].value)
    }

    private fun subscription(): Subscription {
        return Subscription(
            id = 1L,
            provider = SubscriptionProvider.Supported(
                ZaiCodingPlanProvider(
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
