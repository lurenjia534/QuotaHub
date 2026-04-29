package com.lurenjia534.quotahub.data.provider.zhipu

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import com.lurenjia534.quotahub.data.network.HttpClientFactory
import retrofit2.Retrofit

object ZhipuApiClient {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttpClient = HttpClientFactory.create(timeoutSeconds = 30)

    fun createService(baseUrl: String): ZhipuApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl.ensureTrailingSlash())
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ZhipuApiService::class.java)
    }
}

private fun String.ensureTrailingSlash(): String {
    return if (endsWith("/")) this else "$this/"
}
