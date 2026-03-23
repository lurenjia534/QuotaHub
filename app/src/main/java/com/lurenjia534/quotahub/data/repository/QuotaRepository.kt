package com.lurenjia534.quotahub.data.repository

import com.lurenjia534.quotahub.data.api.MiniMaxApiClient
import com.lurenjia534.quotahub.data.local.ApiKeyDao
import com.lurenjia534.quotahub.data.local.ApiKeyEntity
import com.lurenjia534.quotahub.data.model.ModelRemainResponse
import kotlinx.coroutines.flow.Flow

class QuotaRepository(private val apiKeyDao: ApiKeyDao) {
    val apiKey: Flow<ApiKeyEntity?> = apiKeyDao.getApiKey()

    suspend fun getApiKeyOnce(): String? {
        return apiKeyDao.getApiKeyOnce()?.key
    }

    suspend fun saveApiKey(key: String) {
        apiKeyDao.insertApiKey(ApiKeyEntity(key = key))
    }

    suspend fun deleteApiKey() {
        apiKeyDao.deleteApiKey()
    }

    suspend fun getModelRemains(authorization: String): Result<ModelRemainResponse> {
        return try {
            val response = MiniMaxApiClient.apiService.getModelRemains(authorization)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}