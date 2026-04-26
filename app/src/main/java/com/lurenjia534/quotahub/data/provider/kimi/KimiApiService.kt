package com.lurenjia534.quotahub.data.provider.kimi

import retrofit2.http.GET
import retrofit2.http.Header

interface KimiApiService {
    @GET("coding/v1/usages")
    suspend fun getCodingUsage(
        @Header("Authorization") authorization: String
    ): KimiUsageResponse
}
