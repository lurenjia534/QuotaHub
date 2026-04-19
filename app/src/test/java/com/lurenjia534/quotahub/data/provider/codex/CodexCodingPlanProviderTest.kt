package com.lurenjia534.quotahub.data.provider.codex

import com.lurenjia534.quotahub.data.model.QuotaUnit
import com.lurenjia534.quotahub.data.model.ResourceType
import com.lurenjia534.quotahub.data.model.WindowScope
import com.lurenjia534.quotahub.data.provider.ProviderReplayPayload
import com.lurenjia534.quotahub.data.provider.SecretBundle
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodexCodingPlanProviderTest {
    @Test
    fun validate_requestsUsageSnapshotWithAuthorizationAndOptionalAccountId() = runBlocking {
        val apiService = FakeCodexApiService(sampleResponse())
        val provider = CodexCodingPlanProvider(apiService = apiService)

        val capturedSnapshot = provider.validate(
            SecretBundle.of(
                mapOf(
                    CodexCodingPlanProvider.ACCESS_TOKEN_FIELD to "token-123",
                    CodexCodingPlanProvider.ACCOUNT_ID_FIELD to "account-42"
                )
            )
        ).getOrThrow()

        assertEquals("Bearer token-123", apiService.authorization)
        assertEquals("account-42", apiService.accountId)
        assertEquals(CodexCodingPlanProvider.RAW_PAYLOAD_FORMAT, capturedSnapshot.replayPayload?.payloadFormat)
        assertEquals(2, capturedSnapshot.snapshot.resources.size)

        val primaryResource = capturedSnapshot.snapshot.resources.first()
        assertEquals("codex", primaryResource.key)
        assertEquals(ResourceType.Plan, primaryResource.type)
        assertEquals(2, primaryResource.windows.size)
        assertEquals(WindowScope.Daily, primaryResource.windows.first().scope)
        assertEquals(WindowScope.Weekly, primaryResource.windows.last().scope)
        assertEquals(QuotaUnit.Percent, primaryResource.windows.first().unit)

        val featureResource = capturedSnapshot.snapshot.resources.last()
        assertEquals(ResourceType.Feature, featureResource.type)
        assertEquals("feature-a", featureResource.key)
        assertEquals(12L, featureResource.windows.single().remaining)
    }

    @Test
    fun replay_rebuildsQuotaSnapshotFromStoredPayload() {
        val provider = CodexCodingPlanProvider(apiService = FakeCodexApiService(sampleResponse()))
        val payload = ProviderReplayPayload(
            fetchedAt = 123_456L,
            payloadFormat = CodexCodingPlanProvider.RAW_PAYLOAD_FORMAT,
            rawPayloadJson = Json.encodeToString(CodexUsageResponse.serializer(), sampleResponse()),
            normalizerVersion = CodexCodingPlanProvider.NORMALIZER_VERSION
        )

        val snapshot = provider.replay(payload).getOrThrow().snapshot

        assertEquals(123_456L, snapshot.fetchedAt)
        assertEquals(2, snapshot.resources.size)
        assertEquals("OpenAI Codex Pro", snapshot.resources.first().title)
        assertEquals("Other bucket", snapshot.resources.last().title)
        assertEquals(58L, snapshot.resources.first().windows.first().remaining)
        assertEquals(40L, snapshot.resources.first().windows.last().remaining)
        assertEquals(WindowScope.Rolling, snapshot.resources.last().windows.single().scope)
    }

    @Test
    fun requiresReplay_onlyWhenStoredNormalizerVersionIsOlder() {
        val provider = CodexCodingPlanProvider(apiService = FakeCodexApiService(sampleResponse()))
        val currentPayload = ProviderReplayPayload(
            fetchedAt = 1L,
            payloadFormat = CodexCodingPlanProvider.RAW_PAYLOAD_FORMAT,
            rawPayloadJson = Json.encodeToString(CodexUsageResponse.serializer(), sampleResponse()),
            normalizerVersion = CodexCodingPlanProvider.NORMALIZER_VERSION
        )
        val outdatedPayload = currentPayload.copy(normalizerVersion = 0)

        assertFalse(provider.requiresReplay(currentPayload))
        assertTrue(provider.requiresReplay(outdatedPayload))
    }

    private class FakeCodexApiService(
        private val response: CodexUsageResponse
    ) : CodexApiService {
        var authorization: String? = null
        var accountId: String? = null

        override suspend fun getUsage(
            authorization: String,
            accountId: String?
        ): CodexUsageResponse {
            this.authorization = authorization
            this.accountId = accountId
            return response
        }
    }
}

fun sampleResponse(): CodexUsageResponse {
    return CodexUsageResponse(
        planType = "pro",
        rateLimit = CodexRateLimitStatus(
            primaryWindow = CodexRateLimitWindow(
                usedPercent = 42.0,
                limitWindowSeconds = 86_400,
                resetAfterSeconds = 120,
                resetAt = 2_000_000
            ),
            secondaryWindow = CodexRateLimitWindow(
                usedPercent = 60.0,
                limitWindowSeconds = 604_800,
                resetAfterSeconds = 86_400,
                resetAt = 2_600_000
            )
        ),
        additionalRateLimits = listOf(
            CodexAdditionalRateLimit(
                limitName = "Other bucket",
                meteredFeature = "feature-a",
                rateLimit = CodexRateLimitStatus(
                    primaryWindow = CodexRateLimitWindow(
                        usedPercent = 88.0,
                        limitWindowSeconds = 18_000,
                        resetAfterSeconds = 600,
                        resetAt = 1_900_000
                    )
                )
            )
        )
    )
}
