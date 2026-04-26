package com.lurenjia534.quotahub.data.provider.kimi

import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.provider.CapturedQuotaSnapshot
import com.lurenjia534.quotahub.data.provider.CodingPlanProvider
import com.lurenjia534.quotahub.data.provider.CredentialFieldSpec
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor
import com.lurenjia534.quotahub.data.provider.ProviderFailure
import com.lurenjia534.quotahub.data.provider.ProviderReplayPayload
import com.lurenjia534.quotahub.data.provider.ProviderReplaySupport
import com.lurenjia534.quotahub.data.provider.ProviderSyncException
import com.lurenjia534.quotahub.data.provider.SecretBundle
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class KimiCodingPlanProvider(
    private val apiService: KimiApiService = KimiApiClient.apiService
) : CodingPlanProvider {

    override val descriptor: ProviderDescriptor = ProviderDescriptor(
        id = ID,
        displayName = "Kimi Coding Plan",
        credentialFields = listOf(
            CredentialFieldSpec(
                key = API_KEY_FIELD,
                label = "API Key"
            )
        )
    )

    override val replaySupport: ProviderReplaySupport = ProviderReplaySupport(
        currentPayloadFormat = RAW_PAYLOAD_FORMAT,
        normalizerVersion = NORMALIZER_VERSION
    )

    override suspend fun validate(credentials: SecretBundle): Result<CapturedQuotaSnapshot> {
        return captureFailures {
            val fetchedAt = System.currentTimeMillis()
            val response = apiService.getCodingUsage(authorization(credentials))
            CapturedQuotaSnapshot(
                snapshot = response.toQuotaSnapshot(fetchedAt),
                replayPayload = ProviderReplayPayload(
                    fetchedAt = fetchedAt,
                    payloadFormat = RAW_PAYLOAD_FORMAT,
                    rawPayloadJson = json.encodeToString(KimiUsageResponse.serializer(), response),
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
        return captureFailures {
            require(payload.payloadFormat == RAW_PAYLOAD_FORMAT) {
                "Unsupported replay payload format: ${payload.payloadFormat}"
            }
            val response = json.decodeFromString(
                KimiUsageResponse.serializer(),
                payload.rawPayloadJson
            )
            CapturedQuotaSnapshot(
                snapshot = response.toQuotaSnapshot(fetchedAt = payload.fetchedAt),
                replayPayload = payload.copy(
                    payloadFormat = RAW_PAYLOAD_FORMAT,
                    normalizerVersion = NORMALIZER_VERSION
                )
            )
        }
    }

    private inline fun <T> captureFailures(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (error: Throwable) {
            if (error is CancellationException) {
                throw error
            }
            Result.failure(providerSyncException(error))
        }
    }

    private fun providerSyncException(error: Throwable): ProviderSyncException {
        return if (error is ProviderSyncException) {
            error
        } else {
            ProviderSyncException(
                failure = kimiFailure(error),
                cause = error
            )
        }
    }

    private fun kimiFailure(error: Throwable): ProviderFailure {
        return when (error) {
            is HttpException -> error.toProviderFailure()
            is SerializationException -> ProviderFailure.SchemaChanged(
                userMessage = "Stored Kimi payload could not be parsed. Refresh to capture a new snapshot."
            )
            is IllegalStateException,
            is IllegalArgumentException -> {
                if (error.message?.startsWith("Unsupported replay payload format") == true) {
                    ProviderFailure.SchemaChanged(
                        userMessage = error.message ?: "Stored Kimi replay payload is no longer supported."
                    )
                } else {
                    ProviderFailure.Validation(
                        userMessage = error.message ?: "Invalid Kimi Coding Plan configuration."
                    )
                }
            }
            is IOException -> ProviderFailure.Transient(
                userMessage = "Unable to reach Kimi right now. Try again shortly."
            )
            else -> ProviderFailure.Unknown(
                userMessage = error.message ?: "Failed to sync Kimi Coding Plan."
            )
        }
    }

    private fun HttpException.toProviderFailure(): ProviderFailure {
        val message = message().takeIf { it.isNotBlank() }
            ?: "HTTP ${code()} while syncing Kimi Coding Plan."
        return when (code()) {
            401, 403 -> ProviderFailure.Auth(message)
            429 -> ProviderFailure.RateLimited(
                retryAfterMillis = retryAfterHeaderMillis(),
                userMessage = message
            )
            408 -> ProviderFailure.Transient(message)
            in 500..599 -> ProviderFailure.Transient(message)
            else -> ProviderFailure.Validation(message)
        }
    }

    private fun HttpException.retryAfterHeaderMillis(): Long? {
        val rawValue = response()?.headers()?.get("Retry-After") ?: return null
        return rawValue.toLongOrNull()?.times(1_000)
    }

    private fun authorization(credentials: SecretBundle): String {
        val rawValue = credentials.requireValue(API_KEY_FIELD)
        return if (rawValue.startsWith(BEARER_PREFIX, ignoreCase = true)) {
            rawValue
        } else {
            "$BEARER_PREFIX $rawValue"
        }
    }

    companion object {
        const val ID = "kimi"
        const val API_KEY_FIELD = "apiKey"
        const val RAW_PAYLOAD_FORMAT = "kimi.coding-usage-response.v1"
        const val NORMALIZER_VERSION = 1

        private const val BEARER_PREFIX = "Bearer"

        private val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }
}
