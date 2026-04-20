package com.lurenjia534.quotahub.bootstrap.provider

import com.lurenjia534.quotahub.data.provider.SubscriptionCardProjectorRegistry
import com.lurenjia534.quotahub.ui.screens.home.ProviderQuotaDetailProjectorRegistry

internal fun SubscriptionCardProjectorRegistry.projectorsForTest() =
    javaClass.getDeclaredField("projectors").apply { isAccessible = true }.get(this) as Map<*, *>

internal fun ProviderQuotaDetailProjectorRegistry.projectorsForTest() =
    javaClass.getDeclaredField("projectors").apply { isAccessible = true }.get(this) as Map<*, *>
