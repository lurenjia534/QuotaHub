package com.lurenjia534.quotahub.data.repository

import com.lurenjia534.quotahub.data.api.MiniMaxApiClient
import com.lurenjia534.quotahub.data.local.ApiKeyDao
import com.lurenjia534.quotahub.data.local.ApiKeyEntity
import com.lurenjia534.quotahub.data.local.ModelRemainDao
import com.lurenjia534.quotahub.data.local.toEntity
import com.lurenjia534.quotahub.data.local.toModelRemain
import com.lurenjia534.quotahub.data.model.ModelRemain
import com.lurenjia534.quotahub.data.model.ModelRemainResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class QuotaRepository(
    private val apiKeyDao: ApiKeyDao,
    private val modelRemainDao: ModelRemainDao
) {
    val apiKey: Flow<ApiKeyEntity?> = apiKeyDao.getApiKey()
    val modelRemains: Flow<List<ModelRemain>> = modelRemainDao.getModelRemains().map { entities ->
        entities.map { it.toModelRemain() }
    }

    suspend fun getApiKeyOnce(): String? {
        return apiKeyDao.getApiKeyOnce()?.key
    }

    suspend fun saveApiKey(key: String) {
        apiKeyDao.insertApiKey(ApiKeyEntity(key = key))
    }

    suspend fun deleteApiKey() {
        apiKeyDao.deleteApiKey()
        modelRemainDao.clearModelRemains()
    }

    suspend fun getModelRemains(authorization: String): Result<ModelRemainResponse> {
        return try {
            val response = MiniMaxApiClient.apiService.getModelRemains(authorization)
            val cachedAt = System.currentTimeMillis()
            modelRemainDao.clearModelRemains()
            modelRemainDao.upsertModelRemains(
                response.modelRemains.map { it.toEntity(cachedAt) }
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
