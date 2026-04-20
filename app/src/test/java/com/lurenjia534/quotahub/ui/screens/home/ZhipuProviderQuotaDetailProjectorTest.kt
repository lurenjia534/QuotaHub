package com.lurenjia534.quotahub.ui.screens.home

import com.lurenjia534.quotahub.data.model.CredentialState
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.model.SubscriptionProvider
import com.lurenjia534.quotahub.data.model.SubscriptionSyncStatus
import com.lurenjia534.quotahub.data.provider.monitor.MonitorQuotaDetailProjector
import com.lurenjia534.quotahub.data.provider.zhipu.ZhipuCodingPlanProvider
import com.lurenjia534.quotahub.data.provider.zhipu.ZhipuUsageBundle
import com.lurenjia534.quotahub.data.provider.zhipu.sampleModelUsageResponse
import com.lurenjia534.quotahub.data.provider.zhipu.sampleQuotaLimitResponseWithoutUsageDetails
import com.lurenjia534.quotahub.data.provider.zhipu.sampleToolUsageResponseWithoutTotals
import com.lurenjia534.quotahub.data.provider.zhipu.sampleUsageBundle
import com.lurenjia534.quotahub.data.provider.zhipu.toQuotaSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class ZhipuProviderQuotaDetailProjectorTest {
    private val projector = MonitorQuotaDetailProjector(providerName = "Zhipu")

    @Test
    fun project_surfacesQuotaLimitsAndUsageBreakdown() {
        val detail = projector.project(
            subscription = subscription(),
            snapshot = sampleUsageBundle().toQuotaSnapshot(fetchedAt = 1_000L)
        )

        assertEquals("Quota limits", detail.sectionTitle)
        assertEquals("86%", detail.summary?.headlineValue)
        assertEquals(QuotaRisk.Healthy, detail.summary?.risk)
        assertEquals("Limits", detail.summary?.primaryMetrics?.first?.label)
        assertEquals("2", detail.summary?.primaryMetrics?.first?.value)
        assertEquals("Models", detail.summary?.secondaryMetrics?.first?.label)
        assertEquals("1", detail.summary?.secondaryMetrics?.first?.value)
        assertEquals("Tokens used", detail.summary?.secondaryMetrics?.second?.label)
        assertEquals("5,335,805", detail.summary?.secondaryMetrics?.second?.value)
        assertEquals("Tool uses", detail.summary?.secondaryMetrics?.third?.label)
        assertEquals("14", detail.summary?.secondaryMetrics?.third?.value)

        val tokenLimit = detail.resources.first { it.key == "limit:tokens_limit" }
        assertEquals("Token usage (5 hours)", tokenLimit.title)
        assertEquals("Headroom", tokenLimit.primaryMetrics[0].label)
        assertEquals("95%", tokenLimit.primaryMetrics[0].value)
        assertEquals("GLM-4.7", tokenLimit.secondaryMetrics[0].label)
        assertEquals("5,335,805 tok", tokenLimit.secondaryMetrics[0].value)

        val mcpLimit = detail.resources.first { it.key == "limit:time_limit" }
        assertEquals("MCP usage (1 month)", mcpLimit.title)
        assertEquals("86%", mcpLimit.primaryMetrics[0].value)
        assertEquals("Web Reader", mcpLimit.secondaryMetrics[0].label)
        assertEquals("8 uses", mcpLimit.secondaryMetrics[0].value)
    }

    @Test
    fun project_fallsBackToSampledToolUsageWhenCurrentUsageDetailsAreMissing() {
        val detail = projector.project(
            subscription = subscription(),
            snapshot = ZhipuUsageBundle(
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
