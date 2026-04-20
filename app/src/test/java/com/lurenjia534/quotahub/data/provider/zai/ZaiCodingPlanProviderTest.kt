package com.lurenjia534.quotahub.data.provider.zai

import com.lurenjia534.quotahub.data.model.ResourceType
import com.lurenjia534.quotahub.data.model.WindowScope
import com.lurenjia534.quotahub.data.provider.ProviderFailure
import com.lurenjia534.quotahub.data.provider.ProviderReplayPayload
import com.lurenjia534.quotahub.data.provider.ProviderSyncException
import com.lurenjia534.quotahub.data.provider.SecretBundle
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class ZaiCodingPlanProviderTest {
    @Test
    fun validate_requestsAllUsageEndpointsWithNormalizedBaseUrl() = runBlocking {
        val apiService = FakeZaiApiService(
            modelUsage = sampleModelUsageResponse(),
            toolUsage = sampleToolUsageResponse(),
            quotaLimit = sampleQuotaLimitResponse()
        )
        var capturedBaseUrl: String? = null
        val provider = ZaiCodingPlanProvider(
            apiServiceFactory = { baseUrl ->
                capturedBaseUrl = baseUrl
                apiService
            },
            clock = Clock.fixed(
                Instant.parse("2026-04-19T06:23:10Z"),
                ZoneId.of("Asia/Shanghai")
            )
        )

        val capturedSnapshot = provider.validate(
            SecretBundle.of(
                mapOf(
                    ZaiCodingPlanProvider.AUTH_TOKEN_FIELD to "token-123",
                    ZaiCodingPlanProvider.BASE_URL_FIELD to "https://api.z.ai/api/anthropic"
                )
            )
        ).getOrThrow()

        assertEquals("https://api.z.ai/", capturedBaseUrl)
        assertEquals("token-123", apiService.authorization)
        assertEquals("2026-04-18 14:00:00", apiService.startTime)
        assertEquals("2026-04-19 14:59:59", apiService.endTime)
        assertEquals(ZaiCodingPlanProvider.RAW_PAYLOAD_FORMAT, capturedSnapshot.replayPayload?.payloadFormat)

        val limitResource = capturedSnapshot.snapshot.resources.first { it.key == "limit:time_limit" }
        assertEquals(ResourceType.Plan, limitResource.type)
        assertEquals("MCP usage (1 month)", limitResource.title)
        assertEquals(WindowScope.Monthly, limitResource.windows.single().scope)
        assertEquals(14L, limitResource.windows.single().used)
        assertEquals(86L, limitResource.windows.single().remaining)

        val modelResource = capturedSnapshot.snapshot.resources.first { it.key == "model:GLM-4.7" }
        assertEquals(ResourceType.Model, modelResource.type)
        assertEquals(5_335_805L, modelResource.windows.single().used)
    }

    @Test
    fun validate_usesDefaultZaiBaseUrlWhenCredentialOmitsBaseUrl() = runBlocking {
        var capturedBaseUrl: String? = null
        val provider = ZaiCodingPlanProvider(
            apiServiceFactory = { baseUrl ->
                capturedBaseUrl = baseUrl
                FakeZaiApiService(
                    modelUsage = sampleModelUsageResponse(),
                    toolUsage = sampleToolUsageResponse(),
                    quotaLimit = sampleQuotaLimitResponse()
                )
            }
        )

        provider.validate(
            SecretBundle.of(
                mapOf(
                    ZaiCodingPlanProvider.AUTH_TOKEN_FIELD to "token-123"
                )
            )
        ).getOrThrow()

        assertEquals("https://api.z.ai/", capturedBaseUrl)
    }

    @Test
    fun validate_failsWhenEndpointReturnsNullData() = runBlocking {
        val provider = ZaiCodingPlanProvider(
            apiServiceFactory = {
                FakeZaiApiService(
                    modelUsage = ZaiApiEnvelope(
                        code = 200,
                        msg = "Operation successful",
                        success = true,
                        data = null
                    ),
                    toolUsage = sampleToolUsageResponse(),
                    quotaLimit = sampleQuotaLimitResponse()
                )
            }
        )

        val result = provider.validate(
            SecretBundle.of(
                mapOf(
                    ZaiCodingPlanProvider.AUTH_TOKEN_FIELD to "token-123"
                )
            )
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ProviderSyncException)
        assertTrue((result.exceptionOrNull() as ProviderSyncException).failure is ProviderFailure.SchemaChanged)
        assertEquals("Missing data in model usage response", result.exceptionOrNull()?.message)
    }

    @Test
    fun validate_failsWhenEndpointReturnsNonSuccessCode() = runBlocking {
        val provider = ZaiCodingPlanProvider(
            apiServiceFactory = {
                FakeZaiApiService(
                    modelUsage = sampleModelUsageResponse(),
                    toolUsage = sampleToolUsageResponse(),
                    quotaLimit = ZaiApiEnvelope(
                        code = 500,
                        msg = "server exploded",
                        success = false,
                        data = null
                    )
                )
            }
        )

        val result = provider.validate(
            SecretBundle.of(
                mapOf(
                    ZaiCodingPlanProvider.AUTH_TOKEN_FIELD to "token-123"
                )
            )
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ProviderSyncException)
        assertTrue((result.exceptionOrNull() as ProviderSyncException).failure is ProviderFailure.Transient)
        assertEquals("server exploded", result.exceptionOrNull()?.message)
    }

    @Test
    fun replay_rebuildsQuotaSnapshotFromStoredPayload() {
        val provider = ZaiCodingPlanProvider(
            apiServiceFactory = { error("No network call expected during replay") }
        )
        val payload = ProviderReplayPayload(
            fetchedAt = 123_456L,
            payloadFormat = ZaiCodingPlanProvider.RAW_PAYLOAD_FORMAT,
            rawPayloadJson = Json.encodeToString(
                ZaiUsageBundle.serializer(),
                sampleUsageBundle()
            ),
            normalizerVersion = ZaiCodingPlanProvider.NORMALIZER_VERSION
        )

        val snapshot = provider.replay(payload).getOrThrow().snapshot

        assertEquals(123_456L, snapshot.fetchedAt)
        assertEquals("Token usage (5 hours)", snapshot.resources.first { it.key == "limit:tokens_limit" }.title)
        assertEquals(8L, snapshot.resources.first { it.key == "tool-limit:web-reader" }.windows.single().used)
    }

    @Test
    fun requiresReplay_onlyWhenStoredNormalizerVersionIsOlder() {
        val provider = ZaiCodingPlanProvider(
            apiServiceFactory = { error("No network call expected") }
        )
        val currentPayload = ProviderReplayPayload(
            fetchedAt = 1L,
            payloadFormat = ZaiCodingPlanProvider.RAW_PAYLOAD_FORMAT,
            rawPayloadJson = Json.encodeToString(
                ZaiUsageBundle.serializer(),
                sampleUsageBundle()
            ),
            normalizerVersion = ZaiCodingPlanProvider.NORMALIZER_VERSION
        )
        val outdatedPayload = currentPayload.copy(normalizerVersion = 0)

        assertFalse(provider.requiresReplay(currentPayload))
        assertTrue(provider.requiresReplay(outdatedPayload))
    }

    private class FakeZaiApiService(
        private val modelUsage: ZaiApiEnvelope<ZaiModelUsageData>,
        private val toolUsage: ZaiApiEnvelope<ZaiToolUsageData>,
        private val quotaLimit: ZaiApiEnvelope<ZaiQuotaLimitData>
    ) : ZaiApiService {
        var authorization: String? = null
        var startTime: String? = null
        var endTime: String? = null

        override suspend fun getModelUsage(
            authorization: String,
            startTime: String,
            endTime: String
        ): ZaiApiEnvelope<ZaiModelUsageData> {
            this.authorization = authorization
            this.startTime = startTime
            this.endTime = endTime
            return modelUsage
        }

        override suspend fun getToolUsage(
            authorization: String,
            startTime: String,
            endTime: String
        ): ZaiApiEnvelope<ZaiToolUsageData> {
            this.authorization = authorization
            this.startTime = startTime
            this.endTime = endTime
            return toolUsage
        }

        override suspend fun getQuotaLimit(
            authorization: String
        ): ZaiApiEnvelope<ZaiQuotaLimitData> {
            this.authorization = authorization
            return quotaLimit
        }
    }
}

