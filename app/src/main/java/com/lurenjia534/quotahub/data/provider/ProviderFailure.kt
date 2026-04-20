package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.data.model.SyncState
import com.lurenjia534.quotahub.data.provider.monitor.MonitorQuotaEnvelopeException
import com.lurenjia534.quotahub.data.provider.monitor.MonitorQuotaEnvelopeFailureKind
import java.io.IOException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

sealed interface ProviderFailure {
    val userMessage: String

    data class Auth(
        override val userMessage: String
    ) : ProviderFailure

    data class RateLimited(
        val retryAfterMillis: Long? = null,
        override val userMessage: String
    ) : ProviderFailure

    data class Transient(
        override val userMessage: String
    ) : ProviderFailure

    data class SchemaChanged(
        override val userMessage: String
    ) : ProviderFailure

    data class Validation(
        override val userMessage: String
    ) : ProviderFailure

    data class Unknown(
        override val userMessage: String
    ) : ProviderFailure
}

class ProviderSyncException(
    val failure: ProviderFailure,
    cause: Throwable? = null
) : IllegalStateException(failure.userMessage, cause)

fun ProviderFailure.toSyncState(): SyncState {
    return when (this) {
        is ProviderFailure.Auth -> SyncState.AuthFailed
        is ProviderFailure.RateLimited,
        is ProviderFailure.Transient,
        is ProviderFailure.SchemaChanged,
        is ProviderFailure.Validation,
        is ProviderFailure.Unknown -> SyncState.SyncError
    }
}

fun Throwable.toProviderFailureOrNull(): ProviderFailure? {
    return when (this) {
        is ProviderSyncException -> failure
        else -> null
    }
}

internal fun monitorQuotaFailure(
    providerName: String,
    error: Throwable
): ProviderFailure {
    return when (error) {
        is ProviderSyncException -> error.failure
        is MonitorQuotaEnvelopeException -> error.toProviderFailure()
        is HttpException -> error.toProviderFailure(providerName)
        is SerializationException -> ProviderFailure.SchemaChanged(
            userMessage = "Stored $providerName payload could not be parsed. Refresh to capture a new snapshot."
        )
        is IllegalArgumentException -> {
            if (error.message?.startsWith("Unsupported replay payload format") == true) {
                ProviderFailure.SchemaChanged(
                    userMessage = error.message ?: "Stored $providerName replay payload is no longer supported."
                )
            } else {
                ProviderFailure.Validation(
                    userMessage = error.message ?: "Invalid $providerName configuration."
                )
            }
        }
        is IOException -> ProviderFailure.Transient(
            userMessage = "Unable to reach $providerName right now. Try again shortly."
        )
        else -> ProviderFailure.Unknown(
            userMessage = error.message ?: "Failed to sync $providerName."
        )
    }
}

private fun MonitorQuotaEnvelopeException.toProviderFailure(): ProviderFailure {
    return when {
        code == 401 || code == 403 || messageLooksAuthRelated() -> ProviderFailure.Auth(
            userMessage = message ?: "Authentication failed."
        )
        code == 429 -> ProviderFailure.RateLimited(
            userMessage = message ?: "Request quota exhausted. Try again later."
        )
        kind == MonitorQuotaEnvelopeFailureKind.MissingData -> ProviderFailure.SchemaChanged(
            userMessage = message ?: "The provider response shape changed and could not be normalized."
        )
        code != null && code >= 500 -> ProviderFailure.Transient(
            userMessage = message ?: "Provider service is temporarily unavailable."
        )
        else -> ProviderFailure.Validation(
            userMessage = message ?: "Provider response could not be validated."
        )
    }
}

private fun HttpException.toProviderFailure(providerName: String): ProviderFailure {
    val message = message()
        .takeIf { it.isNotBlank() }
        ?: "HTTP ${code()} while syncing $providerName."
    return when (code()) {
        401, 403 -> ProviderFailure.Auth(message)
        429 -> ProviderFailure.RateLimited(
            retryAfterMillis = retryAfterHeaderMillis(),
            userMessage = message
        )
        408 -> ProviderFailure.Transient(message)
        in 500..599 -> ProviderFailure.Transient(message)
        else -> ProviderFailure.Validation(message)
    }
}

private fun HttpException.retryAfterHeaderMillis(): Long? {
    val rawValue = response()?.headers()?.get("Retry-After") ?: return null
    return rawValue.toLongOrNull()?.times(1_000)
}

private fun MonitorQuotaEnvelopeException.messageLooksAuthRelated(): Boolean {
    val rawMessage = message?.lowercase() ?: return false
    return listOf(
        "auth",
        "token",
        "unauthorized",
        "forbidden",
        "credential",
        "api key",
        "apikey",
        "permission",
        "login"
    ).any(rawMessage::contains)
}
