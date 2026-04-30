package com.lurenjia534.quotahub.sync

import com.lurenjia534.quotahub.data.model.SyncCause
import com.lurenjia534.quotahub.data.preferences.RefreshCadence
import com.lurenjia534.quotahub.data.preferences.UiPreferences
import com.lurenjia534.quotahub.data.repository.SubscriptionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SubscriptionAutoRefreshScheduler(
    private val scope: CoroutineScope,
    private val preferences: StateFlow<UiPreferences>,
    private val repository: SubscriptionRepository,
    private val syncCoordinator: SubscriptionSyncCoordinator,
    private val refreshPolicy: SubscriptionRefreshPolicy
) {
    private var job: Job? = null

    fun start() {
        if (job != null) {
            return
        }

        job = scope.launch {
            preferences
                .map { it.refreshCadence }
                .distinctUntilChanged()
                .collectLatest { refreshCadence ->
                    val intervalMillis = refreshCadence.autoRefreshIntervalMillis
                        ?: return@collectLatest
                    while (isActive) {
                        refreshEligibleSubscriptions(refreshCadence)
                        delay(intervalMillis)
                    }
                }
        }
    }

    private suspend fun refreshEligibleSubscriptions(refreshCadence: RefreshCadence) {
        val subscriptions = repository.subscriptions.first()
        subscriptions
            .filter { subscription ->
                refreshPolicy.shouldAutoRefresh(
                    subscription = subscription,
                    refreshCadence = refreshCadence
                )
            }
            .forEach { subscription ->
                syncCoordinator.refresh(
                    subscriptionId = subscription.id,
                    cause = SyncCause.AutoRefresh
                )
            }
    }
}
