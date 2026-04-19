package com.lurenjia534.quotahub.data.provider

class ProviderCatalog(
    providers: List<CodingPlanProvider>
) {
    private val providersById = providers.associateBy { it.descriptor.id }

    val descriptors: List<ProviderDescriptor> = providers.map { it.descriptor }

    fun descriptor(providerId: String): ProviderDescriptor? {
        return providersById[providerId]?.descriptor
    }

    fun provider(providerId: String): CodingPlanProvider? {
        return providersById[providerId]
    }

    fun provider(descriptor: ProviderDescriptor): CodingPlanProvider? {
        return provider(descriptor.id)
    }
}
