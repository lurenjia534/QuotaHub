package com.lurenjia534.quotahub.ui.provider

import androidx.annotation.DrawableRes
import com.lurenjia534.quotahub.bootstrap.provider.ProviderModule
import com.lurenjia534.quotahub.bootstrap.provider.requireValidProviderModules
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor

data class ProviderUiMetadata(
    val subtitle: String,
    @param:DrawableRes val iconRes: Int,
    val connectDescription: String,
    val detailDescription: String
)

class ProviderUiRegistry(
    private val metadataById: Map<String, ProviderUiMetadata>
) {
    fun require(provider: ProviderDescriptor): ProviderUiMetadata {
        return require(provider.id)
    }

    fun require(providerId: String): ProviderUiMetadata {
        return metadataById[providerId]
            ?: throw IllegalArgumentException("Missing UI metadata for provider: $providerId")
    }

    companion object {
        fun fromModules(modules: List<ProviderModule>): ProviderUiRegistry {
            val validatedModules = requireValidProviderModules(modules)
            return ProviderUiRegistry(
                metadataById = validatedModules.associate { module ->
                    module.provider.descriptor.id to module.uiMetadata
                }
            )
        }
    }
}
