package com.lurenjia534.quotahub.data.provider.zai

import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.provider.CapturedQuotaSnapshot
import com.lurenjia534.quotahub.data.provider.CodingPlanProvider
import com.lurenjia534.quotahub.data.provider.monitor.MonitorQuotaBrand
import com.lurenjia534.quotahub.data.provider.monitor.MonitorQuotaProviderFamily
import kotlinx.serialization.json.Json
import java.time.Clock

class ZaiCodingPlanProvider(
    private val apiServiceFactory: (String) -> ZaiApiService = ZaiApiClient::createService,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val json: Json = JSON
) : CodingPlanProvider by MonitorQuotaProviderFamily(
    brand = BRAND,
    bundleSerializer = ZaiUsageBundle.serializer(),
    bundleFetcher = { baseUrl, authorization, usageWindow ->
        val service = apiServiceFactory(baseUrl)
        ZaiUsageBundle(
            modelUsage = service.getModelUsage(
                authorization = authorization,
                startTime = usageWindow.startTime,
                endTime = usageWindow.endTime
            ),
            toolUsage = service.getToolUsage(
                authorization = authorization,
                startTime = usageWindow.startTime,
                endTime = usageWindow.endTime
            ),
            quotaLimit = service.getQuotaLimit(authorization = authorization)
        )
    },
    snapshotFactory = { bundle, fetchedAt ->
        bundle.toQuotaSnapshot(fetchedAt = fetchedAt)
    },
    clock = clock,
    json = json
) {

    companion object {
        const val ID = "zai"
        const val AUTH_TOKEN_FIELD = MonitorQuotaProviderFamily.AUTH_TOKEN_FIELD
        const val BASE_URL_FIELD = MonitorQuotaProviderFamily.BASE_URL_FIELD
        const val RAW_PAYLOAD_FORMAT = "zai.monitor-usage-bundle.v1"
        const val NORMALIZER_VERSION = 1
        const val DEFAULT_BASE_URL = "https://api.z.ai/api/anthropic"

        private val BRAND = MonitorQuotaBrand(
            id = ID,
            displayName = "Z.ai",
            defaultBaseUrl = DEFAULT_BASE_URL,
            rawPayloadFormat = RAW_PAYLOAD_FORMAT,
            normalizerVersion = NORMALIZER_VERSION
        )

        private val JSON = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }
}
