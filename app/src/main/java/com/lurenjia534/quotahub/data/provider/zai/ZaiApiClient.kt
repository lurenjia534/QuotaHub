package com.lurenjia534.quotahub.data.provider.zai

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import com.lurenjia534.quotahub.data.network.HttpClientFactory
import retrofit2.Retrofit

object ZaiApiClient {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttpClient = HttpClientFactory.create(timeoutSeconds = 30)

    fun createService(baseUrl: String): ZaiApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl.ensureTrailingSlash())
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ZaiApiService::class.java)
    }
}

private fun String.ensureTrailingSlash(): String {
    return if (endsWith("/")) this else "$this/"
}
