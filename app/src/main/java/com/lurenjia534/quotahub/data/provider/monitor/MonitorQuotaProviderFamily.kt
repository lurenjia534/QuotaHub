package com.lurenjia534.quotahub.data.provider.monitor

import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.provider.CapturedQuotaSnapshot
import com.lurenjia534.quotahub.data.provider.CodingPlanProvider
import com.lurenjia534.quotahub.data.provider.CredentialFieldSpec
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor
import com.lurenjia534.quotahub.data.provider.ProviderReplayPayload
import com.lurenjia534.quotahub.data.provider.ProviderReplaySupport
import com.lurenjia534.quotahub.data.provider.SecretBundle
import java.net.URL
import java.time.Clock
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

data class MonitorQuotaBrand(
    val id: String,
    val displayName: String,
    val defaultBaseUrl: String,
    val rawPayloadFormat: String,
    val normalizerVersion: Int
)

data class MonitorQuotaQueryWindow(
    val startTime: String,
    val endTime: String
)

class MonitorQuotaProviderFamily<TBundle>(
    private val brand: MonitorQuotaBrand,
    private val bundleSerializer: KSerializer<TBundle>,
    private val bundleFetcher: suspend (
        baseUrl: String,
        authorization: String,
        usageWindow: MonitorQuotaQueryWindow
    ) -> TBundle,
    private val snapshotFactory: (bundle: TBundle, fetchedAt: Long) -> com.lurenjia534.quotahub.data.model.QuotaSnapshot,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val json: Json = DEFAULT_JSON
) : CodingPlanProvider {
    override val descriptor: ProviderDescriptor = ProviderDescriptor(
        id = brand.id,
        displayName = brand.displayName,
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
        payloadFormat = brand.rawPayloadFormat,
        normalizerVersion = brand.normalizerVersion
    )

    override suspend fun validate(credentials: SecretBundle): Result<CapturedQuotaSnapshot> {
        return runCatching {
            val authorization = credentials.requireValue(AUTH_TOKEN_FIELD)
            val fetchedAt = clock.millis()
            val bundle = bundleFetcher(
                resolveMonitorBaseUrl(credentials),
                authorization,
                usageWindow(clock)
            )
            CapturedQuotaSnapshot(
                snapshot = snapshotFactory(bundle, fetchedAt),
                replayPayload = ProviderReplayPayload(
                    fetchedAt = fetchedAt,
                    payloadFormat = brand.rawPayloadFormat,
                    rawPayloadJson = json.encodeToString(bundleSerializer, bundle),
                    normalizerVersion = brand.normalizerVersion
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
            require(payload.payloadFormat == brand.rawPayloadFormat) {
                "Unsupported replay payload format: ${payload.payloadFormat}"
            }
            val bundle = json.decodeFromString(bundleSerializer, payload.rawPayloadJson)
            CapturedQuotaSnapshot(
                snapshot = snapshotFactory(bundle, payload.fetchedAt),
                replayPayload = payload.copy(
                    payloadFormat = brand.rawPayloadFormat,
                    normalizerVersion = brand.normalizerVersion
                )
            )
        }
    }

    private fun resolveMonitorBaseUrl(credentials: SecretBundle): String {
        val rawBaseUrl = credentials.value(BASE_URL_FIELD) ?: brand.defaultBaseUrl
        val parsed = URL(rawBaseUrl)
        val portSuffix = if (parsed.port == -1 || parsed.port == parsed.defaultPort) {
            ""
        } else {
            ":${parsed.port}"
        }
        return "${parsed.protocol}://${parsed.host}$portSuffix/"
    }

    companion object {
        const val AUTH_TOKEN_FIELD = "authToken"
        const val BASE_URL_FIELD = "baseUrl"

        private val DEFAULT_JSON = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        private val QUERY_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        private fun usageWindow(clock: Clock): MonitorQuotaQueryWindow {
            val currentHour = ZonedDateTime.now(clock).truncatedTo(ChronoUnit.HOURS)
            return MonitorQuotaQueryWindow(
                startTime = currentHour.minusDays(1).format(QUERY_FORMATTER),
                endTime = currentHour.plusHours(1).minusSeconds(1).format(QUERY_FORMATTER)
            )
        }
    }
}
