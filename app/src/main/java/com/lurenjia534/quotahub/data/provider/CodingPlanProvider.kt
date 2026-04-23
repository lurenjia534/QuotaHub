package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.Subscription

interface CodingPlanProvider {
    val descriptor: ProviderDescriptor
    val replaySupport: ProviderReplaySupport?
        get() = null

    suspend fun validate(credentials: SecretBundle): Result<CapturedQuotaSnapshot>

    suspend fun fetchSnapshot(
        subscription: Subscription,
        credentials: SecretBundle
    ): Result<CapturedQuotaSnapshot>

    fun canReplay(payload: ProviderReplayPayload): Boolean {
        val support = replaySupport ?: return false
        return support.supportsFormat(payload.payloadFormat)
    }

    fun requiresReplay(payload: ProviderReplayPayload): Boolean {
        val support = replaySupport ?: return false
        return payload.normalizerVersion < support.normalizerVersion ||
            payload.payloadFormat != support.currentPayloadFormat
    }

    fun replayIncompatibilityReason(payload: ProviderReplayPayload): String {
        val support = replaySupport ?: return "Replay is unsupported for provider ${descriptor.id}."
        return buildString {
            append("Unsupported replay payload format: ${payload.payloadFormat}.")
            append(" Supported formats: ")
            append(support.supportedPayloadFormats.sorted().joinToString())
            append(".")
        }
    }

    fun replay(payload: ProviderReplayPayload): Result<CapturedQuotaSnapshot>
}
