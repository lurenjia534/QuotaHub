package com.lurenjia534.quotahub.data.provider.minimax

import retrofit2.http.GET
import retrofit2.http.Header

interface MiniMaxApiService {
    @GET("v1/api/openplatform/coding_plan/remains")
    suspend fun getModelRemains(
        @Header("Authorization") authorization: String
    ): MiniMaxQuotaResponse
}
