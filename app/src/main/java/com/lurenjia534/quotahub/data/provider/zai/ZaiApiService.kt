package com.lurenjia534.quotahub.data.provider.zai

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Query

interface ZaiApiService {
    @Headers(
        "Accept-Language: en-US,en",
        "Content-Type: application/json"
    )
    @GET("api/monitor/usage/model-usage")
    suspend fun getModelUsage(
        @Header("Authorization") authorization: String,
        @Query("startTime") startTime: String,
        @Query("endTime") endTime: String
    ): ZaiApiEnvelope<ZaiModelUsageData>

    @Headers(
        "Accept-Language: en-US,en",
        "Content-Type: application/json"
    )
    @GET("api/monitor/usage/tool-usage")
    suspend fun getToolUsage(
        @Header("Authorization") authorization: String,
        @Query("startTime") startTime: String,
        @Query("endTime") endTime: String
    ): ZaiApiEnvelope<ZaiToolUsageData>

    @Headers(
        "Accept-Language: en-US,en",
        "Content-Type: application/json"
    )
    @GET("api/monitor/usage/quota/limit")
    suspend fun getQuotaLimit(
        @Header("Authorization") authorization: String
    ): ZaiApiEnvelope<ZaiQuotaLimitData>
}
