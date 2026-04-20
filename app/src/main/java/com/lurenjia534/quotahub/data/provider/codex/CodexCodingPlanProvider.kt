package com.lurenjia534.quotahub.data.provider.codex

import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.provider.CapturedQuotaSnapshot
import com.lurenjia534.quotahub.data.provider.CodingPlanProvider
import com.lurenjia534.quotahub.data.provider.CredentialFieldSpec
import com.lurenjia534.quotahub.data.provider.ProviderFailure
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor
import com.lurenjia534.quotahub.data.provider.ProviderReplayPayload
import com.lurenjia534.quotahub.data.provider.ProviderReplaySupport
import com.lurenjia534.quotahub.data.provider.ProviderSyncException
import com.lurenjia534.quotahub.data.provider.SecretBundle
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class CodexCodingPlanProvider(
    private val apiService: CodexApiService = CodexApiClient.apiService,
    private val json: Json = JSON
) : CodingPlanProvider {
    override val descriptor: ProviderDescriptor = ProviderDescriptor(
        id = ID,
        displayName = "OpenAI Codex",
        credentialFields = listOf(
            CredentialFieldSpec(
                key = ACCESS_TOKEN_FIELD,
                label = "Access Token"
            ),
            CredentialFieldSpec(
                key = ACCOUNT_ID_FIELD,
                label = "ChatGPT Account ID (optional)",
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
        return captureFailures {
            val fetchedAt = System.currentTimeMillis()
            val response = apiService.getUsage(
                authorization = authorization(credentials),
                accountId = credentials.value(ACCOUNT_ID_FIELD)
            )
            CapturedQuotaSnapshot(
                snapshot = response.toQuotaSnapshot(fetchedAt),
                replayPayload = ProviderReplayPayload(
                    fetchedAt = fetchedAt,
                    payloadFormat = RAW_PAYLOAD_FORMAT,
                    rawPayloadJson = json.encodeToString(CodexUsageResponse.serializer(), response),
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
            val response = json.decodeFromString(CodexUsageResponse.serializer(), payload.rawPayloadJson)
            CapturedQuotaSnapshot(
                snapshot = response.toQuotaSnapshot(payload.fetchedAt),
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
                failure = codexFailure(error),
                cause = error
            )
        }
    }

    private fun codexFailure(error: Throwable): ProviderFailure {
        return when (error) {
            is HttpException -> error.toProviderFailure()
            is SerializationException -> ProviderFailure.SchemaChanged(
                userMessage = "Stored OpenAI Codex payload could not be parsed. Refresh to capture a new snapshot."
            )
            is IllegalStateException,
            is IllegalArgumentException -> {
                if (error.message?.startsWith("Unsupported replay payload format") == true) {
                    ProviderFailure.SchemaChanged(
                        userMessage = error.message ?: "Stored OpenAI Codex replay payload is no longer supported."
                    )
                } else {
                    ProviderFailure.Validation(
                        userMessage = error.message ?: "Invalid OpenAI Codex configuration."
                    )
                }
            }
            is IOException -> ProviderFailure.Transient(
                userMessage = "Unable to reach OpenAI Codex right now. Try again shortly."
            )
            else -> ProviderFailure.Unknown(
                userMessage = error.message ?: "Failed to sync OpenAI Codex."
            )
        }
    }

    private fun HttpException.toProviderFailure(): ProviderFailure {
        val message = message().takeIf { it.isNotBlank() }
            ?: "HTTP ${code()} while syncing OpenAI Codex."
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
        return "Bearer ${credentials.requireValue(ACCESS_TOKEN_FIELD)}"
    }

    companion object {
        const val ID = "codex"
        const val ACCESS_TOKEN_FIELD = "accessToken"
        const val ACCOUNT_ID_FIELD = "accountId"
        const val RAW_PAYLOAD_FORMAT = "codex.usage-response.v1"
        const val NORMALIZER_VERSION = 1

        private val JSON = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }
}
