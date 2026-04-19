package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.data.model.QuotaSnapshot

data class ProviderReplayPayload(
    val fetchedAt: Long,
    val payloadFormat: String,
    val rawPayloadJson: String,
    val normalizerVersion: Int
)

data class ProviderReplaySupport(
    val payloadFormat: String,
    val normalizerVersion: Int
)

data class CapturedQuotaSnapshot(
    val snapshot: QuotaSnapshot,
    val replayPayload: ProviderReplayPayload? = null
)