fun sampleUsageBundle(): ZaiUsageBundle {
    return ZaiUsageBundle(
        modelUsage = sampleModelUsageResponse(),
        toolUsage = sampleToolUsageResponse(),
        quotaLimit = sampleQuotaLimitResponse()
    )
}

fun sampleModelUsageResponse(): ZaiApiEnvelope<ZaiModelUsageData> {
    return ZaiApiEnvelope(
        code = 200,
        msg = "Operation successful",
        success = true,
        data = ZaiModelUsageData(
            xTime = listOf(
                "2026-04-18 14:00",
                "2026-04-19 14:00"
            ),
            totalUsage = ZaiModelTotalUsage(
                totalModelCallCount = 149L,
                totalTokensUsage = 4_214_935L,
                modelSummaryList = listOf(
                    ZaiModelSummary(
                        modelName = "GLM-4.7",
                        totalTokens = 5_335_805L,
                        sortOrder = 1
                    )
                )
            ),
            modelSummaryList = listOf(
                ZaiModelSummary(
                    modelName = "GLM-4.7",
                    totalTokens = 5_335_805L,
                    sortOrder = 1
                )
            )
        )
    )
}

fun sampleToolUsageResponse(): ZaiApiEnvelope<ZaiToolUsageData> {
    return ZaiApiEnvelope(
        code = 200,
        msg = "Operation successful",
        success = true,
        data = ZaiToolUsageData(
            xTime = listOf(
                "2026-04-18 14:00",
                "2026-04-19 14:00"
            ),
            totalUsage = ZaiToolTotalUsage(
                totalNetworkSearchCount = 3L,
                totalWebReadMcpCount = 5L,
                totalZreadMcpCount = 0L
            )
        )
    )
}

