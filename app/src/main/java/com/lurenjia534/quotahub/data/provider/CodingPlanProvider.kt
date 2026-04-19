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
        return payload.payloadFormat == support.payloadFormat
    }

    fun requiresReplay(payload: ProviderReplayPayload): Boolean {
        val support = replaySupport ?: return false
        return canReplay(payload) && payload.normalizerVersion < support.normalizerVersion
    }

    fun replay(payload: ProviderReplayPayload): Result<CapturedQuotaSnapshot>
}
