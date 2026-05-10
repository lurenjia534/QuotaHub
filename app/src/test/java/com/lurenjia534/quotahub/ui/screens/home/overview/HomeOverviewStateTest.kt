package com.lurenjia534.quotahub.ui.screens.home.overview

import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.SyncState
import com.lurenjia534.quotahub.data.provider.CardMetric
import com.lurenjia534.quotahub.data.provider.QuotaProgressMetric
import com.lurenjia534.quotahub.ui.screens.home.SubscriptionCardUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeOverviewStateTest {
    @Test
    fun dominantRisk_prioritizesCriticalThenWatch() {
        val cards = listOf(
            subscriptionCard(risk = QuotaRisk.Healthy),
            subscriptionCard(risk = QuotaRisk.Watch),
            subscriptionCard(risk = QuotaRisk.Critical)
        )

        assertEquals(QuotaRisk.Critical, cards.dominantRisk())
        assertEquals(QuotaRisk.Watch, cards.dropLast(1).dominantRisk())
        assertEquals(QuotaRisk.Healthy, listOf(subscriptionCard()).dominantRisk())
    }

    @Test
    fun dominantSyncState_usesHomePriorityOrder() {
        val cards = listOf(
            subscriptionCard(syncState = SyncState.Active),
            subscriptionCard(syncState = SyncState.NeverSynced),
            subscriptionCard(syncState = SyncState.Syncing),
            subscriptionCard(syncState = SyncState.Stale),
            subscriptionCard(syncState = SyncState.SyncError),
            subscriptionCard(syncState = SyncState.AuthFailed)
        )

        assertEquals(SyncState.AuthFailed, cards.dominantSyncState())
        assertEquals(SyncState.SyncError, cards.dropLast(1).dominantSyncState())
        assertEquals(SyncState.Stale, cards.dropLast(2).dominantSyncState())
        assertEquals(SyncState.Syncing, cards.dropLast(3).dominantSyncState())
        assertEquals(SyncState.NeverSynced, cards.dropLast(4).dominantSyncState())
        assertEquals(SyncState.Active, cards.dropLast(5).dominantSyncState())
    }

    @Test
    fun sourceGroups_placesCloudBeforeLocalAndKeepsLabels() {
        val groups = listOf(
            subscriptionCard(subscriptionId = 1L, isCloudSynced = false),
            subscriptionCard(subscriptionId = 2L, isCloudSynced = true)
        ).sourceGroups()

        assertEquals(listOf("cloud", "local"), groups.map { it.key })
        assertEquals("Cloud subscriptions", groups[0].title)
        assertEquals("Relay", groups[0].label)
        assertEquals(listOf(2L), groups[0].cards.map { it.subscriptionId })
        assertEquals("Local subscriptions", groups[1].title)
        assertEquals("Device", groups[1].label)
        assertEquals(listOf(1L), groups[1].cards.map { it.subscriptionId })
    }

    @Test
    fun landscapeMetricOrder_prioritizesWindowLabels() {
        val ordered = listOf(
            progressMetric("Other"),
            progressMetric("Plan monthly"),
            progressMetric("Weekly"),
            progressMetric("5h window")
        ).landscapeMetricOrder()

        assertEquals(listOf("5h window", "Weekly", "Plan monthly", "Other"), ordered.map { it.label })
    }

    @Test
    fun percentValueOrNull_parsesAndBoundsPercentStrings() {
        assertEquals(35f, "35%".percentValueOrNull())
        assertEquals(100f, "140%".percentValueOrNull())
        assertEquals(0f, "-3%".percentValueOrNull())
        assertNull("manual".percentValueOrNull())
    }

    @Test
    fun providerSummary_reportsLinkedAccountsResourcesAndDominantSync() {
        val cards = listOf(
            subscriptionCard(resourceCount = 2, syncState = SyncState.Active),
            subscriptionCard(resourceCount = 3, syncState = SyncState.Stale)
        )

        assertEquals("2 linked", cards.providerStatusLabel(isConnected = true))
        assertEquals("2 accounts / 5 resources / stale", cards.providerAccessSummary())
        assertEquals("Linked", listOf(subscriptionCard()).providerStatusLabel(isConnected = true))
        assertEquals("Open", emptyList<SubscriptionCardUiModel>().providerStatusLabel(isConnected = false))
        assertEquals("Ready to connect", emptyList<SubscriptionCardUiModel>().providerAccessSummary())
    }

    private fun subscriptionCard(
        subscriptionId: Long = 1L,
        risk: QuotaRisk = QuotaRisk.Healthy,
        syncState: SyncState = SyncState.Active,
        isCloudSynced: Boolean = false,
        resourceCount: Int = 1
    ): SubscriptionCardUiModel {
        return SubscriptionCardUiModel(
            subscriptionId = subscriptionId,
            providerId = "provider",
            displayTitle = "Provider",
            subtitle = "Provider subtitle",
            providerIconRes = 0,
            primaryMetric = CardMetric("Quota", "50%"),
            secondaryMetric = CardMetric("Resources", resourceCount.toString()),
            resourceCount = resourceCount,
            nextResetAt = null,
            hubProgressMetrics = emptyList(),
            risk = risk,
            syncState = syncState,
            syncLabel = syncState.name,
            syncDescription = syncState.name,
            isConnected = syncState != SyncState.AuthFailed,
            canOpenDetail = true,
            isCloudSynced = isCloudSynced,
            sourceLabel = if (isCloudSynced) "Cloud" else "Local",
            sourceDescription = if (isCloudSynced) "Managed by QuotaHub Relay" else "Added on this device"
        )
    }

    private fun progressMetric(label: String): QuotaProgressMetric {
        return QuotaProgressMetric(
            label = label,
            used = 1L,
            total = 2L,
            remaining = 1L,
            resetAtEpochMillis = null
        )
    }
}
