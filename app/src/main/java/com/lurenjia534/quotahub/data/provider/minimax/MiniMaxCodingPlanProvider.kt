package com.lurenjia534.quotahub.data.provider.minimax

import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.provider.CodingPlanProvider
import com.lurenjia534.quotahub.data.provider.CredentialFieldSpec
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor
import com.lurenjia534.quotahub.data.provider.SecretBundle

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
    override suspend fun validate(credentials: SecretBundle): Result<QuotaSnapshot> {
        return runCatching {
            MiniMaxApiClient.apiService
                .getModelRemains(authorization(credentials))
                .toQuotaSnapshot()
        }
    }

    override suspend fun fetchSnapshot(subscription: Subscription): Result<QuotaSnapshot> {
        return validate(subscription.credentials)
    }

    private fun authorization(credentials: SecretBundle): String {
        return "Bearer ${credentials.requireValue(API_KEY_FIELD)}"
    }

    companion object {
        const val ID = "minimax"
        const val API_KEY_FIELD = "apiKey"
    }
}
