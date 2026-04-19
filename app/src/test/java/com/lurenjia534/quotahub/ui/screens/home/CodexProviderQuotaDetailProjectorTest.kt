package com.lurenjia534.quotahub.ui.screens.home

import com.lurenjia534.quotahub.data.model.CredentialState
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.model.SubscriptionSyncStatus
import com.lurenjia534.quotahub.data.provider.codex.CodexCodingPlanProvider
import com.lurenjia534.quotahub.data.provider.codex.sampleResponse
import com.lurenjia534.quotahub.data.provider.codex.toQuotaSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class CodexProviderQuotaDetailProjectorTest {
    private val projector = CodexProviderQuotaDetailProjector()

    @Test
    fun project_surfacesBucketHeadroomAndWindowLabels() {
        val detail = projector.project(
            subscription = subscription(),
            snapshot = sampleResponse().toQuotaSnapshot(fetchedAt = 1_000L)
        )

        assertEquals("Quota buckets", detail.sectionTitle)
        assertEquals(
            "Plan: Pro. Codex exposes quota windows as budget percentages. Pull down anytime to refresh remote values.",
            detail.sectionSubtitle
        )
        assertEquals("12%", detail.summary?.headlineValue)
        assertEquals(QuotaRisk.Watch, detail.summary?.risk)
        assertEquals(
            "Plan: Pro. Some Codex windows are trending low and should be monitored.",
            detail.summary?.stateDescription
        )
        assertEquals("Buckets", detail.summary?.primaryMetrics?.first?.label)
        assertEquals("2", detail.summary?.primaryMetrics?.first?.value)
        assertEquals("Peak usage", detail.summary?.primaryMetrics?.third?.label)
        assertEquals("88%", detail.summary?.primaryMetrics?.third?.value)

        val primaryBucket = detail.resources.first()
        assertEquals("OpenAI Codex", primaryBucket.title)
        assertEquals(QuotaRisk.Healthy, primaryBucket.risk)
        assertEquals("Daily left", primaryBucket.primaryMetrics[0].label)
        assertEquals("58%", primaryBucket.primaryMetrics[0].value)
        assertEquals("Weekly left", primaryBucket.secondaryMetrics[0].label)
        assertEquals("40%", primaryBucket.secondaryMetrics[0].value)

        val featureBucket = detail.resources.last()
        assertEquals("Other bucket", featureBucket.title)
        assertEquals(QuotaRisk.Watch, featureBucket.risk)
        assertEquals("5h 0m left", featureBucket.primaryMetrics[0].label)
        assertEquals("12%", featureBucket.primaryMetrics[0].value)
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
