package com.lurenjia534.quotahub.bootstrap.provider

import com.lurenjia534.quotahub.data.provider.SubscriptionCardProjectorRegistry
import com.lurenjia534.quotahub.ui.provider.ProviderUiRegistry
import com.lurenjia534.quotahub.ui.screens.home.ProviderQuotaDetailProjectorRegistry

object ProviderRegistryAssembly {
    fun providerUiRegistry(modules: List<ProviderModule>): ProviderUiRegistry {
        val validatedModules = requireValidProviderModules(modules)
        return ProviderUiRegistry(
            metadataById = validatedModules.associate { module ->
                module.provider.descriptor.id to module.uiMetadata
            }
        )
    }

    fun subscriptionCardProjectorRegistry(
        modules: List<ProviderModule>
    ): SubscriptionCardProjectorRegistry {
        val validatedModules = requireValidProviderModules(modules)
        return SubscriptionCardProjectorRegistry(
            projectors = validatedModules.associate { module ->
                module.provider.descriptor.id to module.cardProjector
            }
        )
    }

    fun providerQuotaDetailProjectorRegistry(
        modules: List<ProviderModule>
    ): ProviderQuotaDetailProjectorRegistry {
        val validatedModules = requireValidProviderModules(modules)
        return ProviderQuotaDetailProjectorRegistry(
            projectors = validatedModules.associate { module ->
                module.provider.descriptor.id to module.detailProjector
            }
        )
    }
}
