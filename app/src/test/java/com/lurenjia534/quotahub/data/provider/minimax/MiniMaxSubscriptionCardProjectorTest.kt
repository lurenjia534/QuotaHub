package com.lurenjia534.quotahub.data.provider.minimax

import com.lurenjia534.quotahub.data.model.QuotaResource
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.QuotaBuckets
import com.lurenjia534.quotahub.data.model.QuotaUnit
import com.lurenjia534.quotahub.data.model.QuotaWindow
import com.lurenjia534.quotahub.data.model.QuotaWindowLabels
import com.lurenjia534.quotahub.data.model.ResourceRole
import com.lurenjia534.quotahub.data.model.ResourceType
import com.lurenjia534.quotahub.data.model.CredentialState
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.model.SubscriptionSyncStatus
import com.lurenjia534.quotahub.data.model.WindowScope
import com.lurenjia534.quotahub.data.provider.CredentialFieldSpec
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor
import org.junit.Assert.assertEquals
import org.junit.Test

class MiniMaxSubscriptionCardProjectorTest {
    private val projector = MiniMaxSubscriptionCardProjector()

    @Test
    fun project_usesPlanLevelWeeklyQuotaWhenAnchorResourceExists() {
        val projection = projector.project(
            subscription = subscription(),
            snapshot = QuotaSnapshot(
                fetchedAt = 1_000L,
                resources = listOf(
                    resource(
                        key = "MiniMax-M*",
                        intervalRemaining = 90,
                        intervalUsed = 10,
                        intervalResetAt = 1_500L,
                        weeklyRemaining = 80,
                        weeklyUsed = 120,
                        weeklyResetAt = 2_500L
                    ),
                    resource(
                        key = "coding-plan-vlm",
                        intervalRemaining = 60,
                        intervalUsed = 40,
                        intervalResetAt = 1_200L,
                        weeklyRemaining = 50,
                        weeklyUsed = 150,
                        weeklyResetAt = 2_000L
                    )
                )
            )
        )

        assertEquals("Usable left", projection.primaryMetric.label)
        assertEquals("130", projection.primaryMetric.value)
        assertEquals("Models", projection.secondaryMetric?.label)
        assertEquals("2", projection.secondaryMetric?.value)
        assertEquals(2, projection.resourceCount)
        assertEquals(1_200L, projection.nextResetAt)
        assertEquals(QuotaRisk.Healthy, projection.risk)
    }

    @Test
    fun project_fallsBackToIntervalQuotaWithoutWeeklyAnchor() {
        val projection = projector.project(
            subscription = subscription(),
            snapshot = QuotaSnapshot(
                fetchedAt = 1_000L,
                resources = listOf(
                    resource(
                        key = "MiniMax-Text-01",
                        intervalRemaining = 40,
                        intervalUsed = 60,
                        intervalResetAt = 3_000L,
                        weeklyRemaining = 5,
                        weeklyUsed = 195,
                        weeklyResetAt = 2_000L
                    )
                )
            )
        )

        assertEquals("Calls left", projection.primaryMetric.label)
        assertEquals("40", projection.primaryMetric.value)
        assertEquals("Models", projection.secondaryMetric?.label)
        assertEquals("1", projection.secondaryMetric?.value)
        assertEquals(1, projection.resourceCount)
        assertEquals(3_000L, projection.nextResetAt)
        assertEquals(QuotaRisk.Healthy, projection.risk)
    }

    private fun subscription(): Subscription {
        return Subscription(
            id = 1L,
            provider = ProviderDescriptor(
                id = MiniMaxCodingPlanProvider.ID,
                displayName = "MiniMax Coding Plan",
                credentialFields = listOf(
                    CredentialFieldSpec(
                        key = MiniMaxCodingPlanProvider.API_KEY_FIELD,
                        label = "API Key"
                    )
                )
            ),
            customTitle = null,
            credentialState = CredentialState.Available,
            syncStatus = SubscriptionSyncStatus.neverSynced(),
            createdAt = 0L
        )
    }

    private fun resource(
        key: String,
        intervalRemaining: Long,
        intervalUsed: Long,
        intervalResetAt: Long,
        weeklyRemaining: Long,
        weeklyUsed: Long,
        weeklyResetAt: Long
    ): QuotaResource {
        return QuotaResource(
            key = key,
            title = key,
            type = ResourceType.Model,
            role = if (key in MiniMaxWeeklyPlanAnchorResourceKeys) {
                ResourceRole.Anchor
            } else {
                ResourceRole.Limit
            },
            bucket = QuotaBuckets.ModelCalls,
            windows = listOf(
                QuotaWindow(
                    windowKey = "interval",
                    scope = WindowScope.Interval,
                    label = QuotaWindowLabels.Interval,
                    total = intervalRemaining + intervalUsed,
                    used = intervalUsed,
                    remaining = intervalRemaining,
                    resetAtEpochMillis = intervalResetAt,
                    unit = QuotaUnit.Request
                ),
                QuotaWindow(
                    windowKey = "weekly",
                    scope = WindowScope.Weekly,
                    label = QuotaWindowLabels.Weekly,
                    total = weeklyRemaining + weeklyUsed,
                    used = weeklyUsed,
                    remaining = weeklyRemaining,
                    resetAtEpochMillis = weeklyResetAt,
                    unit = QuotaUnit.Request
                )
            )
        )
    }
}
