package com.lurenjia534.quotahub.bootstrap.provider

import com.lurenjia534.quotahub.R
import com.lurenjia534.quotahub.data.provider.CodingPlanProvider
import com.lurenjia534.quotahub.data.provider.SubscriptionCardProjector
import com.lurenjia534.quotahub.data.provider.codex.CodexCodingPlanProvider
import com.lurenjia534.quotahub.data.provider.codex.CodexSubscriptionCardProjector
import com.lurenjia534.quotahub.data.provider.minimax.MiniMaxCodingPlanProvider
import com.lurenjia534.quotahub.data.provider.minimax.MiniMaxSubscriptionCardProjector
import com.lurenjia534.quotahub.data.provider.zai.ZaiCodingPlanProvider
import com.lurenjia534.quotahub.data.provider.zai.ZaiSubscriptionCardProjector
import com.lurenjia534.quotahub.data.provider.zhipu.ZhipuCodingPlanProvider
import com.lurenjia534.quotahub.data.provider.zhipu.ZhipuSubscriptionCardProjector
import com.lurenjia534.quotahub.ui.screens.home.CodexProviderQuotaDetailProjector
import com.lurenjia534.quotahub.ui.provider.ProviderUiMetadata
import com.lurenjia534.quotahub.ui.screens.home.MiniMaxProviderQuotaDetailProjector
import com.lurenjia534.quotahub.ui.screens.home.ProviderQuotaDetailProjector
import com.lurenjia534.quotahub.ui.screens.home.ZaiProviderQuotaDetailProjector
import com.lurenjia534.quotahub.ui.screens.home.ZhipuProviderQuotaDetailProjector

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
                provider = CodexCodingPlanProvider(),
                uiMetadata = ProviderUiMetadata(
                    subtitle = "chatgpt.com",
                    iconRes = R.drawable.codex_color,
                    connectDescription = "Connect to OpenAI Codex quota usage",
                    detailDescription = "Monitor your Codex quota buckets and reset windows"
                ),
                cardProjector = CodexSubscriptionCardProjector(),
                detailProjector = CodexProviderQuotaDetailProjector()
            ),
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
            ),
            ProviderModule(
                provider = ZaiCodingPlanProvider(),
                uiMetadata = ProviderUiMetadata(
                    subtitle = "z.ai",
                    iconRes = R.drawable.zai_color,
                    connectDescription = "Connect to Z.ai monitor usage",
                    detailDescription = "Monitor your token and MCP quota windows"
                ),
                cardProjector = ZaiSubscriptionCardProjector(),
                detailProjector = ZaiProviderQuotaDetailProjector()
            ),
            ProviderModule(
                provider = ZhipuCodingPlanProvider(),
                uiMetadata = ProviderUiMetadata(
                    subtitle = "bigmodel.cn",
                    iconRes = R.drawable.zhipu_color,
                    connectDescription = "Connect to Zhipu monitor usage",
                    detailDescription = "Monitor your token and MCP quota windows"
                ),
                cardProjector = ZhipuSubscriptionCardProjector(),
                detailProjector = ZhipuProviderQuotaDetailProjector()
            )
        )
    )
}
