package com.lurenjia534.quotahub.data.api

import com.lurenjia534.quotahub.data.model.ModelRemainResponse
import retrofit2.http.GET
import retrofit2.http.Header

interface MiniMaxApiService {
    @GET("v1/api/openplatform/coding_plan/remains")
    suspend fun getModelRemains(
        @Header("Authorization") authorization: String
    ): ModelRemainResponse
}