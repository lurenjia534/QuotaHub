package com.lurenjia534.quotahub

import android.app.Application
import android.util.Log
import com.lurenjia534.quotahub.data.local.QuotaDatabase
import com.lurenjia534.quotahub.data.preferences.UiPreferencesRepository
import com.lurenjia534.quotahub.data.provider.ProviderCatalog
import com.lurenjia534.quotahub.data.provider.SubscriptionCardProjectorRegistry
import com.lurenjia534.quotahub.data.provider.SubscriptionRegistry
import com.lurenjia534.quotahub.data.provider.minimax.MiniMaxCodingPlanProvider
import com.lurenjia534.quotahub.data.repository.SubscriptionRepository
import com.lurenjia534.quotahub.data.security.AndroidKeystoreApiKeyCipher
import com.lurenjia534.quotahub.ui.screens.home.ProviderQuotaDetailProjectorRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class QuotaApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            val result = subscriptionRepository.replayStoredQuotaSnapshotsNeedingUpgrade()
            if (result.replayed > 0 || result.failures.isNotEmpty()) {
                Log.i(
                    TAG,
                    "Quota snapshot replay finished: checked=${result.checked}, replayed=${result.replayed}, skipped=${result.skipped}, failures=${result.failures.size}"
                )
            }
            result.failures.forEach { failure ->
                Log.w(
                    TAG,
                    "Replay failed for subscription=${failure.subscriptionId}, provider=${failure.providerId}: ${failure.reason}"
                )
            }
        }
    }

    private companion object {
        private const val TAG = "QuotaApplication"
    }
}
