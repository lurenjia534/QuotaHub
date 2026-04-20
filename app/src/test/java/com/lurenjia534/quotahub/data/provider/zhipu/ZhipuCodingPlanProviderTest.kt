package com.lurenjia534.quotahub.data.provider.zhipu

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

class ZhipuCodingPlanProviderTest {
    @Test
    fun validate_requestsAllUsageEndpointsWithNormalizedBaseUrl() = runBlocking {
        val apiService = FakeZhipuApiService(
            modelUsage = sampleModelUsageResponse(),
            toolUsage = sampleToolUsageResponse(),
            quotaLimit = sampleQuotaLimitResponse()
        )
        var capturedBaseUrl: String? = null
        val provider = ZhipuCodingPlanProvider(
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
                    ZhipuCodingPlanProvider.AUTH_TOKEN_FIELD to "token-123",
                    ZhipuCodingPlanProvider.BASE_URL_FIELD to "https://open.bigmodel.cn/api/anthropic"
                )
            )
        ).getOrThrow()

        assertEquals("https://open.bigmodel.cn/", capturedBaseUrl)
        assertEquals("token-123", apiService.authorization)
        assertEquals("2026-04-18 14:00:00", apiService.startTime)
        assertEquals("2026-04-19 14:59:59", apiService.endTime)
        assertEquals(ZhipuCodingPlanProvider.RAW_PAYLOAD_FORMAT, capturedSnapshot.replayPayload?.payloadFormat)

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
    fun validate_usesDefaultBaseUrlWhenCredentialOmitsBaseUrl() = runBlocking {
        var capturedBaseUrl: String? = null
        val provider = ZhipuCodingPlanProvider(
            apiServiceFactory = { baseUrl ->
                capturedBaseUrl = baseUrl
                FakeZhipuApiService(
                    modelUsage = sampleModelUsageResponse(),
                    toolUsage = sampleToolUsageResponse(),
                    quotaLimit = sampleQuotaLimitResponse()
                )
            }
        )

        provider.validate(
            SecretBundle.of(
                mapOf(
                    ZhipuCodingPlanProvider.AUTH_TOKEN_FIELD to "token-123"
                )
            )
        ).getOrThrow()

        assertEquals("https://open.bigmodel.cn/", capturedBaseUrl)
    }

    @Test
    fun validate_failsWhenEndpointReturnsNullData() = runBlocking {
        val provider = ZhipuCodingPlanProvider(
            apiServiceFactory = {
                FakeZhipuApiService(
                    modelUsage = ZhipuApiEnvelope(
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
                    ZhipuCodingPlanProvider.AUTH_TOKEN_FIELD to "token-123"
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
        val provider = ZhipuCodingPlanProvider(
            apiServiceFactory = {
                FakeZhipuApiService(
                    modelUsage = sampleModelUsageResponse(),
                    toolUsage = sampleToolUsageResponse(),
                    quotaLimit = ZhipuApiEnvelope(
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
                    ZhipuCodingPlanProvider.AUTH_TOKEN_FIELD to "token-123"
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
        val provider = ZhipuCodingPlanProvider(
            apiServiceFactory = { error("No network call expected during replay") }
        )
        val payload = ProviderReplayPayload(
            fetchedAt = 123_456L,
            payloadFormat = ZhipuCodingPlanProvider.RAW_PAYLOAD_FORMAT,
            rawPayloadJson = Json.encodeToString(
                ZhipuUsageBundle.serializer(),
                sampleUsageBundle()
            ),
            normalizerVersion = ZhipuCodingPlanProvider.NORMALIZER_VERSION
        )

        val snapshot = provider.replay(payload).getOrThrow().snapshot

        assertEquals(123_456L, snapshot.fetchedAt)
        assertEquals(7, snapshot.resources.size)
        assertEquals(
            "Token usage (5 hours)",
            snapshot.resources.first { it.key == "limit:tokens_limit" }.title
        )
        assertEquals(
            8L,
            snapshot.resources.first { it.key == "tool-limit:web-reader" }.windows.single().used
        )
    }

    @Test
    fun requiresReplay_onlyWhenStoredNormalizerVersionIsOlder() {
        val provider = ZhipuCodingPlanProvider(
            apiServiceFactory = { error("No network call expected") }
        )
        val currentPayload = ProviderReplayPayload(
            fetchedAt = 1L,
            payloadFormat = ZhipuCodingPlanProvider.RAW_PAYLOAD_FORMAT,
            rawPayloadJson = Json.encodeToString(
                ZhipuUsageBundle.serializer(),
                sampleUsageBundle()
            ),
            normalizerVersion = ZhipuCodingPlanProvider.NORMALIZER_VERSION
        )
        val outdatedPayload = currentPayload.copy(normalizerVersion = 0)

        assertFalse(provider.requiresReplay(currentPayload))
        assertTrue(provider.requiresReplay(outdatedPayload))
    }

    private class FakeZhipuApiService(
        private val modelUsage: ZhipuApiEnvelope<ZhipuModelUsageData>,
        private val toolUsage: ZhipuApiEnvelope<ZhipuToolUsageData>,
        private val quotaLimit: ZhipuApiEnvelope<ZhipuQuotaLimitData>
    ) : ZhipuApiService {
        var authorization: String? = null
        var startTime: String? = null
        var endTime: String? = null

        override suspend fun getModelUsage(
            authorization: String,
            startTime: String,
            endTime: String
        ): ZhipuApiEnvelope<ZhipuModelUsageData> {
            this.authorization = authorization
            this.startTime = startTime
            this.endTime = endTime
            return modelUsage
        }

        override suspend fun getToolUsage(
            authorization: String,
            startTime: String,
            endTime: String
        ): ZhipuApiEnvelope<ZhipuToolUsageData> {
            this.authorization = authorization
            this.startTime = startTime
            this.endTime = endTime
            return toolUsage
        }

        override suspend fun getQuotaLimit(
            authorization: String
        ): ZhipuApiEnvelope<ZhipuQuotaLimitData> {
            this.authorization = authorization
            return quotaLimit
        }
    }
}

fun sampleUsageBundle(): ZhipuUsageBundle {
    return ZhipuUsageBundle(
        modelUsage = sampleModelUsageResponse(),
        toolUsage = sampleToolUsageResponse(),
        quotaLimit = sampleQuotaLimitResponse()
    )
}

fun sampleModelUsageResponse(): ZhipuApiEnvelope<ZhipuModelUsageData> {
    return ZhipuApiEnvelope(
        code = 200,
        msg = "Operation successful",
        success = true,
        data = ZhipuModelUsageData(
            xTime = listOf(
                "2026-04-18 14:00",
                "2026-04-19 14:00"
            ),
            totalUsage = ZhipuModelTotalUsage(
                totalModelCallCount = 149L,
                totalTokensUsage = 4_214_935L,
                modelSummaryList = listOf(
                    ZhipuModelSummary(
                        modelName = "GLM-4.7",
                        totalTokens = 5_335_805L,
                        sortOrder = 1
                    )
                )
            ),
            modelSummaryList = listOf(
                ZhipuModelSummary(
                    modelName = "GLM-4.7",
                    totalTokens = 5_335_805L,
                    sortOrder = 1
                )
            )
        )
    )
}

fun sampleToolUsageResponse(): ZhipuApiEnvelope<ZhipuToolUsageData> {
    return ZhipuApiEnvelope(
        code = 200,
        msg = "Operation successful",
        success = true,
        data = ZhipuToolUsageData(
            xTime = listOf(
                "2026-04-18 14:00",
                "2026-04-19 14:00"
            ),
            totalUsage = ZhipuToolTotalUsage(
                totalNetworkSearchCount = 3L,
                totalWebReadMcpCount = 5L,
                totalZreadMcpCount = 0L
            )
        )
    )
}

fun sampleToolUsageResponseWithoutTotals(): ZhipuApiEnvelope<ZhipuToolUsageData> {
    return ZhipuApiEnvelope(
        code = 200,
        msg = "Operation successful",
        success = true,
        data = ZhipuToolUsageData(
            xTime = listOf(
                "2026-04-18 14:00",
                "2026-04-19 14:00"
            ),
            totalUsage = ZhipuToolTotalUsage(),
            toolSummaryList = listOf(
                ZhipuToolSummary(
                    modelCode = "search-prime",
                    usage = 6L
                ),
                ZhipuToolSummary(
                    modelCode = "web-reader",
                    usage = 8L
                )
            )
        )
    )
}

fun sampleQuotaLimitResponse(): ZhipuApiEnvelope<ZhipuQuotaLimitData> {
    return ZhipuApiEnvelope(
        code = 200,
        msg = "Operation successful",
        success = true,
        data = ZhipuQuotaLimitData(
            level = "lite",
            limits = listOf(
                ZhipuQuotaLimit(
                    type = "TIME_LIMIT",
                    unit = 5,
                    number = 1,
                    usage = 100L,
                    currentValue = 14L,
                    remaining = 86L,
                    percentage = 14,
                    nextResetTime = 1_776_836_140_998L,
                    usageDetails = listOf(
                        ZhipuQuotaUsageDetail(
                            modelCode = "search-prime",
                            usage = 6L
                        ),
                        ZhipuQuotaUsageDetail(
                            modelCode = "web-reader",
                            usage = 8L
                        )
                    )
                ),
                ZhipuQuotaLimit(
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

fun sampleQuotaLimitResponseWithoutUsageDetails(): ZhipuApiEnvelope<ZhipuQuotaLimitData> {
    return ZhipuApiEnvelope(
        code = 200,
        msg = "Operation successful",
        success = true,
        data = ZhipuQuotaLimitData(
            level = "lite",
            limits = listOf(
                ZhipuQuotaLimit(
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
                ZhipuQuotaLimit(
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
