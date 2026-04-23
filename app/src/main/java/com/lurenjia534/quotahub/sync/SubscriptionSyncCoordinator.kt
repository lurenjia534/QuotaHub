package com.lurenjia534.quotahub.sync

import com.lurenjia534.quotahub.data.model.SyncCause
import com.lurenjia534.quotahub.data.provider.ProviderCatalog
import com.lurenjia534.quotahub.data.provider.SecretBundle
import com.lurenjia534.quotahub.data.repository.SubscriptionRepository
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface SubscriptionSyncCoordinator {
    suspend fun refresh(
        subscriptionId: Long,
        cause: SyncCause = SyncCause.ManualRefresh
    ): Result<Unit>

    suspend fun reauthenticate(subscriptionId: Long, credentials: SecretBundle): Result<Unit>
}

class DefaultSubscriptionSyncCoordinator(
    private val repository: SubscriptionRepository,
    private val providerCatalog: ProviderCatalog
) : SubscriptionSyncCoordinator {
    private val subscriptionMutexes = ConcurrentHashMap<Long, Mutex>()

    override suspend fun refresh(
        subscriptionId: Long,
        cause: SyncCause
    ): Result<Unit> {
        return mutexFor(subscriptionId).withLock {
            repository.markSubscriptionSyncing(subscriptionId, cause)
            val currentSubscription = repository.getSubscriptionForRefresh(subscriptionId)
                .getOrElse { error ->
                    repository.markSubscriptionSyncFailure(subscriptionId, error)
                    return@withLock Result.failure(error)
                }
            val provider = providerCatalog.provider(currentSubscription.provider.id)
                ?: return@withLock fail(
                    subscriptionId,
                    IllegalStateException("Unsupported provider: ${currentSubscription.provider.id}")
                )
            val credentials = repository.readCredentials(subscriptionId).getOrElse { error ->
                repository.markSubscriptionSyncFailure(subscriptionId, error)
                return@withLock Result.failure(error)
            }

            provider.fetchSnapshot(currentSubscription, credentials).fold(
                onSuccess = { capturedSnapshot ->
                    runCatching {
                        repository.cacheQuotaSnapshot(subscriptionId, capturedSnapshot)
                        repository.markSubscriptionSyncSuccess(
                            subscriptionId = subscriptionId,
                            fetchedAt = capturedSnapshot.snapshot.fetchedAt
                        )
                    }.onFailure { error ->
                        repository.markSubscriptionSyncFailure(subscriptionId, error)
                    }
                },
                onFailure = { error ->
                    repository.markSubscriptionSyncFailure(subscriptionId, error)
                    Result.failure(error)
                }
            )
        }
    }

    override suspend fun reauthenticate(
        subscriptionId: Long,
        credentials: SecretBundle
    ): Result<Unit> {
        return mutexFor(subscriptionId).withLock {
            repository.markSubscriptionSyncing(subscriptionId, SyncCause.CredentialsUpdated)
            val currentSubscription = repository.getSubscriptionOnce(subscriptionId)
                ?: return@withLock fail(
                    subscriptionId,
                    IllegalStateException("Subscription no longer exists")
                )
            if (!currentSubscription.isProviderSupported) {
                return@withLock fail(
                    subscriptionId,
                    IllegalStateException(
                        currentSubscription.credentialIssue
                            ?: "Unsupported provider: ${currentSubscription.provider.id}"
                    )
                )
            }
            val provider = providerCatalog.provider(currentSubscription.provider.id)
                ?: return@withLock fail(
                    subscriptionId,
                    IllegalStateException("Unsupported provider: ${currentSubscription.provider.id}")
                )

            provider.validate(credentials).fold(
                onSuccess = { capturedSnapshot ->
                    runCatching {
                        repository.updateSubscriptionCredentials(subscriptionId, credentials)
                        repository.cacheQuotaSnapshot(subscriptionId, capturedSnapshot)
                        repository.markSubscriptionSyncSuccess(
                            subscriptionId = subscriptionId,
                            fetchedAt = capturedSnapshot.snapshot.fetchedAt
                        )
                    }.onFailure { error ->
                        repository.markSubscriptionSyncFailure(subscriptionId, error)
                    }
                },
                onFailure = { error ->
                    repository.markSubscriptionSyncFailure(subscriptionId, error)
                    Result.failure(error)
                }
            )
        }
    }

    private suspend fun fail(subscriptionId: Long, error: Throwable): Result<Unit> {
        repository.markSubscriptionSyncFailure(subscriptionId, error)
        return Result.failure(error)
    }

    private fun mutexFor(subscriptionId: Long): Mutex {
        return subscriptionMutexes.getOrPut(subscriptionId) { Mutex() }
    }
}
