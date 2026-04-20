package com.lurenjia534.quotahub.ui.provider

import androidx.annotation.DrawableRes
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
}
