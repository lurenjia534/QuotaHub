package com.lurenjia534.quotahub.bootstrap.provider

import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.QuotaRisk
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.provider.CapturedQuotaSnapshot
import com.lurenjia534.quotahub.data.provider.CardMetric
import com.lurenjia534.quotahub.data.provider.CodingPlanProvider
import com.lurenjia534.quotahub.data.provider.CredentialFieldSpec
import com.lurenjia534.quotahub.data.provider.ProviderCatalog
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor
import com.lurenjia534.quotahub.data.provider.ProviderReplayPayload
import com.lurenjia534.quotahub.data.provider.SecretBundle
import com.lurenjia534.quotahub.data.provider.SubscriptionCardProjection
import com.lurenjia534.quotahub.data.provider.SubscriptionCardProjector
import com.lurenjia534.quotahub.ui.provider.ProviderUiMetadata
import com.lurenjia534.quotahub.ui.screens.home.LabeledValueUiModel
import com.lurenjia534.quotahub.ui.screens.home.ProviderQuotaDetailProjector
import com.lurenjia534.quotahub.ui.screens.home.ProviderQuotaDetailUiModel
import com.lurenjia534.quotahub.ui.screens.home.ProviderQuotaSummaryUiModel
import com.lurenjia534.quotahub.ui.screens.home.SummaryMetricRowUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ProviderModuleValidationTest {
    @Test
    fun requireValidProviderModules_rejectsDuplicateProviderIds() {
        val duplicateModules = listOf(
            module(providerId = "duplicate"),
            module(providerId = "duplicate")
        )

        try {
            requireValidProviderModules(duplicateModules)
            fail("Expected duplicate provider ids to be rejected")
        } catch (error: IllegalArgumentException) {
            assertEquals("Duplicate provider module ids: duplicate", error.message)
        }
    }

    @Test
    fun providerCatalog_rejectsDuplicateProviderIds() {
        try {
            ProviderCatalog(
                providers = listOf(
                    FakeProvider("duplicate"),
                    FakeProvider("duplicate")
                )
            )
            fail("Expected duplicate provider ids to be rejected")
        } catch (error: IllegalArgumentException) {
            assertEquals("Duplicate provider ids: duplicate", error.message)
        }
    }

    @Test
    fun providerModules_includeCodexProvider() {
        assertTrue(
            ProviderModules.all.any { module ->
                module.provider.descriptor.id == "codex"
            }
        )
    }

    @Test
    fun providerModules_includeZhipuProvider() {
        assertTrue(
            ProviderModules.all.any { module ->
                module.provider.descriptor.id == "zhipu"
            }
        )
    }

    private fun module(providerId: String): ProviderModule {
        return ProviderModule(
            provider = FakeProvider(providerId),
            uiMetadata = ProviderUiMetadata(
                subtitle = "test.example",
                iconRes = 0,
                connectDescription = "Connect",
                detailDescription = "Monitor"
            ),
            cardProjector = FakeCardProjector(),
            detailProjector = FakeDetailProjector()
        )
    }
}

private class FakeProvider(providerId: String) : CodingPlanProvider {
    override val descriptor: ProviderDescriptor = ProviderDescriptor(
        id = providerId,
        displayName = providerId,
        credentialFields = listOf(
            CredentialFieldSpec(
                key = "apiKey",
                label = "API Key"
            )
        )
    )

    override suspend fun validate(credentials: SecretBundle): Result<CapturedQuotaSnapshot> {
        return Result.success(CapturedQuotaSnapshot(QuotaSnapshot.empty()))
    }

    override suspend fun fetchSnapshot(
        subscription: Subscription,
        credentials: SecretBundle
    ): Result<CapturedQuotaSnapshot> {
        return Result.success(CapturedQuotaSnapshot(QuotaSnapshot.empty()))
    }

    override fun replay(payload: ProviderReplayPayload): Result<CapturedQuotaSnapshot> {
        return Result.success(CapturedQuotaSnapshot(QuotaSnapshot.empty()))
    }
}

private class FakeCardProjector : SubscriptionCardProjector {
    override fun project(
        subscription: Subscription,
        snapshot: QuotaSnapshot
    ): SubscriptionCardProjection {
        return SubscriptionCardProjection(
            primaryMetric = CardMetric(label = "Quota", value = "0"),
            secondaryMetric = null,
            resourceCount = 0,
            nextResetAt = null,
            risk = QuotaRisk.Healthy
        )
    }
}

private class FakeDetailProjector : ProviderQuotaDetailProjector {
    override fun project(
        subscription: Subscription,
        snapshot: QuotaSnapshot
    ): ProviderQuotaDetailUiModel {
        return ProviderQuotaDetailUiModel(
            summary = ProviderQuotaSummaryUiModel(
                risk = QuotaRisk.Healthy,
                syncLabel = "Ready",
                syncDescription = "Ready",
                headlineValue = "0",
                headlineLabel = "Quota",
                stateLabel = "Ready",
                stateDescription = "Ready",
                primaryMetrics = SummaryMetricRowUiModel(
                    first = LabeledValueUiModel("A", "0"),
                    second = LabeledValueUiModel("B", "0"),
                    third = LabeledValueUiModel("C", "0")
                )
            )
        )
    }
}
