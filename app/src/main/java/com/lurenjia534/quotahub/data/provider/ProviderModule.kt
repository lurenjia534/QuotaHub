package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.R
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

object ProviderModules {
    val all: List<ProviderModule> = listOf(
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
}
