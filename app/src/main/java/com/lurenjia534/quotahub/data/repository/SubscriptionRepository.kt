package com.lurenjia534.quotahub.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.lurenjia534.quotahub.data.local.QuotaDatabase
import com.lurenjia534.quotahub.data.local.QuotaSnapshotDao
import com.lurenjia534.quotahub.data.local.QuotaSnapshotEntity
import com.lurenjia534.quotahub.data.local.SubscriptionDao
import com.lurenjia534.quotahub.data.local.SubscriptionEntity
import com.lurenjia534.quotahub.data.local.toEntities
import com.lurenjia534.quotahub.data.local.toQuotaSnapshot
import com.lurenjia534.quotahub.data.model.CredentialState
import com.lurenjia534.quotahub.data.model.CredentialUnavailableException
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.model.SubscriptionProvider
import com.lurenjia534.quotahub.data.model.SyncCause
import com.lurenjia534.quotahub.data.model.SyncFailureKind
import com.lurenjia534.quotahub.data.model.SubscriptionSyncStatus
import com.lurenjia534.quotahub.data.model.SyncState
import com.lurenjia534.quotahub.data.model.toSubscription
import com.lurenjia534.quotahub.data.provider.CapturedQuotaSnapshot
import com.lurenjia534.quotahub.data.provider.ProviderCatalog
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor
import com.lurenjia534.quotahub.data.provider.ProviderFailure
import com.lurenjia534.quotahub.data.provider.ProviderReplayPayload
import com.lurenjia534.quotahub.data.provider.SecretBundle
import com.lurenjia534.quotahub.data.provider.SubscriptionGatewayStore
import com.lurenjia534.quotahub.data.provider.toProviderFailureOrNull
import com.lurenjia534.quotahub.data.security.CredentialVault
import com.lurenjia534.quotahub.data.security.VaultCredentialState
import com.lurenjia534.quotahub.data.upgrade.QuotaSnapshotReplayRunner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException

data class QuotaSnapshotReplayFailure(
    val subscriptionId: Long,
    val providerId: String,
    val reason: String
)

