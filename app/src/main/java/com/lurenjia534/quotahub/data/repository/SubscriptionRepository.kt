package com.lurenjia534.quotahub.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.lurenjia534.quotahub.data.local.QuotaDatabase
import com.lurenjia534.quotahub.data.local.QuotaSnapshotDao
import com.lurenjia534.quotahub.data.local.SubscriptionDao
import com.lurenjia534.quotahub.data.local.SubscriptionEntity
import com.lurenjia534.quotahub.data.local.toEntities
import com.lurenjia534.quotahub.data.local.toQuotaSnapshot
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.model.SubscriptionSyncStatus
import com.lurenjia534.quotahub.data.model.SyncState
import com.lurenjia534.quotahub.data.model.toSubscription
import com.lurenjia534.quotahub.data.provider.ProviderCatalog
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor
import com.lurenjia534.quotahub.data.provider.SecretBundle
import com.lurenjia534.quotahub.data.security.ApiKeyCipher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * 订阅仓库类
 *
 * 只负责本地持久化、加密凭证存取和数据库投影，不直接调用远程 provider API。
 */
class SubscriptionRepository(
    private val database: QuotaDatabase,
    private val subscriptionDao: SubscriptionDao,
    private val quotaSnapshotDao: QuotaSnapshotDao,
    private val providerCatalog: ProviderCatalog,
    private val apiKeyCipher: ApiKeyCipher
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    val subscriptions: Flow<List<Subscription>> = subscriptionDao.getAllSubscriptions().map { entities ->
        entities.mapNotNull(::toSubscription)
    }

    fun getSubscription(subscriptionId: Long): Flow<Subscription?> {
        return subscriptionDao.getSubscription(subscriptionId).map { entity ->
            entity?.let(::toSubscription)
        }
    }

    suspend fun getSubscriptionOnce(subscriptionId: Long): Subscription? {
        return subscriptionDao.getSubscriptionOnce(subscriptionId)?.let(::toSubscription)
    }

    suspend fun getSubscriptionCount(): Int {
        return subscriptionDao.getSubscriptionCount()
    }

    suspend fun createSubscription(
        provider: ProviderDescriptor,
        customTitle: String?,
        credentials: SecretBundle,
        syncStatus: SubscriptionSyncStatus = SubscriptionSyncStatus.neverSynced()
    ): Long {
        val entity = SubscriptionEntity(
            providerId = provider.id,
            customTitle = customTitle?.trim()?.takeIf { it.isNotBlank() },
            apiKey = apiKeyCipher.encrypt(serializeCredentials(credentials)),
            syncState = syncStatus.state.toPersistedState().name,
            lastSuccessAt = syncStatus.lastSuccessAt,
            lastFailureAt = syncStatus.lastFailureAt,
            lastError = syncStatus.lastError
        )
        return subscriptionDao.insertSubscription(entity)
    }

    suspend fun updateSubscription(subscription: Subscription) {
        val entity = SubscriptionEntity(
            id = subscription.id,
            providerId = subscription.provider.id,
            customTitle = subscription.customTitle?.trim()?.takeIf { it.isNotBlank() },
            apiKey = apiKeyCipher.encrypt(serializeCredentials(subscription.credentials)),
            syncState = subscription.syncStatus.state.toPersistedState().name,
            lastSuccessAt = subscription.syncStatus.lastSuccessAt,
            lastFailureAt = subscription.syncStatus.lastFailureAt,
            lastError = subscription.syncStatus.lastError,
            createdAt = subscription.createdAt
        )
        subscriptionDao.updateSubscription(entity)
    }

    suspend fun updateSubscriptionTitle(subscriptionId: Long, customTitle: String?): Result<Unit> {
        return runCatching {
            val currentSubscription = subscriptionDao.getSubscriptionOnce(subscriptionId)
                ?: throw IllegalStateException("Subscription no longer exists")
            subscriptionDao.updateSubscription(
                currentSubscription.copy(
                    customTitle = customTitle?.trim()?.takeIf { it.isNotBlank() }
                )
            )
        }
    }

    suspend fun deleteSubscription(subscriptionId: Long) {
        subscriptionDao.deleteSubscriptionById(subscriptionId)
    }

    suspend fun markSubscriptionSyncing(subscriptionId: Long) {
        updateSyncStatus(subscriptionId) { current ->
            SubscriptionSyncStatus.syncing(current)
        }
    }

    suspend fun markSubscriptionSyncSuccess(subscriptionId: Long, fetchedAt: Long) {
        updateSyncStatus(subscriptionId) { current ->
            SubscriptionSyncStatus.active(
                fetchedAt = fetchedAt,
                previous = current
            )
        }
    }

    suspend fun markSubscriptionSyncFailure(subscriptionId: Long, error: Throwable) {
        updateSyncStatus(subscriptionId) { current ->
            SubscriptionSyncStatus.failed(
                state = error.toSyncFailureState(),
                failedAt = System.currentTimeMillis(),
                errorMessage = error.message,
                previous = current
            )
        }
    }

    fun getQuotaSnapshot(subscriptionId: Long): Flow<QuotaSnapshot> {
        return quotaSnapshotDao.observeQuotaSnapshotRows(subscriptionId).map { rows ->
            rows.toQuotaSnapshot()
        }
    }

    suspend fun cacheQuotaSnapshot(
        subscriptionId: Long,
        snapshot: QuotaSnapshot
    ) {
        ensureSubscriptionExists(subscriptionId)
        val entities = snapshot.toEntities(subscriptionId)

        database.withTransaction {
            quotaSnapshotDao.clearQuotaSnapshot(subscriptionId)
            ensureSubscriptionExists(subscriptionId)
            quotaSnapshotDao.upsertQuotaSnapshot(entities.snapshot)
            if (entities.resources.isNotEmpty()) {
                quotaSnapshotDao.upsertQuotaResources(entities.resources)
            }
            if (entities.windows.isNotEmpty()) {
                quotaSnapshotDao.upsertQuotaWindows(entities.windows)
            }
        }
    }

    private suspend fun ensureSubscriptionExists(subscriptionId: Long) {
        if (subscriptionDao.getSubscriptionOnce(subscriptionId) == null) {
            throw IllegalStateException("Subscription no longer exists")
        }
    }

    private fun toSubscription(entity: SubscriptionEntity): Subscription? {
        val descriptor = providerCatalog.descriptor(entity.providerId) ?: return null
        return runCatching {
            entity.toSubscription(
                provider = descriptor,
                credentials = deserializeCredentials(entity.apiKey, descriptor),
                syncStatus = entity.toSyncStatus()
            )
        }.getOrElse { error ->
            Log.e(TAG, "Failed to decrypt credentials for subscription ${entity.id}", error)
            null
        }
    }

    private fun serializeCredentials(credentials: SecretBundle): String {
        return json.encodeToString(SecretBundle.serializer(), credentials)
    }

    private fun deserializeCredentials(
        encryptedPayload: String,
        provider: ProviderDescriptor
    ): SecretBundle {
        val decrypted = apiKeyCipher.decrypt(encryptedPayload)
        return runCatching {
            json.decodeFromString(SecretBundle.serializer(), decrypted)
        }.getOrElse {
            SecretBundle.single(provider.primaryCredentialField.key, decrypted)
        }
    }

    private suspend fun updateSyncStatus(
        subscriptionId: Long,
        transform: (SubscriptionSyncStatus) -> SubscriptionSyncStatus
    ) {
        val currentSubscription = subscriptionDao.getSubscriptionOnce(subscriptionId)
            ?: throw IllegalStateException("Subscription no longer exists")
        val updatedStatus = transform(currentSubscription.toSyncStatus())
        subscriptionDao.updateSubscription(
            currentSubscription.copy(
                syncState = updatedStatus.state.toPersistedState().name,
                lastSuccessAt = updatedStatus.lastSuccessAt,
                lastFailureAt = updatedStatus.lastFailureAt,
                lastError = updatedStatus.lastError
            )
        )
    }

    private companion object {
        private const val TAG = "SubscriptionRepository"
    }
}

private fun SubscriptionEntity.toSyncStatus(now: Long = System.currentTimeMillis()): SubscriptionSyncStatus {
    val persistedState = runCatching { SyncState.valueOf(syncState) }
        .getOrDefault(SyncState.NeverSynced)
    val effectiveState = when {
        persistedState == SyncState.Active &&
            lastSuccessAt != null &&
            now - lastSuccessAt > StaleAfterMillis -> SyncState.Stale
        persistedState == SyncState.SyncError &&
            lastSuccessAt != null &&
            now - lastSuccessAt > StaleAfterMillis -> SyncState.Stale
        else -> persistedState
    }

    return SubscriptionSyncStatus(
        state = effectiveState,
        lastSuccessAt = lastSuccessAt,
        lastFailureAt = lastFailureAt,
        lastError = lastError
    )
}

private fun SyncState.toPersistedState(): SyncState {
    return if (this == SyncState.Stale) SyncState.Active else this
}

private fun Throwable.toSyncFailureState(): SyncState {
    return if (this is HttpException && (code() == 401 || code() == 403)) {
        SyncState.AuthFailed
    } else {
        SyncState.SyncError
    }
}

private const val StaleAfterMillis = 24 * 60 * 60 * 1000L
