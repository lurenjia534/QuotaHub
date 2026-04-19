package com.lurenjia534.quotahub.bootstrap.provider

import com.lurenjia534.quotahub.R
import com.lurenjia534.quotahub.data.provider.CodingPlanProvider
import com.lurenjia534.quotahub.data.provider.SubscriptionCardProjector
import com.lurenjia534.quotahub.data.provider.minimax.MiniMaxCodingPlanProvider
import com.lurenjia534.quotahub.data.provider.minimax.MiniMaxSubscriptionCardProjector
import com.lurenjia534.quotahub.ui.provider.ProviderUiMetadata
import com.lurenjia534.quotahub.ui.screens.home.MiniMaxProviderQuotaDetailProjector
import com.lurenjia534.quotahub.ui.screens.home.ProviderQuotaDetailProjector

data class ProviderModule(
    val provider: CodingPlanProvider,
    val uiMetadata: ProviderUiMetadata,
    val cardProjector: SubscriptionCardProjector,
    val detailProjector: ProviderQuotaDetailProjector
)

internal fun requireValidProviderModules(modules: List<ProviderModule>): List<ProviderModule> {
    val duplicateProviderIds = modules
        .groupBy { it.provider.descriptor.id }
        .filterValues { it.size > 1 }
        .keys
        .sorted()
    require(duplicateProviderIds.isEmpty()) {
        "Duplicate provider module ids: ${duplicateProviderIds.joinToString()}"
    }
    return modules
}

object ProviderModules {
    val all: List<ProviderModule> = requireValidProviderModules(
        listOf(
            ProviderModule(
                provider = MiniMaxCodingPlanProvider(),
                uiMetadata = ProviderUiMetadata(
                    subtitle = "minimaxi.com",
                    iconRes = R.drawable.minimax_color,
                    connectDescription = "Connect to MiniMax API",
                    detailDescription = "Monitor your MiniMax quota usage"
                ),
                cardProjector = MiniMaxSubscriptionCardProjector(),
                detailProjector = MiniMaxProviderQuotaDetailProjector()
            )
        )
    )
}
