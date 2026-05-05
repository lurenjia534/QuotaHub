package com.lurenjia534.quotahub.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lurenjia534.quotahub.data.preferences.UiPreferences
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class BackgroundRefreshScheduler(
    context: Context,
    private val scope: CoroutineScope,
    private val preferences: StateFlow<UiPreferences>
) {
    private val workManager = WorkManager.getInstance(context.applicationContext)
    private var job: Job? = null

    fun start() {
        if (job != null) {
            return
        }

        job = scope.launch {
            preferences
                .map { it.backgroundRefreshEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    if (enabled) {
                        enqueueBackgroundRefresh()
                    } else {
                        cancelBackgroundRefresh()
                    }
                }
        }
    }

    private fun enqueueBackgroundRefresh() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<BackgroundRefreshWorker>(
            BackgroundRefreshWorker.BackgroundRefreshIntervalHours,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(
                BackgroundRefreshWorker.BackgroundRefreshIntervalHours,
                TimeUnit.HOURS
            )
            .addTag(BackgroundRefreshWorker.WorkTag)
            .build()

        workManager.enqueueUniquePeriodicWork(
            BackgroundRefreshWorker.UniqueWorkName,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun cancelBackgroundRefresh() {
        workManager.cancelUniqueWork(BackgroundRefreshWorker.UniqueWorkName)
    }
}
