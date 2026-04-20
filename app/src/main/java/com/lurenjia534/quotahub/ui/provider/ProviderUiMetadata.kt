package com.lurenjia534.quotahub.ui.provider

import androidx.annotation.DrawableRes
import com.lurenjia534.quotahub.R
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor

data class ProviderUiMetadata(
    val subtitle: String,
    @param:DrawableRes val iconRes: Int,
    val connectDescription: String,
    val detailDescription: String
)

class ProviderUiRegistry(
    internal val metadataById: Map<String, ProviderUiMetadata>
) {
    fun require(provider: ProviderDescriptor): ProviderUiMetadata {
        return require(provider.id)
    }

    fun require(providerId: String): ProviderUiMetadata {
        return metadataById[providerId]
            ?: throw IllegalArgumentException("Missing UI metadata for provider: $providerId")
    }

    fun getOrFallback(providerId: String): ProviderUiMetadata {
        return metadataById[providerId] ?: unsupportedProviderUiMetadata
    }
}

private val unsupportedProviderUiMetadata = ProviderUiMetadata(
    subtitle = "Unavailable in this app build",
    iconRes = R.drawable.provider_unavailable,
    connectDescription = "Stored provider data is still visible, but this provider is unavailable in the current app build.",
    detailDescription = "Stored provider data is still visible, but this provider is unavailable in the current app build."
)
