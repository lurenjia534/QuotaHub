package com.lurenjia534.quotahub.data.provider.minimax

import com.lurenjia534.quotahub.data.model.ResourceType
import com.lurenjia534.quotahub.data.model.WindowScope
import com.lurenjia534.quotahub.data.provider.ProviderReplayPayload
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
