package com.lurenjia534.quotahub.data.provider.monitor

enum class MonitorQuotaEnvelopeFailureKind {
    RequestRejected,
    MissingData
}

class MonitorQuotaEnvelopeException(
    val endpointName: String,
    val code: Int? = null,
    val success: Boolean? = null,
    val responseMessage: String? = null,
    val kind: MonitorQuotaEnvelopeFailureKind
) : IllegalStateException(
    responseMessage ?: when (kind) {
        MonitorQuotaEnvelopeFailureKind.RequestRejected ->
            "Request to $endpointName failed"
        MonitorQuotaEnvelopeFailureKind.MissingData ->
            "Missing data in $endpointName response"
    }
)

internal fun <T> requireMonitorQuotaData(
    endpointName: String,
    success: Boolean?,
    code: Int?,
    responseMessage: String?,
    data: T?
): T {
    if (success == false) {
        throw MonitorQuotaEnvelopeException(
            endpointName = endpointName,
            code = code,
            success = success,
            responseMessage = responseMessage,
            kind = MonitorQuotaEnvelopeFailureKind.RequestRejected
        )
    }
    if (code != null && code != 200) {
        throw MonitorQuotaEnvelopeException(
            endpointName = endpointName,
            code = code,
            success = success,
            responseMessage = responseMessage
                ?: "Unexpected response code $code from $endpointName",
            kind = MonitorQuotaEnvelopeFailureKind.RequestRejected
        )
    }
    return data ?: throw MonitorQuotaEnvelopeException(
        endpointName = endpointName,
        code = code,
        success = success,
        responseMessage = "Missing data in $endpointName response",
        kind = MonitorQuotaEnvelopeFailureKind.MissingData
    )
}
