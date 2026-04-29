package com.lurenjia534.quotahub.data.update

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import com.lurenjia534.quotahub.data.network.HttpClientFactory
import retrofit2.Retrofit

object GitHubReleaseApiClient {
    private const val BASE_URL = "https://api.github.com/"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttpClient = HttpClientFactory.create(timeoutSeconds = 20)

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val releaseService: GitHubReleaseService = retrofit.create(GitHubReleaseService::class.java)
}
