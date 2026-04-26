package com.lurenjia534.quotahub.data.provider.kimi

import com.lurenjia534.quotahub.data.model.ResourceType
import com.lurenjia534.quotahub.data.model.WindowScope
import com.lurenjia534.quotahub.data.provider.ProviderFailure
import com.lurenjia534.quotahub.data.provider.ProviderReplayPayload
import com.lurenjia534.quotahub.data.provider.ProviderSyncException
import com.lurenjia534.quotahub.data.provider.SecretBundle
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class KimiCodingPlanProviderTest {
    @Test
    fun validate_fetchesCodingUsageAndNormalizesQuotaWindows() = runBlocking {
        val service = FakeKimiApiService(response = sampleKimiUsageResponse())

        val capturedSnapshot = KimiCodingPlanProvider(apiService = service).validate(
            SecretBundle.single(KimiCodingPlanProvider.API_KEY_FIELD, "sk-test")
        ).getOrThrow()

        assertEquals("Bearer sk-test", service.authorization)
        assertEquals(KimiCodingPlanProvider.RAW_PAYLOAD_FORMAT, capturedSnapshot.replayPayload?.payloadFormat)

        val planResource = capturedSnapshot.snapshot.resources.first { it.key == PLAN_RESOURCE_KEY }
        assertEquals(ResourceType.Plan, planResource.type)
        assertEquals("Total plan quota", planResource.title)
        assertEquals(1, planResource.windows.size)

        val planWindow = planResource.windows.first { it.windowKey == "total" }
        assertEquals(WindowScope.Rolling, planWindow.scope)
        assertEquals(100L, planWindow.total)
        assertEquals(75L, planWindow.remaining)
        assertEquals(25L, planWindow.used)
        assertEquals(1_777_806_228_011L, planWindow.resetAtEpochMillis)

        val fiveHourResource = capturedSnapshot.snapshot.resources.first {
            it.key == "${LIMIT_RESOURCE_KEY_PREFIX}0"
        }
        assertEquals("5 hours window", fiveHourResource.title)
        val fiveHourWindow = fiveHourResource.windows.single()
        assertEquals(WindowScope.Rolling, fiveHourWindow.scope)
        assertEquals("5 hours", fiveHourWindow.label)
        assertEquals(1_777_219_428_011L, fiveHourWindow.resetAtEpochMillis)
        assertEquals(1_777_201_428_011L, fiveHourWindow.startsAt)

        val monthResource = capturedSnapshot.snapshot.resources.first {
            it.key == "${LIMIT_RESOURCE_KEY_PREFIX}1"
        }
        assertEquals("1 month window", monthResource.title)
        assertEquals(WindowScope.Monthly, monthResource.windows.single().scope)
        assertEquals("1 month", monthResource.windows.single().label)

        val parallelResource = capturedSnapshot.snapshot.resources.first { it.key == PARALLEL_RESOURCE_KEY }
        assertEquals(ResourceType.Feature, parallelResource.type)
        assertEquals(20L, parallelResource.windows.single().total)
    }

    @Test
    fun validate_preservesBearerPrefixWhenUserPastesFullHeaderValue() = runBlocking {
        val service = FakeKimiApiService(response = sampleKimiUsageResponse())

        KimiCodingPlanProvider(apiService = service).validate(
            SecretBundle.single(KimiCodingPlanProvider.API_KEY_FIELD, "Bearer sk-test")
        ).getOrThrow()

        assertEquals("Bearer sk-test", service.authorization)
    }

    @Test
    fun replay_rebuildsQuotaSnapshotFromStoredPayload() {
        val provider = KimiCodingPlanProvider(
            apiService = FakeKimiApiService(throwable = IllegalStateException("No network call expected"))
        )
        val payload = ProviderReplayPayload(
            fetchedAt = 123_456L,
            payloadFormat = KimiCodingPlanProvider.RAW_PAYLOAD_FORMAT,
            rawPayloadJson = Json.encodeToString(
                KimiUsageResponse.serializer(),
                sampleKimiUsageResponse()
            ),
            normalizerVersion = KimiCodingPlanProvider.NORMALIZER_VERSION
        )

        val snapshot = provider.replay(payload).getOrThrow().snapshot

        assertEquals(123_456L, snapshot.fetchedAt)
        assertEquals(4, snapshot.resources.size)
        assertEquals(75L, snapshot.resources.first { it.key == PLAN_RESOURCE_KEY }.kimiPlanWindow?.remaining)
    }

    @Test
    fun validate_wrapsUnauthorizedHttpExceptionAsAuthFailure() = runBlocking {
        val result = KimiCodingPlanProvider(
            apiService = FakeKimiApiService(throwable = httpException(401))
        ).validate(
            SecretBundle.single(KimiCodingPlanProvider.API_KEY_FIELD, "sk-test")
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ProviderSyncException)
        assertTrue((result.exceptionOrNull() as ProviderSyncException).failure is ProviderFailure.Auth)
    }

    @Test
    fun replay_wrapsMalformedJsonAsSchemaChangedFailure() {
        val provider = KimiCodingPlanProvider(
            apiService = FakeKimiApiService(throwable = IllegalStateException("No network call expected"))
        )
        val payload = ProviderReplayPayload(
            fetchedAt = 123_456L,
            payloadFormat = KimiCodingPlanProvider.RAW_PAYLOAD_FORMAT,
            rawPayloadJson = """{"usage":{"limit":"100"}""",
            normalizerVersion = KimiCodingPlanProvider.NORMALIZER_VERSION
        )

        val result = provider.replay(payload)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ProviderSyncException)
        assertTrue((result.exceptionOrNull() as ProviderSyncException).failure is ProviderFailure.SchemaChanged)
    }

    @Test
    fun requiresReplay_onlyWhenStoredNormalizerVersionIsOlder() {
        val provider = KimiCodingPlanProvider(
            apiService = FakeKimiApiService(throwable = IllegalStateException("No network call expected"))
        )
        val currentPayload = ProviderReplayPayload(
            fetchedAt = 1L,
            payloadFormat = KimiCodingPlanProvider.RAW_PAYLOAD_FORMAT,
            rawPayloadJson = Json.encodeToString(
                KimiUsageResponse.serializer(),
                sampleKimiUsageResponse()
            ),
            normalizerVersion = KimiCodingPlanProvider.NORMALIZER_VERSION
        )
        val outdatedPayload = currentPayload.copy(normalizerVersion = 0)

        assertFalse(provider.requiresReplay(currentPayload))
        assertTrue(provider.requiresReplay(outdatedPayload))
    }

    private class FakeKimiApiService(
        private val response: KimiUsageResponse? = null,
        private val throwable: Throwable? = null
    ) : KimiApiService {
        var authorization: String? = null

        override suspend fun getCodingUsage(authorization: String): KimiUsageResponse {
            this.authorization = authorization
            throwable?.let { throw it }
            return response ?: error("Expected either a response or an error")
        }
    }

    private fun httpException(code: Int): HttpException {
        val body = "{}".toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(code, body))
    }
}

