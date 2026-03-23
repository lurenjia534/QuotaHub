package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.data.repository.QuotaRepository
import com.lurenjia534.quotahub.ui.screens.home.QuotaProvider
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class MiniMaxQuotaProviderGateway(
    private val repository: QuotaRepository
) : QuotaProviderGateway {
    override val provider: QuotaProvider = QuotaProvider.MiniMax

    override val snapshot = combine(repository.apiKey, repository.modelRemains) { apiKeyEntity, modelRemains ->
        QuotaProviderSnapshot(
            provider = provider,
            credential = apiKeyEntity?.key,
            modelRemains = modelRemains
        )
    }

    override suspend fun connect(credential: String): Result<Unit> {
        val trimmedCredential = credential.trim()
        if (trimmedCredential.isBlank()) {
            return Result.failure(IllegalArgumentException("API key is required"))
        }

        repository.saveApiKey(trimmedCredential)
        return refresh()
    }

    override suspend fun refresh(): Result<Unit> {
        val savedCredential = snapshot.first().credential
            ?: return Result.failure(IllegalStateException("API key is required"))

        return repository.getModelRemains("Bearer $savedCredential")
            .map { Unit }
    }

    override suspend fun disconnect() {
        repository.deleteApiKey()
    }
}
