package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.Subscription

interface CodingPlanProvider {
    val descriptor: ProviderDescriptor

    suspend fun validate(credentials: SecretBundle): Result<QuotaSnapshot>

    suspend fun fetchSnapshot(subscription: Subscription): Result<QuotaSnapshot>
}
