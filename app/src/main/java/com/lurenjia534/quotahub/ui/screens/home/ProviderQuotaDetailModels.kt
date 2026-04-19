package com.lurenjia534.quotahub.ui.screens.home

import com.lurenjia534.quotahub.data.model.QuotaRisk

data class ProviderQuotaDetailUiModel(
    val summary: ProviderQuotaSummaryUiModel? = null,
    val sectionTitle: String = "Model quota",
    val sectionSubtitle: String = "Pull down anytime to refresh remote values.",
    val resources: List<ProviderQuotaResourceUiModel> = emptyList()
) {
    val hasData: Boolean
        get() = resources.isNotEmpty()
}

data class ProviderQuotaSummaryUiModel(
    val risk: QuotaRisk,
    val syncLabel: String,
    val syncDescription: String,
    val headlineValue: String,
    val headlineLabel: String,
    val stateLabel: String,
    val stateDescription: String,
    val primaryMetrics: SummaryMetricRowUiModel,
    val secondaryMetrics: SummaryMetricRowUiModel? = null
)

data class SummaryMetricRowUiModel(
    val first: LabeledValueUiModel,
    val second: LabeledValueUiModel,
    val third: LabeledValueUiModel
)

data class ProviderQuotaResourceUiModel(
    val key: String,
    val title: String,
    val resetLabel: String,
    val risk: QuotaRisk,
    val progress: Float,
    val primaryMetrics: List<LabeledValueUiModel>,
    val secondaryMetrics: List<LabeledValueUiModel> = emptyList()
)

data class LabeledValueUiModel(
    val label: String,
    val value: String,
    val highlightRisk: Boolean = false
)