fun sampleToolUsageResponseWithoutTotals(): ZaiApiEnvelope<ZaiToolUsageData> {
    return ZaiApiEnvelope(
        code = 200,
        msg = "Operation successful",
        success = true,
        data = ZaiToolUsageData(
            xTime = listOf(
                "2026-04-18 14:00",
                "2026-04-19 14:00"
            ),
            totalUsage = ZaiToolTotalUsage(),
            toolSummaryList = listOf(
                ZaiToolSummary(
                    modelCode = "search-prime",
                    usage = 6L
                ),
                ZaiToolSummary(
                    modelCode = "web-reader",
                    usage = 8L
                )
            )
        )
    )
}

fun sampleQuotaLimitResponse(): ZaiApiEnvelope<ZaiQuotaLimitData> {
    return ZaiApiEnvelope(
        code = 200,
        msg = "Operation successful",
        success = true,
        data = ZaiQuotaLimitData(
            level = "lite",
            limits = listOf(
                ZaiQuotaLimit(
                    type = "TIME_LIMIT",
                    unit = 5,
                    number = 1,
                    usage = 100L,
                    currentValue = 14L,
                    remaining = 86L,
                    percentage = 14,
                    nextResetTime = 1_776_836_140_998L,
                    usageDetails = listOf(
                        ZaiQuotaUsageDetail(
                            modelCode = "search-prime",
                            usage = 6L
                        ),
                        ZaiQuotaUsageDetail(
                            modelCode = "web-reader",
                            usage = 8L
                        )
                    )
                ),
                ZaiQuotaLimit(
                    type = "TOKENS_LIMIT",
                    unit = 3,
                    number = 5,
                    percentage = 5,
                    nextResetTime = 1_776_610_539_070L
                )
            )
        )
    )
}

fun sampleQuotaLimitResponseWithoutUsageDetails(): ZaiApiEnvelope<ZaiQuotaLimitData> {
    return ZaiApiEnvelope(
        code = 200,
        msg = "Operation successful",
        success = true,
        data = ZaiQuotaLimitData(
            level = "lite",
            limits = listOf(
                ZaiQuotaLimit(
                    type = "TIME_LIMIT",
                    unit = 5,
                    number = 1,
                    usage = 100L,
                    currentValue = 14L,
                    remaining = 86L,
                    percentage = 14,
                    nextResetTime = 1_776_836_140_998L,
                    usageDetails = emptyList()
                ),
                ZaiQuotaLimit(
                    type = "TOKENS_LIMIT",
                    unit = 3,
                    number = 5,
                    percentage = 5,
                    nextResetTime = 1_776_610_539_070L
                )
            )
        )
    )
}
