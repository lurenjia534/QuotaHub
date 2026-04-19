package com.lurenjia534.quotahub

import android.app.Application
import android.util.Log
import com.lurenjia534.quotahub.bootstrap.provider.ProviderModules
import com.lurenjia534.quotahub.data.local.QuotaDatabase
import com.lurenjia534.quotahub.data.preferences.UiPreferencesRepository
import com.lurenjia534.quotahub.data.provider.ProviderCatalog
import com.lurenjia534.quotahub.data.provider.SubscriptionCardProjectorRegistry
import com.lurenjia534.quotahub.data.provider.SubscriptionRegistry
import com.lurenjia534.quotahub.data.repository.SubscriptionRepository
import com.lurenjia534.quotahub.data.security.AndroidKeystoreApiKeyCipher
import com.lurenjia534.quotahub.data.security.EncryptedCredentialVault
import com.lurenjia534.quotahub.data.upgrade.QuotaUpgradeCoordinator
import com.lurenjia534.quotahub.sync.DefaultSubscriptionRefreshPolicy
import com.lurenjia534.quotahub.sync.SubscriptionRefreshPolicy
import com.lurenjia534.quotahub.ui.provider.ProviderUiRegistry
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

    val providerModules by lazy {
        ProviderModules.all
    }

    val providerCatalog: ProviderCatalog by lazy {
        ProviderCatalog(
            providers = providerModules.map { it.provider }
        )
    }

    val providerUiRegistry: ProviderUiRegistry by lazy {
        ProviderUiRegistry.fromModules(providerModules)
    }

    val subscriptionRepository: SubscriptionRepository by lazy {
        SubscriptionRepository(
            database = database,
            subscriptionDao = database.subscriptionDao(),
            quotaSnapshotDao = database.quotaSnapshotDao(),
            providerCatalog = providerCatalog,
            credentialVault = EncryptedCredentialVault(AndroidKeystoreApiKeyCipher())
        )
    }

    val subscriptionRegistry: SubscriptionRegistry by lazy {
        SubscriptionRegistry(
            repository = subscriptionRepository,
            providerCatalog = providerCatalog,
            cardProjectorRegistry = SubscriptionCardProjectorRegistry.fromModules(providerModules)
        )
    }

    val quotaUpgradeCoordinator: QuotaUpgradeCoordinator by lazy {
        QuotaUpgradeCoordinator(
            replayRunner = subscriptionRepository,
            upgradeStateDao = database.quotaUpgradeStateDao(),
            providerCatalog = providerCatalog
        )
    }

    val providerQuotaDetailProjectorRegistry: ProviderQuotaDetailProjectorRegistry by lazy {
        ProviderQuotaDetailProjectorRegistry.fromModules(providerModules)
    }

    val uiPreferencesRepository: UiPreferencesRepository by lazy {
        UiPreferencesRepository(this)
    }

    val subscriptionRefreshPolicy: SubscriptionRefreshPolicy by lazy {
        DefaultSubscriptionRefreshPolicy()
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            val upgradeResult = quotaUpgradeCoordinator.runPendingUpgrades()
            val replayResult = upgradeResult.replayBatchResult
            if (upgradeResult.replayTriggered && replayResult != null) {
                Log.i(
                    TAG,
                    "Quota upgrade replay finished: checked=${replayResult.checked}, replayed=${replayResult.replayed}, skipped=${replayResult.skipped}, failures=${replayResult.failures.size}"
                )
            }
            replayResult?.failures?.forEach { failure ->
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
