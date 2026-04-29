package com.lurenjia534.quotahub.data.provider.codex

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import com.lurenjia534.quotahub.data.network.HttpClientFactory
import retrofit2.Retrofit

object CodexApiClient {
    private const val BASE_URL = "https://chatgpt.com/backend-api/"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttpClient = HttpClientFactory.create(timeoutSeconds = 30)

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val apiService: CodexApiService = retrofit.create(CodexApiService::class.java)
}
