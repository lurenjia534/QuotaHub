package com.lurenjia534.quotahub.data.cloud

import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.SubscriptionSyncStatus
import com.lurenjia534.quotahub.data.model.SyncFailureKind
import com.lurenjia534.quotahub.data.model.SyncState
import com.lurenjia534.quotahub.data.repository.SubscriptionRepository

data class CloudSyncResult(
    val imported: Int,
    val skipped: Int,
    val pruned: Int,
    val message: String
)

class CloudSyncRepository(
    private val preferencesRepository: CloudSyncPreferencesRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val relayClient: QuotaHubRelayClient = QuotaHubRelayClient()
) {
    val settings = preferencesRepository.settings

    fun setEnabled(enabled: Boolean) {
        preferencesRepository.setEnabled(enabled)
    }

    fun setRelayBaseUrl(baseUrl: String) {
        preferencesRepository.setRelayBaseUrl(baseUrl)
    }

    fun setClientToken(token: String) {
        preferencesRepository.setClientToken(token)
    }

    fun clearClientToken() {
        preferencesRepository.clearClientToken()
    }

    suspend fun testConnection(): Result<String> {
        return runCatching {
            val settings = requireConfiguredSettings(requireEnabled = false)
            val providers = relayClient.listProviders(
                baseUrl = settings.relayBaseUrl,
                clientToken = requireClientToken()
            )
            "Connected to relay. ${providers.size} providers available."
        }.onSuccess { message ->
            preferencesRepository.recordSyncResult(message, syncedAt = null)
        }.onFailure { error ->
            preferencesRepository.recordSyncResult(error.cloudSyncMessage(), syncedAt = null)
        }
    }

    suspend fun syncNow(): Result<CloudSyncResult> {
        return runCatching {
            val settings = requireConfiguredSettings(requireEnabled = true)
            val subscriptions = relayClient.listSubscriptions(
                baseUrl = settings.relayBaseUrl,
                clientToken = requireClientToken()
            )
            applySubscriptionSync(subscriptions)
        }.onSuccess { result ->
            preferencesRepository.recordSyncResult(result.message)
        }.onFailure { error ->
            preferencesRepository.recordSyncResult(error.cloudSyncMessage(), syncedAt = null)
        }
    }

    suspend fun refreshRemoteSubscription(remoteSubscriptionId: String): Result<Unit> {
        return runCatching {
            val settings = requireConfiguredSettings(requireEnabled = true)
            val subscription = relayClient.refreshSubscription(
                baseUrl = settings.relayBaseUrl,
                clientToken = requireClientToken(),
                subscriptionId = remoteSubscriptionId
            )
            importSubscription(subscription)
            preferencesRepository.recordSyncResult(
                "Refreshed ${subscription.displayTitle ?: subscription.providerDisplayName ?: subscription.providerId}."
            )
        }
    }

    private suspend fun applySubscriptionSync(
        response: RelaySubscriptionsResponse
    ): CloudSyncResult {
        var imported = 0
        var skipped = 0
        response.subscriptions.forEach { subscription ->
            if (subscription.providerId.isBlank()) {
                skipped += 1
            } else {
                importSubscription(subscription)
                imported += 1
            }
        }
        val pruned = subscriptionRepository.deleteCloudSubscriptions(
            response.deletedSubscriptions.map { it.id }
        )
        val message = if (skipped == 0 && pruned == 0) {
            "Synced $imported cloud subscriptions."
        } else {
            buildList {
                add("synced $imported")
                if (pruned > 0) {
                    add("removed $pruned deleted")
                }
                if (skipped > 0) {
                    add("skipped $skipped unsupported")
                }
            }.joinToString(
                separator = "; ",
                prefix = "Cloud sync: ",
                postfix = "."
            )
        }
        return CloudSyncResult(
            imported = imported,
            skipped = skipped,
            pruned = pruned,
            message = message
        )
    }

    private suspend fun importSubscription(subscription: RelaySubscriptionDto): Long {
        return subscriptionRepository.upsertCloudSubscription(
            remoteSubscriptionId = subscription.id,
            providerId = subscription.providerId,
            displayTitle = subscription.displayTitle,
            customTitle = subscription.customTitle,
            syncStatus = subscription.toSyncStatus(),
            snapshot = subscription.snapshot?.toDomainOrNull()
        )
    }

    private fun RelaySubscriptionDto.toSyncStatus(): SubscriptionSyncStatus {
        val successAt = lastSyncedAt ?: snapshot?.fetchedAt
        return when (syncState) {
            "active" -> SubscriptionSyncStatus.active(
                fetchedAt = successAt ?: updatedAt
            )
            "auth_failed" -> SubscriptionSyncStatus(
                state = SyncState.AuthFailed,
                lastSuccessAt = successAt,
                lastFailureAt = updatedAt,
                lastError = "Relay reported an upstream authentication failure.",
                lastFailureKind = SyncFailureKind.Auth
            )
            "sync_error" -> SubscriptionSyncStatus(
                state = SyncState.SyncError,
                lastSuccessAt = successAt,
                lastFailureAt = updatedAt,
                lastError = "Relay could not refresh this subscription.",
                lastFailureKind = SyncFailureKind.Unknown
            )
            else -> SubscriptionSyncStatus.neverSynced()
        }
    }

    private fun RelayQuotaSnapshotDto.toDomainOrNull(): QuotaSnapshot? {
        return runCatching { toDomain() }.getOrNull()
    }

    private fun requireConfiguredSettings(requireEnabled: Boolean): CloudSyncSettings {
        val settings = preferencesRepository.settings.value
        if (requireEnabled && !settings.enabled) {
            throw IllegalStateException("Cloud sync is disabled.")
        }
        if (settings.relayBaseUrl.isBlank()) {
            throw IllegalStateException("Relay URL is not set.")
        }
        if (!settings.hasClientToken) {
            throw IllegalStateException("Client token is not set.")
        }
        return settings
    }

    private fun requireClientToken(): String {
        return preferencesRepository.clientToken().getOrElse { error ->
            throw IllegalStateException(
                error.message ?: "Client token could not be read."
            )
        }
    }
}

fun Throwable.cloudSyncMessage(): String {
    return when (this) {
        is RelayApiException -> userMessage
        else -> message ?: "Cloud sync failed."
    }
}