data class QuotaSnapshotReplayBatchResult(
    val checked: Int,
    val replayed: Int,
    val skipped: Int,
    val failures: List<QuotaSnapshotReplayFailure>
)

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
    private val credentialVault: CredentialVault
) : QuotaSnapshotReplayRunner, SubscriptionGatewayStore {
    val subscriptions: Flow<List<Subscription>> = subscriptionDao.getAllSubscriptions().map { entities ->
        entities.map(::toSubscription)
    }

    override fun getSubscription(subscriptionId: Long): Flow<Subscription?> {
        return subscriptionDao.getSubscription(subscriptionId).map { entity ->
            entity?.let(::toSubscription)
        }
    }

    suspend fun getSubscriptionOnce(subscriptionId: Long): Subscription? {
        return subscriptionDao.getSubscriptionOnce(subscriptionId)?.let(::toSubscription)
    }

    override suspend fun getSubscriptionForRefresh(subscriptionId: Long): Result<Subscription> {
        val entity = subscriptionDao.getSubscriptionOnce(subscriptionId)
            ?: return Result.failure(IllegalStateException("Subscription no longer exists"))
        val subscription = toSubscription(entity)
        if (!subscription.isProviderSupported) {
            return Result.failure(
                IllegalStateException(
                    subscription.credentialIssue
                        ?: "Unsupported provider: ${entity.providerId}"
                )
            )
        }
        if (!subscription.hasUsableCredentials) {
            return Result.failure(
                CredentialUnavailableException(
                    subscription.credentialIssue
                        ?: "Stored credentials are unavailable and need to be entered again."
                )
            )
        }
        return Result.success(subscription)
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
            apiKey = credentialVault.seal(credentials),
            syncState = syncStatus.state.toPersistedState().name,
            lastSuccessAt = syncStatus.lastSuccessAt,
            lastFailureAt = syncStatus.lastFailureAt,
            lastFailureKind = syncStatus.lastFailureKind?.name,
            lastError = syncStatus.lastError,
            retryAfterUntil = syncStatus.retryAfterUntil,
            nextEligibleSyncAt = syncStatus.nextEligibleSyncAt,
            syncStartedAt = syncStatus.syncStartedAt,
            lastSyncCause = syncStatus.lastSyncCause?.name
        )
        return subscriptionDao.insertSubscription(entity)
    }

    override suspend fun updateSubscriptionCredentials(
        subscriptionId: Long,
        credentials: SecretBundle
    ) {
        val currentSubscription = subscriptionDao.getSubscriptionOnce(subscriptionId)
            ?: throw IllegalStateException("Subscription no longer exists")
        subscriptionDao.updateSubscription(
            currentSubscription.copy(
                apiKey = credentialVault.seal(credentials)
            )
        )
    }

    override suspend fun readCredentials(subscriptionId: Long): Result<SecretBundle> {
        val entity = subscriptionDao.getSubscriptionOnce(subscriptionId)
            ?: return Result.failure(IllegalStateException("Subscription no longer exists"))
        val provider = providerCatalog.descriptor(entity.providerId)
            ?: return Result.failure(IllegalStateException("Unsupported provider: ${entity.providerId}"))

        return when (val state = resolveVaultCredentialState(entity, provider)) {
            is VaultCredentialState.Available -> Result.success(state.credentials)
            is VaultCredentialState.Missing -> Result.failure(
                CredentialUnavailableException(state.reason)
            )
            is VaultCredentialState.Broken -> Result.failure(
                CredentialUnavailableException(state.reason)
            )
        }
    }

    override suspend fun updateSubscriptionTitle(subscriptionId: Long, customTitle: String?): Result<Unit> {
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

    override suspend fun deleteSubscription(subscriptionId: Long) {
        subscriptionDao.deleteSubscriptionById(subscriptionId)
    }

    override suspend fun markSubscriptionSyncing(subscriptionId: Long, cause: SyncCause) {
        updateSyncStatus(subscriptionId) { current ->
            SubscriptionSyncStatus.syncing(
                previous = current,
                cause = cause
            )
        }
    }

    override suspend fun markSubscriptionSyncSuccess(subscriptionId: Long, fetchedAt: Long) {
        updateSyncStatus(subscriptionId) { current ->
            SubscriptionSyncStatus.active(
                fetchedAt = maxOf(current.lastSuccessAt ?: fetchedAt, fetchedAt),
                previous = current
            )
        }
    }

    override suspend fun markSubscriptionSyncFailure(subscriptionId: Long, error: Throwable) {
        updateSyncStatus(subscriptionId) { current ->
            val failedAt = System.currentTimeMillis()
            val failure = error.toPersistedSyncFailure(
                failedAt = failedAt,
                cause = current.lastSyncCause ?: SyncCause.ManualRefresh
            )
            SubscriptionSyncStatus.failed(
                state = failure.state,
                failedAt = failedAt,
                errorMessage = failure.message,
                previous = current,
                failureKind = failure.kind,
                retryAfterUntil = failure.retryAfterUntil,
                nextEligibleSyncAt = failure.nextEligibleSyncAt
            )
        }
    }

    override fun getQuotaSnapshot(subscriptionId: Long): Flow<QuotaSnapshot> {
        return quotaSnapshotDao.observeQuotaSnapshotRows(subscriptionId).map { rows ->
            rows.toQuotaSnapshot()
        }
    }

    override suspend fun cacheQuotaSnapshot(
        subscriptionId: Long,
        capturedSnapshot: CapturedQuotaSnapshot
    ) {
        ensureSubscriptionExists(subscriptionId)
        val snapshot = capturedSnapshot.snapshot
        val replayPayload = capturedSnapshot.replayPayload
        val entities = snapshot.toEntities(
            subscriptionId = subscriptionId,
            rawPayloadJson = replayPayload?.rawPayloadJson,
            rawPayloadFormat = replayPayload?.payloadFormat,
            normalizerVersion = replayPayload?.normalizerVersion
        )

        database.withTransaction {
            val currentMetadata = quotaSnapshotDao.getQuotaSnapshotMetadata(subscriptionId)
            if (currentMetadata != null && currentMetadata.fetchedAt > snapshot.fetchedAt) {
                return@withTransaction
            }
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

    suspend fun replayStoredQuotaSnapshot(subscriptionId: Long): Result<QuotaSnapshot> {
        val subscription = getSubscriptionOnce(subscriptionId)
            ?: return Result.failure(IllegalStateException("Subscription no longer exists"))
        val provider = providerCatalog.provider(subscription.provider.id)
            ?: return Result.failure(
                IllegalStateException("Unsupported provider: ${subscription.provider.id}")
            )
        val replayPayload = quotaSnapshotDao.getQuotaSnapshotMetadata(subscriptionId)
            ?.toReplayPayload()
            ?: return Result.failure(
                IllegalStateException("No replay payload stored for subscription $subscriptionId")
            )

        return provider.replay(replayPayload).mapCatching { capturedSnapshot ->
            cacheQuotaSnapshot(
                subscriptionId = subscriptionId,
                capturedSnapshot = capturedSnapshot
            )
            capturedSnapshot.snapshot
        }
    }

    override suspend fun replayStoredQuotaSnapshotsNeedingUpgrade(): QuotaSnapshotReplayBatchResult {
        val subscriptions = subscriptionDao.getAllSubscriptionsOnce()
        val metadataBySubscriptionId = quotaSnapshotDao.getAllQuotaSnapshotMetadata()
            .associateBy { it.subscriptionId }

        var checked = 0
        var replayed = 0
        var skipped = 0
        val failures = mutableListOf<QuotaSnapshotReplayFailure>()

        for (subscriptionEntity in subscriptions) {
            val provider = providerCatalog.provider(subscriptionEntity.providerId)
            if (provider == null) {
                skipped += 1
                continue
            }

            val replayPayload = metadataBySubscriptionId[subscriptionEntity.id]?.toReplayPayload()
            if (replayPayload == null) {
                skipped += 1
                continue
            }

            checked += 1
            if (!provider.requiresReplay(replayPayload)) {
                skipped += 1
                continue
            }
            if (!provider.canReplay(replayPayload)) {
                failures += QuotaSnapshotReplayFailure(
                    subscriptionId = subscriptionEntity.id,
                    providerId = subscriptionEntity.providerId,
                    reason = provider.replayIncompatibilityReason(replayPayload)
                )
                continue
            }

            provider.replay(replayPayload).fold(
                onSuccess = { capturedSnapshot ->
                    runCatching {
                        cacheQuotaSnapshot(subscriptionEntity.id, capturedSnapshot)
                    }.onSuccess {
                        replayed += 1
                    }.onFailure { error ->
                        failures += QuotaSnapshotReplayFailure(
                            subscriptionId = subscriptionEntity.id,
                            providerId = subscriptionEntity.providerId,
                            reason = error.message ?: "Failed to persist replayed snapshot"
                        )
                    }
                },
                onFailure = { error ->
                    failures += QuotaSnapshotReplayFailure(
                        subscriptionId = subscriptionEntity.id,
                        providerId = subscriptionEntity.providerId,
                        reason = error.message ?: "Failed to replay stored snapshot"
                    )
                }
            )
        }

        return QuotaSnapshotReplayBatchResult(
            checked = checked,
            replayed = replayed,
            skipped = skipped,
            failures = failures
        )
    }

    private suspend fun ensureSubscriptionExists(subscriptionId: Long) {
        if (subscriptionDao.getSubscriptionOnce(subscriptionId) == null) {
            throw IllegalStateException("Subscription no longer exists")
        }
    }

    private fun toSubscription(entity: SubscriptionEntity): Subscription {
        val descriptor = providerCatalog.descriptor(entity.providerId)
        val provider = descriptor?.let { SubscriptionProvider.Supported(it) }
            ?: SubscriptionProvider.Unsupported(entity.providerId)
        val credentialState = descriptor?.let { resolveCredentialState(entity, it) }
            ?: CredentialState.Broken(
                reason = "Provider module is unavailable in the current app build."
            )
        return entity.toSubscription(
            provider = provider,
            credentialState = credentialState,
            syncStatus = entity.toSyncStatus().withCredentialState(
                credentialState = credentialState,
                forceAuthFailed = descriptor != null
            )
        )
    }

    private fun resolveCredentialState(
        entity: SubscriptionEntity,
        provider: ProviderDescriptor
    ): CredentialState {
        return when (val state = resolveVaultCredentialState(entity, provider)) {
            is VaultCredentialState.Available -> CredentialState.Available
            is VaultCredentialState.Missing -> CredentialState.Missing(
                reason = state.reason
            )
            is VaultCredentialState.Broken -> {
                runCatching {
                    Log.e(
                        TAG,
                        "Failed to load credentials for subscription ${entity.id}: ${state.reason}"
                    )
                }
                CredentialState.Broken(
                    reason = state.reason
                )
            }
        }
    }

    private fun resolveVaultCredentialState(
        entity: SubscriptionEntity,
        provider: ProviderDescriptor
    ): VaultCredentialState {
        return credentialVault.resolve(
            storedPayload = entity.apiKey,
            provider = provider
        )
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
                lastFailureKind = updatedStatus.lastFailureKind?.name,
                lastError = updatedStatus.lastError,
                retryAfterUntil = updatedStatus.retryAfterUntil,
                nextEligibleSyncAt = updatedStatus.nextEligibleSyncAt,
                syncStartedAt = updatedStatus.syncStartedAt,
                lastSyncCause = updatedStatus.lastSyncCause?.name
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
    val persistedFailureKind = lastFailureKind?.let { raw ->
        runCatching { SyncFailureKind.valueOf(raw) }.getOrNull()
    }
    val persistedSyncCause = lastSyncCause?.let { raw ->
        runCatching { SyncCause.valueOf(raw) }.getOrNull()
    }
    val syncingTimedOut = persistedState == SyncState.Syncing &&
        (syncStartedAt == null || now - syncStartedAt > SyncTimeoutMillis)
    val effectiveFailureAt = if (syncingTimedOut) {
        syncStartedAt ?: now
    } else {
        lastFailureAt
    }
    val effectiveError = if (syncingTimedOut) {
        lastError ?: InterruptedSyncMessage
    } else {
        lastError
    }
    val effectiveFailureKind = if (syncingTimedOut) {
        SyncFailureKind.Unknown
    } else {
        persistedFailureKind
    }
    val effectiveRetryAfterUntil = if (syncingTimedOut) {
        null
    } else {
        retryAfterUntil
    }
    val effectiveNextEligibleSyncAt = if (syncingTimedOut) {
        effectiveFailureAt?.let { failureAt ->
            failureAt + (persistedSyncCause ?: SyncCause.AutoRefresh).defaultRetryBackoffMillis()
        }
    } else {
        nextEligibleSyncAt
    }
    val effectiveState = when {
        syncingTimedOut &&
            lastSuccessAt != null &&
            now - lastSuccessAt > StaleAfterMillis -> SyncState.Stale
        syncingTimedOut -> SyncState.SyncError
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
        lastFailureAt = effectiveFailureAt,
        lastError = effectiveError,
        syncStartedAt = if (effectiveState == SyncState.Syncing) syncStartedAt else null,
        lastFailureKind = effectiveFailureKind,
        retryAfterUntil = effectiveRetryAfterUntil,
        nextEligibleSyncAt = effectiveNextEligibleSyncAt,
        lastSyncCause = persistedSyncCause
    )
}

private fun SyncState.toPersistedState(): SyncState {
    return if (this == SyncState.Stale) SyncState.Active else this
}

private fun SubscriptionSyncStatus.withCredentialState(
    credentialState: CredentialState,
    forceAuthFailed: Boolean = true
): SubscriptionSyncStatus {
    val issue = when (credentialState) {
        is CredentialState.Available -> return this
        is CredentialState.Broken -> credentialState.reason
        is CredentialState.Missing -> credentialState.reason
    }
    return copy(
        state = if (forceAuthFailed) SyncState.AuthFailed else SyncState.SyncError,
        lastError = issue ?: lastError,
        lastFailureKind = if (forceAuthFailed) SyncFailureKind.Auth else lastFailureKind,
        retryAfterUntil = null,
        nextEligibleSyncAt = null
    )
}

private fun QuotaSnapshotEntity.toReplayPayload(): ProviderReplayPayload? {
    val rawPayloadJsonValue = rawPayloadJson ?: return null
    val rawPayloadFormatValue = rawPayloadFormat ?: return null
    return ProviderReplayPayload(
        fetchedAt = fetchedAt,
        payloadFormat = rawPayloadFormatValue,
        rawPayloadJson = rawPayloadJsonValue,
        normalizerVersion = normalizerVersion ?: 1
    )
}

private data class PersistedSyncFailure(
    val state: SyncState,
    val kind: SyncFailureKind,
    val message: String?,
    val retryAfterUntil: Long? = null,
    val nextEligibleSyncAt: Long? = null
)

private fun Throwable.toPersistedSyncFailure(
    failedAt: Long,
    cause: SyncCause
): PersistedSyncFailure {
    toProviderFailureOrNull()?.let { failure ->
        return failure.toPersistedSyncFailure(
            failedAt = failedAt,
            cause = cause
        )
    }

    return if (
        this is CredentialUnavailableException ||
        (this is HttpException && (code() == 401 || code() == 403))
    ) {
        PersistedSyncFailure(
            state = SyncState.AuthFailed,
            kind = SyncFailureKind.Auth,
            message = message
        )
    } else {
        PersistedSyncFailure(
            state = SyncState.SyncError,
            kind = SyncFailureKind.Unknown,
            message = message,
            nextEligibleSyncAt = failedAt + cause.defaultRetryBackoffMillis()
        )
    }
}

private const val StaleAfterMillis = 24 * 60 * 60 * 1000L
private const val SyncTimeoutMillis = 2 * 60 * 1000L
private const val InterruptedSyncMessage = "Previous sync was interrupted before completion."
private const val ManualRetryBackoffMillis = 30 * 1000L
private const val AutoRetryBackoffMillis = 2 * 60 * 1000L

private fun ProviderFailure.toPersistedSyncFailure(
    failedAt: Long,
    cause: SyncCause
): PersistedSyncFailure {
    return when (this) {
        is ProviderFailure.Auth -> PersistedSyncFailure(
            state = SyncState.AuthFailed,
            kind = SyncFailureKind.Auth,
            message = userMessage
        )
        is ProviderFailure.RateLimited -> {
            val retryAfterUntil = retryAfterMillis?.let { duration ->
                failedAt + duration.coerceAtLeast(0L)
            }
            PersistedSyncFailure(
                state = SyncState.SyncError,
                kind = SyncFailureKind.RateLimited,
                message = userMessage,
                retryAfterUntil = retryAfterUntil,
                nextEligibleSyncAt = retryAfterUntil ?: (failedAt + cause.defaultRetryBackoffMillis())
            )
        }
        is ProviderFailure.Transient -> PersistedSyncFailure(
            state = SyncState.SyncError,
            kind = SyncFailureKind.Transient,
            message = userMessage,
            nextEligibleSyncAt = failedAt + cause.defaultRetryBackoffMillis()
        )
        is ProviderFailure.SchemaChanged -> PersistedSyncFailure(
            state = SyncState.SyncError,
            kind = SyncFailureKind.SchemaChanged,
            message = userMessage
        )
        is ProviderFailure.Validation -> PersistedSyncFailure(
            state = SyncState.SyncError,
            kind = SyncFailureKind.Validation,
            message = userMessage
        )
        is ProviderFailure.Unknown -> PersistedSyncFailure(
            state = SyncState.SyncError,
            kind = SyncFailureKind.Unknown,
            message = userMessage,
            nextEligibleSyncAt = failedAt + cause.defaultRetryBackoffMillis()
        )
    }
}

private fun SyncCause.defaultRetryBackoffMillis(): Long {
    return when (this) {
        SyncCause.ManualRefresh,
        SyncCause.CredentialsUpdated -> ManualRetryBackoffMillis
        SyncCause.AutoRefresh -> AutoRetryBackoffMillis
    }
}
