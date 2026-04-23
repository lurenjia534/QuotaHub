package com.lurenjia534.quotahub.data.provider.minimax

import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.provider.CapturedQuotaSnapshot
import com.lurenjia534.quotahub.data.provider.CodingPlanProvider
import com.lurenjia534.quotahub.data.provider.CredentialFieldSpec
import com.lurenjia534.quotahub.data.provider.ProviderFailure
import com.lurenjia534.quotahub.data.provider.ProviderReplayPayload
import com.lurenjia534.quotahub.data.provider.ProviderReplaySupport
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor
import com.lurenjia534.quotahub.data.provider.ProviderSyncException
import com.lurenjia534.quotahub.data.provider.SecretBundle
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class MiniMaxCodingPlanProvider(
    private val apiService: MiniMaxApiService = MiniMaxApiClient.apiService
) : CodingPlanProvider {

    override val descriptor: ProviderDescriptor = ProviderDescriptor(
        id = ID,
        displayName = "MiniMax Coding Plan",
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
            val response = apiService.getModelRemains(authorization(credentials))
            CapturedQuotaSnapshot(
                snapshot = response.toQuotaSnapshot(fetchedAt),
                replayPayload = ProviderReplayPayload(
                    fetchedAt = fetchedAt,
                    payloadFormat = RAW_PAYLOAD_FORMAT,
                    rawPayloadJson = json.encodeToString(MiniMaxQuotaResponse.serializer(), response),
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
            val snapshot = json.decodeFromString(
                MiniMaxQuotaResponse.serializer(),
                payload.rawPayloadJson
            ).toQuotaSnapshot(fetchedAt = payload.fetchedAt)
            CapturedQuotaSnapshot(
                snapshot = snapshot,
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
                failure = minimaxFailure(error),
                cause = error
            )
        }
    }

    private fun minimaxFailure(error: Throwable): ProviderFailure {
        return when (error) {
            is HttpException -> error.toProviderFailure()
            is MiniMaxApiException -> error.toProviderFailure()
            is SerializationException -> ProviderFailure.SchemaChanged(
                userMessage = "Stored MiniMax payload could not be parsed. Refresh to capture a new snapshot."
            )
            is IllegalStateException,
            is IllegalArgumentException -> {
                if (error.message?.startsWith("Unsupported replay payload format") == true) {
                    ProviderFailure.SchemaChanged(
                        userMessage = error.message ?: "Stored MiniMax replay payload is no longer supported."
                    )
                } else {
                    ProviderFailure.Validation(
                        userMessage = error.message ?: "Invalid MiniMax Coding Plan configuration."
                    )
                }
            }
            is IOException -> ProviderFailure.Transient(
                userMessage = "Unable to reach MiniMax right now. Try again shortly."
            )
            else -> ProviderFailure.Unknown(
                userMessage = error.message ?: "Failed to sync MiniMax Coding Plan."
            )
        }
    }

    private fun MiniMaxApiException.toProviderFailure(): ProviderFailure {
        val message = message?.takeIf { it.isNotBlank() }
            ?: "MiniMax request failed with status code $statusCode."
        return when (statusCode) {
            1004 -> ProviderFailure.Auth(message)
            1002 -> ProviderFailure.RateLimited(userMessage = message)
            1000, 1001, 1024, 1033 -> ProviderFailure.Transient(message)
            1008, 1026, 1027, 1039, 1042, 1043 -> ProviderFailure.Validation(message)
            1041 -> ProviderFailure.RateLimited(userMessage = message)
            else -> ProviderFailure.Unknown(message)
        }
    }

    private fun HttpException.toProviderFailure(): ProviderFailure {
        val message = message().takeIf { it.isNotBlank() }
            ?: "HTTP ${code()} while syncing MiniMax Coding Plan."
        return when {
            code() == 401 || code() == 403 || message.looksAuthRelated() -> ProviderFailure.Auth(message)
            code() == 429 -> ProviderFailure.RateLimited(
                retryAfterMillis = retryAfterHeaderMillis(),
                userMessage = message
            )
            code() == 408 || code() in 500..599 || message.looksTransient() -> ProviderFailure.Transient(message)
            else -> ProviderFailure.Validation(message)
        }
    }

    private fun HttpException.retryAfterHeaderMillis(): Long? {
        val rawValue = response()?.headers()?.get("Retry-After") ?: return null
        return rawValue.toLongOrNull()?.times(1_000)
    }

    private fun String.looksAuthRelated(): Boolean {
        val rawMessage = lowercase()
        return listOf(
            "auth",
            "token",
            "unauthorized",
            "forbidden",
            "credential",
            "api key",
            "apikey",
            "permission",
            "login"
        ).any(rawMessage::contains)
    }

    private fun String.looksTransient(): Boolean {
        val rawMessage = lowercase()
        return listOf(
            "timeout",
            "temporarily unavailable",
            "server error",
            "bad gateway",
            "gateway timeout",
            "service unavailable",
            "internal error"
        ).any(rawMessage::contains)
    }

    private fun authorization(credentials: SecretBundle): String {
        return "Bearer ${credentials.requireValue(API_KEY_FIELD)}"
    }

    companion object {
        const val ID = "minimax"
        const val API_KEY_FIELD = "apiKey"
        const val RAW_PAYLOAD_FORMAT = "minimax.quota-response.v1"
        const val NORMALIZER_VERSION = 1

        private val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }
}
