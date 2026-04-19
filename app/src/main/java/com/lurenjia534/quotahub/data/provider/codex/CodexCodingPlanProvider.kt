package com.lurenjia534.quotahub.data.provider.codex

import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.provider.CapturedQuotaSnapshot
import com.lurenjia534.quotahub.data.provider.CodingPlanProvider
import com.lurenjia534.quotahub.data.provider.CredentialFieldSpec
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor
import com.lurenjia534.quotahub.data.provider.ProviderReplayPayload
import com.lurenjia534.quotahub.data.provider.ProviderReplaySupport
import com.lurenjia534.quotahub.data.provider.SecretBundle
import kotlinx.serialization.json.Json

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
        return runCatching {
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
        return runCatching {
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
