package com.lurenjia534.quotahub.bootstrap.provider

import com.lurenjia534.quotahub.R
import com.lurenjia534.quotahub.data.provider.CodingPlanProvider
import com.lurenjia534.quotahub.data.provider.SubscriptionCardProjector
import com.lurenjia534.quotahub.data.provider.codex.CodexCodingPlanProvider
import com.lurenjia534.quotahub.data.provider.codex.CodexProviderQuotaDetailProjector
import com.lurenjia534.quotahub.data.provider.codex.CodexSubscriptionCardProjector
import com.lurenjia534.quotahub.data.provider.kimi.KimiCodingPlanProvider
import com.lurenjia534.quotahub.data.provider.kimi.KimiProviderQuotaDetailProjector
import com.lurenjia534.quotahub.data.provider.kimi.KimiSubscriptionCardProjector
import com.lurenjia534.quotahub.data.provider.minimax.MiniMaxCodingPlanProvider
import com.lurenjia534.quotahub.data.provider.minimax.MiniMaxProviderQuotaDetailProjector
import com.lurenjia534.quotahub.data.provider.minimax.MiniMaxSubscriptionCardProjector
import com.lurenjia534.quotahub.data.provider.monitor.MonitorQuotaDetailProjector
import com.lurenjia534.quotahub.data.provider.monitor.MonitorQuotaSubscriptionCardProjector
import com.lurenjia534.quotahub.data.provider.zai.ZaiCodingPlanProvider
import com.lurenjia534.quotahub.data.provider.zhipu.ZhipuCodingPlanProvider
import com.lurenjia534.quotahub.ui.provider.ProviderUiMetadata
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
                provider = KimiCodingPlanProvider(),
                uiMetadata = ProviderUiMetadata(
                    subtitle = "api.kimi.com",
                    iconRes = R.drawable.kimi,
                    connectDescription = "Connect to Kimi Coding Plan usage",
                    detailDescription = "Monitor your Kimi coding quota and reset windows"
                ),
                cardProjector = KimiSubscriptionCardProjector(),
                detailProjector = KimiProviderQuotaDetailProjector()
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
                cardProjector = MonitorQuotaSubscriptionCardProjector(),
                detailProjector = MonitorQuotaDetailProjector(providerName = "Z.ai")
            ),
            ProviderModule(
                provider = ZhipuCodingPlanProvider(),
                uiMetadata = ProviderUiMetadata(
                    subtitle = "bigmodel.cn",
                    iconRes = R.drawable.zhipu_color,
                    connectDescription = "Connect to Zhipu monitor usage",
                    detailDescription = "Monitor your token and MCP quota windows"
                ),
                cardProjector = MonitorQuotaSubscriptionCardProjector(),
                detailProjector = MonitorQuotaDetailProjector(providerName = "Zhipu")
            )
        )
    )
}
