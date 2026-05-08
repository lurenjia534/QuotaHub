package com.lurenjia534.quotahub.data.cloud

object CloudSubscriptionIdentity {
    private const val PREFIX = "cloud::relay::v1::"

    fun credentialMarker(remoteSubscriptionId: String): String {
        return PREFIX + remoteSubscriptionId
    }

    fun remoteSubscriptionId(storedCredentialPayload: String): String? {
        return storedCredentialPayload
            .takeIf { it.startsWith(PREFIX) }
            ?.removePrefix(PREFIX)
            ?.takeIf { it.isNotBlank() }
    }
}
