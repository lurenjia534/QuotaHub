package com.lurenjia534.quotahub.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lurenjia534.quotahub.QuotaApplication
import com.lurenjia534.quotahub.data.model.SyncCause
import com.lurenjia534.quotahub.data.preferences.RefreshCadence
import kotlinx.coroutines.flow.first

class BackgroundRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val application = applicationContext as? QuotaApplication ?: return Result.failure()
        application.awaitStartup()

        val preferences = application.uiPreferencesRepository.preferences.first()
        if (!preferences.backgroundRefreshEnabled) {
            return Result.success()
        }

        val subscriptions = application.subscriptionRepository.subscriptions.first()
        subscriptions
            .filter { subscription ->
                application.subscriptionRefreshPolicy.shouldAutoRefresh(
                    subscription = subscription,
                    refreshCadence = RefreshCadence.Balanced
                )
            }
            .forEach { subscription ->
                application.subscriptionSyncCoordinator.refresh(
                    subscriptionId = subscription.id,
                    cause = SyncCause.AutoRefresh
                )
            }

        return Result.success()
    }

    companion object {
        const val UniqueWorkName = "quota_background_refresh"
        const val WorkTag = "quota_background_refresh"
        const val BackgroundRefreshIntervalHours = 1L
    }
}
