package com.lurenjia534.quotahub.data.provider.minimax

import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.provider.CapturedQuotaSnapshot
import com.lurenjia534.quotahub.data.provider.CodingPlanProvider
import com.lurenjia534.quotahub.data.provider.CredentialFieldSpec
import com.lurenjia534.quotahub.data.provider.ProviderReplayPayload
import com.lurenjia534.quotahub.data.provider.ProviderReplaySupport
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor
import com.lurenjia534.quotahub.data.provider.SecretBundle
import kotlinx.serialization.json.Json

class MiniMaxCodingPlanProvider : CodingPlanProvider {
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
        payloadFormat = RAW_PAYLOAD_FORMAT,
        normalizerVersion = NORMALIZER_VERSION
    )

    override suspend fun validate(credentials: SecretBundle): Result<CapturedQuotaSnapshot> {
        return runCatching {
            val fetchedAt = System.currentTimeMillis()
            val response = MiniMaxApiClient.apiService
                .getModelRemains(authorization(credentials))
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

    override suspend fun fetchSnapshot(subscription: Subscription): Result<CapturedQuotaSnapshot> {
        return validate(subscription.requireCredentials())
    }

    override fun replay(payload: ProviderReplayPayload): Result<CapturedQuotaSnapshot> {
        return runCatching {
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
