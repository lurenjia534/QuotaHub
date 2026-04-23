package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.data.model.QuotaSnapshot

data class ProviderReplayPayload(
    val fetchedAt: Long,
    val payloadFormat: String,
    val rawPayloadJson: String,
    val normalizerVersion: Int
)

data class ProviderReplaySupport(
    val currentPayloadFormat: String,
    val supportedPayloadFormats: Set<String> = setOf(currentPayloadFormat),
    val normalizerVersion: Int
) {
    init {
        require(currentPayloadFormat in supportedPayloadFormats) {
            "currentPayloadFormat must be included in supportedPayloadFormats"
        }
    }

    fun supportsFormat(payloadFormat: String): Boolean {
        return payloadFormat in supportedPayloadFormats
    }
}

data class CapturedQuotaSnapshot(
    val snapshot: QuotaSnapshot,
    val replayPayload: ProviderReplayPayload? = null
)
