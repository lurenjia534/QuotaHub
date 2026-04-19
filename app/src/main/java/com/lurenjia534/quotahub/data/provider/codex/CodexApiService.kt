package com.lurenjia534.quotahub.data.provider.codex

import retrofit2.http.GET
import retrofit2.http.Header

interface CodexApiService {
    @GET("wham/usage")
    suspend fun getUsage(
        @Header("Authorization") authorization: String,
        @Header("ChatGPT-Account-Id") accountId: String?
    ): CodexUsageResponse
}
