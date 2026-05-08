package com.lurenjia534.quotahub.data.cloud

import com.lurenjia534.quotahub.data.network.HttpClientFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class QuotaHubRelayClient(
    private val okHttpClient: okhttp3.OkHttpClient = HttpClientFactory.create(timeoutSeconds = 30),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
) {
    suspend fun listProviders(baseUrl: String, clientToken: String): List<RelayProviderDescriptorDto> {
        return get<RelayProvidersResponse>(
            baseUrl = baseUrl,
            pathSegments = listOf("api", "relay", "providers"),
            clientToken = clientToken
        ).providers
    }

    suspend fun listSubscriptions(baseUrl: String, clientToken: String): RelaySubscriptionsResponse {
        return get(
            baseUrl = baseUrl,
            pathSegments = listOf("api", "relay", "subscriptions"),
            clientToken = clientToken
        )
    }

    suspend fun refreshSubscription(
        baseUrl: String,
        clientToken: String,
        subscriptionId: String
    ): RelaySubscriptionDto {
        return post<RelaySubscriptionResponse>(
            baseUrl = baseUrl,
            pathSegments = listOf("api", "relay", "subscriptions", subscriptionId, "refresh"),
            clientToken = clientToken
        ).subscription
    }

    private suspend inline fun <reified T> get(
        baseUrl: String,
        pathSegments: List<String>,
        clientToken: String
    ): T {
        val request = Request.Builder()
            .url(relayUrl(baseUrl, pathSegments))
            .header("Authorization", "Bearer $clientToken")
            .get()
            .build()
        return execute(request)
    }

    private suspend inline fun <reified T> post(
        baseUrl: String,
        pathSegments: List<String>,
        clientToken: String
    ): T {
        val request = Request.Builder()
            .url(relayUrl(baseUrl, pathSegments))
            .header("Authorization", "Bearer $clientToken")
            .post(ByteArray(0).toRequestBody())
            .build()
        return execute(request)
    }

    private suspend inline fun <reified T> execute(request: Request): T {
        return withContext(Dispatchers.IO) {
            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body.string()
                if (!response.isSuccessful) {
                    throw relayException(response.code, body)
                }
                json.decodeFromString<T>(body)
            }
        }
    }

    private fun relayUrl(baseUrl: String, pathSegments: List<String>): okhttp3.HttpUrl {
        val parsedBaseUrl = normalizeBaseUrl(baseUrl).toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Relay URL must start with http:// or https://.")
        return parsedBaseUrl.newBuilder().apply {
            pathSegments.forEach(::addPathSegment)
        }.build()
    }

    private fun normalizeBaseUrl(baseUrl: String): String {
        return baseUrl.trim().trimEnd('/') + "/"
    }

    private fun relayException(statusCode: Int, body: String): RelayApiException {
        val relayError = runCatching {
            json.decodeFromString<RelayErrorResponse>(body).error
        }.getOrNull()
        return RelayApiException(
            statusCode = statusCode,
            code = relayError?.code,
            userMessage = relayError?.message
                ?: "Relay request failed with HTTP $statusCode."
        )
    }
}

class RelayApiException(
    val statusCode: Int,
    val code: String?,
    val userMessage: String
) : IllegalStateException(userMessage)
