package com.lurenjia534.quotahub.data.provider

class ProviderCatalog(
    providers: List<CodingPlanProvider>
) {
    private val providersById = providers.associateBy { it.descriptor.id }

    val descriptors: List<ProviderDescriptor> = providers.map { it.descriptor }
    val replayContractFingerprint: String = providers
        .mapNotNull { provider ->
            provider.replaySupport?.let { support ->
                "${provider.descriptor.id}:${support.payloadFormat}:${support.normalizerVersion}"
            }
        }
        .sorted()
        .joinToString(separator = "|")

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
