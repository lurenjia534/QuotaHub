package com.lurenjia534.quotahub.data.provider.minimax

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

class MiniMaxCodingPlanProviderTest {
    private val provider = MiniMaxCodingPlanProvider()

    @Test
    fun replay_rebuildsQuotaSnapshotFromStoredPayload() {
        val response = MiniMaxQuotaResponse(
            modelRemains = listOf(
                MiniMaxModelQuota(
                    startTime = 100L,
                    endTime = 200L,
                    remainsTime = 180L,
                    currentIntervalTotalCount = 100,
                    currentIntervalUsageCount = 40,
                    modelName = "MiniMax-Text-01",
                    currentWeeklyTotalCount = 300,
                    currentWeeklyUsageCount = 200,
                    weeklyStartTime = 50L,
                    weeklyEndTime = 500L,
                    weeklyRemainsTime = 450L
                )
            ),
            baseResp = MiniMaxBaseResponse(
                statusCode = 0,
                statusMsg = "ok"
            )
        )
        val payload = ProviderReplayPayload(
            fetchedAt = 123456789L,
            payloadFormat = MiniMaxCodingPlanProvider.RAW_PAYLOAD_FORMAT,
            rawPayloadJson = Json.encodeToString(MiniMaxQuotaResponse.serializer(), response),
            normalizerVersion = MiniMaxCodingPlanProvider.NORMALIZER_VERSION
        )

        val snapshot = provider.replay(payload).getOrThrow().snapshot

        assertEquals(123456789L, snapshot.fetchedAt)
        assertEquals(1, snapshot.resources.size)
        val resource = snapshot.resources.single()
        assertEquals("MiniMax-Text-01", resource.key)
        assertEquals(ResourceType.Model, resource.type)
        assertEquals(2, resource.windows.size)
        assertTrue(resource.windows.any { it.scope == WindowScope.Interval })
        assertTrue(resource.windows.any { it.scope == WindowScope.Weekly })
        val intervalWindow = resource.windows.first { it.scope == WindowScope.Interval }
        assertEquals("interval", intervalWindow.windowKey)
        assertEquals(100L, intervalWindow.total)
        assertEquals(60L, intervalWindow.used)
        assertEquals(40L, intervalWindow.remaining)
        assertEquals(200L, intervalWindow.resetAtEpochMillis)
    }

    @Test
    fun validate_wrapsAuthStatusCodeAsAuthFailure() = runBlocking {
        val service = FakeMiniMaxApiService(
            response = MiniMaxQuotaResponse(
                modelRemains = emptyList(),
                baseResp = MiniMaxBaseResponse(
                    statusCode = 1004,
                    statusMsg = "unauthorized"
                )
            )
        )

        val result = MiniMaxCodingPlanProvider(apiService = service).validate(
            SecretBundle.single(MiniMaxCodingPlanProvider.API_KEY_FIELD, "api-key")
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ProviderSyncException)
        assertTrue((result.exceptionOrNull() as ProviderSyncException).failure is ProviderFailure.Auth)
    }

    @Test
    fun validate_wrapsUnauthorizedHttpExceptionAsAuthFailure() = runBlocking {
        val service = FakeMiniMaxApiService(
            throwable = httpException(401)
        )

        val result = MiniMaxCodingPlanProvider(apiService = service).validate(
            SecretBundle.single(MiniMaxCodingPlanProvider.API_KEY_FIELD, "api-key")
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ProviderSyncException)
        assertTrue((result.exceptionOrNull() as ProviderSyncException).failure is ProviderFailure.Auth)
    }

    @Test
    fun validate_wrapsRateLimitedStatusCodeAsRateLimitedFailure() = runBlocking {
        val service = FakeMiniMaxApiService(
            response = MiniMaxQuotaResponse(
                modelRemains = emptyList(),
                baseResp = MiniMaxBaseResponse(
                    statusCode = 1002,
                    statusMsg = "request frequency exceeded"
                )
            )
        )

        val result = MiniMaxCodingPlanProvider(apiService = service).validate(
            SecretBundle.single(MiniMaxCodingPlanProvider.API_KEY_FIELD, "api-key")
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ProviderSyncException)
        assertTrue((result.exceptionOrNull() as ProviderSyncException).failure is ProviderFailure.RateLimited)
    }

    @Test
    fun validate_wrapsTransientStatusCodeAsTransientFailure() = runBlocking {
        val service = FakeMiniMaxApiService(
            response = MiniMaxQuotaResponse(
                modelRemains = emptyList(),
                baseResp = MiniMaxBaseResponse(
                    statusCode = 1024,
                    statusMsg = "internal error"
                )
            )
        )

        val result = MiniMaxCodingPlanProvider(apiService = service).validate(
            SecretBundle.single(MiniMaxCodingPlanProvider.API_KEY_FIELD, "api-key")
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ProviderSyncException)
        assertTrue((result.exceptionOrNull() as ProviderSyncException).failure is ProviderFailure.Transient)
    }

    @Test
    fun validate_wrapsValidationStatusCodeAsValidationFailure() = runBlocking {
        val service = FakeMiniMaxApiService(
            response = MiniMaxQuotaResponse(
                modelRemains = emptyList(),
                baseResp = MiniMaxBaseResponse(
                    statusCode = 1042,
                    statusMsg = "illegal characters"
                )
            )
        )

        val result = MiniMaxCodingPlanProvider(apiService = service).validate(
            SecretBundle.single(MiniMaxCodingPlanProvider.API_KEY_FIELD, "api-key")
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ProviderSyncException)
        assertTrue((result.exceptionOrNull() as ProviderSyncException).failure is ProviderFailure.Validation)
    }

    @Test
    fun replay_wrapsMalformedJsonAsSchemaChangedFailure() {
        val payload = ProviderReplayPayload(
            fetchedAt = 123456789L,
            payloadFormat = MiniMaxCodingPlanProvider.RAW_PAYLOAD_FORMAT,
            rawPayloadJson = """{"model_remains":[{"start_time":1}]""",
            normalizerVersion = MiniMaxCodingPlanProvider.NORMALIZER_VERSION
        )

        val result = provider.replay(payload)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ProviderSyncException)
        assertTrue((result.exceptionOrNull() as ProviderSyncException).failure is ProviderFailure.SchemaChanged)
    }

    @Test
    fun requiresReplay_onlyWhenStoredNormalizerVersionIsOlder() {
        val currentPayload = ProviderReplayPayload(
            fetchedAt = 1L,
            payloadFormat = MiniMaxCodingPlanProvider.RAW_PAYLOAD_FORMAT,
            rawPayloadJson = """{"model_remains":[],"base_resp":{"status_code":0,"status_msg":"ok"}}""",
            normalizerVersion = MiniMaxCodingPlanProvider.NORMALIZER_VERSION
        )
        val outdatedPayload = currentPayload.copy(normalizerVersion = 0)

        assertFalse(provider.requiresReplay(currentPayload))
        assertTrue(provider.requiresReplay(outdatedPayload))
    }

    private class FakeMiniMaxApiService(
        private val response: MiniMaxQuotaResponse? = null,
        private val throwable: Throwable? = null
    ) : MiniMaxApiService {
        override suspend fun getModelRemains(authorization: String): MiniMaxQuotaResponse {
            throwable?.let { throw it }
            return response ?: error("Expected either a response or an error")
        }
    }

    private fun httpException(code: Int): HttpException {
        val body = "{}".toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(code, body))
    }
}