fun sampleKimiUsageResponse(): KimiUsageResponse {
    return KimiUsageResponse(
        user = KimiUser(
            userId = "user-123",
            region = "REGION_CN",
            membership = KimiMembership(level = "LEVEL_INTERMEDIATE")
        ),
        usage = KimiUsageSummary(
            limit = "100",
            remaining = "70",
            resetTime = "2026-05-03T11:03:48.011825Z"
        ),
        limits = listOf(
            KimiUsageLimit(
                window = KimiUsageWindow(
                    duration = 300,
                    timeUnit = "TIME_UNIT_MINUTE"
                ),
                detail = KimiUsageSummary(
                    limit = "100",
                    remaining = "80",
                    resetTime = "2026-04-26T16:03:48.011825Z"
                )
            ),
            KimiUsageLimit(
                window = KimiUsageWindow(
                    duration = 1,
                    timeUnit = "TIME_UNIT_MONTH"
                ),
                detail = KimiUsageSummary(
                    limit = "100",
                    remaining = "75",
                    resetTime = "2026-05-03T11:03:48.011825Z"
                )
            )
        ),
        parallel = KimiParallelLimit(limit = "20"),
        totalQuota = KimiQuotaSummary(
            limit = "100\n",
            remaining = "75"
        ),
        authentication = KimiAuthentication(
            method = "METHOD_API_KEY",
            scope = "FEATURE_CODING"
        ),
        subType = "TYPE_PURCHASE"
    )
}
