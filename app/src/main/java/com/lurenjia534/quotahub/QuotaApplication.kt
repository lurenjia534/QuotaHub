package com.lurenjia534.quotahub

import android.app.Application
import com.lurenjia534.quotahub.data.local.QuotaDatabase
import com.lurenjia534.quotahub.data.repository.QuotaRepository

class QuotaApplication : Application() {
    val database: QuotaDatabase by lazy {
        QuotaDatabase.getDatabase(this)
    }
    val repository: QuotaRepository by lazy {
        QuotaRepository(
            apiKeyDao = database.apiKeyDao(),
            modelRemainDao = database.modelRemainDao()
        )
    }
}
