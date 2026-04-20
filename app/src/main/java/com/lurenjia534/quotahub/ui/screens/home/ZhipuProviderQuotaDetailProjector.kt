package com.lurenjia534.quotahub.ui.screens.home

class ZhipuProviderQuotaDetailProjector : ProviderQuotaDetailProjector by MonitorQuotaDetailProjector(
    providerName = "Zhipu"
)
