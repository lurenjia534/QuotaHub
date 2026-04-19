package com.lurenjia534.quotahub

import android.app.Application
import com.lurenjia534.quotahub.data.local.QuotaDatabase
import com.lurenjia534.quotahub.data.preferences.UiPreferencesRepository
import com.lurenjia534.quotahub.data.provider.ProviderCatalog
import com.lurenjia534.quotahub.data.provider.SubscriptionCardProjectorRegistry
import com.lurenjia534.quotahub.data.provider.SubscriptionRegistry
import com.lurenjia534.quotahub.data.provider.minimax.MiniMaxCodingPlanProvider
import com.lurenjia534.quotahub.data.repository.SubscriptionRepository
import com.lurenjia534.quotahub.data.security.AndroidKeystoreApiKeyCipher
import com.lurenjia534.quotahub.ui.screens.home.ProviderQuotaDetailProjectorRegistry

class QuotaApplication : Application() {
    val database: QuotaDatabase by lazy {
        QuotaDatabase.getDatabase(this)
    }

    val providerCatalog: ProviderCatalog by lazy {
        ProviderCatalog(
            providers = listOf(
                MiniMaxCodingPlanProvider()
            )
        )
    }

    val subscriptionRepository: SubscriptionRepository by lazy {
        SubscriptionRepository(
            database = database,
            subscriptionDao = database.subscriptionDao(),
            quotaSnapshotDao = database.quotaSnapshotDao(),
            providerCatalog = providerCatalog,
            apiKeyCipher = AndroidKeystoreApiKeyCipher()
        )
    }

    val subscriptionRegistry: SubscriptionRegistry by lazy {
        SubscriptionRegistry(
            repository = subscriptionRepository,
            providerCatalog = providerCatalog,
            cardProjectorRegistry = SubscriptionCardProjectorRegistry.default()
        )
    }

    val providerQuotaDetailProjectorRegistry: ProviderQuotaDetailProjectorRegistry by lazy {
        ProviderQuotaDetailProjectorRegistry.default()
    }

    val uiPreferencesRepository: UiPreferencesRepository by lazy {
        UiPreferencesRepository(this)
    }
}
