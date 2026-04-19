package com.lurenjia534.quotahub.data.provider.zhipu

import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.provider.CapturedQuotaSnapshot
import com.lurenjia534.quotahub.data.provider.CodingPlanProvider
import com.lurenjia534.quotahub.data.provider.CredentialFieldSpec
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor
import com.lurenjia534.quotahub.data.provider.ProviderReplayPayload
import com.lurenjia534.quotahub.data.provider.ProviderReplaySupport
import com.lurenjia534.quotahub.data.provider.SecretBundle
import kotlinx.serialization.json.Json
import java.net.URL
import java.time.Clock
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class ZhipuCodingPlanProvider(
    private val apiServiceFactory: (String) -> ZhipuApiService = ZhipuApiClient::createService,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val json: Json = JSON
) : CodingPlanProvider {
    override val descriptor: ProviderDescriptor = ProviderDescriptor(
        id = ID,
        displayName = "Zhipu BigModel",
        credentialFields = listOf(
            CredentialFieldSpec(
                key = AUTH_TOKEN_FIELD,
                label = "Authorization Token"
            ),
            CredentialFieldSpec(
                key = BASE_URL_FIELD,
                label = "Base URL (optional)",
                isSecret = false,
                isRequired = false
            )
        )
    )

    override val replaySupport: ProviderReplaySupport = ProviderReplaySupport(
        payloadFormat = RAW_PAYLOAD_FORMAT,
        normalizerVersion = NORMALIZER_VERSION
    )

    override suspend fun validate(credentials: SecretBundle): Result<CapturedQuotaSnapshot> {
        return runCatching {
            val usageWindow = usageWindow(clock)
            val service = apiServiceFactory(resolveMonitorBaseUrl(credentials))
            val authorization = credentials.requireValue(AUTH_TOKEN_FIELD)
            val fetchedAt = clock.millis()
            val bundle = ZhipuUsageBundle(
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
            CapturedQuotaSnapshot(
                snapshot = bundle.toQuotaSnapshot(fetchedAt = fetchedAt),
                replayPayload = ProviderReplayPayload(
                    fetchedAt = fetchedAt,
                    payloadFormat = RAW_PAYLOAD_FORMAT,
                    rawPayloadJson = json.encodeToString(ZhipuUsageBundle.serializer(), bundle),
                    normalizerVersion = NORMALIZER_VERSION
                )
            )
        }
    }

    override suspend fun fetchSnapshot(
        subscription: Subscription,
        credentials: SecretBundle
    ): Result<CapturedQuotaSnapshot> {
        return validate(credentials)
    }

    override fun replay(payload: ProviderReplayPayload): Result<CapturedQuotaSnapshot> {
        return runCatching {
            require(payload.payloadFormat == RAW_PAYLOAD_FORMAT) {
                "Unsupported replay payload format: ${payload.payloadFormat}"
            }
            val bundle = json.decodeFromString(
                ZhipuUsageBundle.serializer(),
                payload.rawPayloadJson
            )
            CapturedQuotaSnapshot(
                snapshot = bundle.toQuotaSnapshot(fetchedAt = payload.fetchedAt),
                replayPayload = payload.copy(
                    payloadFormat = RAW_PAYLOAD_FORMAT,
                    normalizerVersion = NORMALIZER_VERSION
                )
            )
        }
    }

    private fun resolveMonitorBaseUrl(credentials: SecretBundle): String {
        val rawBaseUrl = credentials.value(BASE_URL_FIELD) ?: DEFAULT_BASE_URL
        val parsed = URL(rawBaseUrl)
        val portSuffix = if (parsed.port == -1 || parsed.port == parsed.defaultPort) {
            ""
        } else {
            ":${parsed.port}"
        }
        return "${parsed.protocol}://${parsed.host}$portSuffix/"
    }

    companion object {
        const val ID = "zhipu"
        const val AUTH_TOKEN_FIELD = "authToken"
        const val BASE_URL_FIELD = "baseUrl"
        const val RAW_PAYLOAD_FORMAT = "zhipu.monitor-usage-bundle.v1"
        const val NORMALIZER_VERSION = 1
        const val DEFAULT_BASE_URL = "https://open.bigmodel.cn/api/anthropic"

        private val JSON = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        private val QUERY_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        private fun usageWindow(clock: Clock): ZhipuUsageWindow {
            val currentHour = ZonedDateTime.now(clock).truncatedTo(ChronoUnit.HOURS)
            return ZhipuUsageWindow(
                startTime = currentHour.minusDays(1).format(QUERY_FORMATTER),
                endTime = currentHour.plusHours(1).minusSeconds(1).format(QUERY_FORMATTER)
            )
        }
    }
}

private data class ZhipuUsageWindow(
    val startTime: String,
    val endTime: String
)
