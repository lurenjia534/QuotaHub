package com.lurenjia534.quotahub.data.network

import com.lurenjia534.quotahub.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object HttpClientFactory {
    fun create(
        timeoutSeconds: Long = 30
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(redactedLoggingInterceptor())
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .build()
    }

    private fun redactedLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            SENSITIVE_HEADERS.forEach(::redactHeader)
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    private val SENSITIVE_HEADERS = listOf(
        "Authorization",
        "ChatGPT-Account-Id",
        "Cookie",
        "Set-Cookie",
        "X-Api-Key",
        "X-Auth-Token"
    )
}
