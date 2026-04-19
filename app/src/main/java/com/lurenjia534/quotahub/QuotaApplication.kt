package com.lurenjia534.quotahub

import android.app.Application
import com.lurenjia534.quotahub.data.local.QuotaDatabase
import com.lurenjia534.quotahub.data.preferences.UiPreferencesRepository
import com.lurenjia534.quotahub.data.provider.SubscriptionRegistry
import com.lurenjia534.quotahub.data.repository.SubscriptionRepository
import com.lurenjia534.quotahub.data.security.AndroidKeystoreApiKeyCipher

class QuotaApplication : Application() {
    val database: QuotaDatabase by lazy {
        QuotaDatabase.getDatabase(this)
    }
    val subscriptionRepository: SubscriptionRepository by lazy {
        SubscriptionRepository(
            subscriptionDao = database.subscriptionDao(),
            modelRemainDao = database.modelRemainDao(),
            apiKeyCipher = AndroidKeystoreApiKeyCipher()
        )
    }

    val subscriptionRegistry: SubscriptionRegistry by lazy {
        SubscriptionRegistry(subscriptionRepository)
    }

    val uiPreferencesRepository: UiPreferencesRepository by lazy {
        UiPreferencesRepository(this)
    }
}
