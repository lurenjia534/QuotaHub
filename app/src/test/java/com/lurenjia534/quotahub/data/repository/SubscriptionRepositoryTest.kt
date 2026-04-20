package com.lurenjia534.quotahub.data.repository

import com.lurenjia534.quotahub.data.local.QuotaDatabase
import com.lurenjia534.quotahub.data.local.QuotaResourceEntity
import com.lurenjia534.quotahub.data.local.QuotaSnapshotDao
import com.lurenjia534.quotahub.data.local.QuotaSnapshotEntity
import com.lurenjia534.quotahub.data.local.QuotaSnapshotRow
import com.lurenjia534.quotahub.data.local.QuotaUpgradeStateDao
import com.lurenjia534.quotahub.data.local.SubscriptionDao
import com.lurenjia534.quotahub.data.local.SubscriptionEntity
import com.lurenjia534.quotahub.data.local.QuotaWindowEntity
import com.lurenjia534.quotahub.data.model.CredentialState
import com.lurenjia534.quotahub.data.model.CredentialUnavailableException
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.model.SyncState
import com.lurenjia534.quotahub.data.provider.CapturedQuotaSnapshot
import com.lurenjia534.quotahub.data.provider.CodingPlanProvider
import com.lurenjia534.quotahub.data.provider.CredentialFieldSpec
import com.lurenjia534.quotahub.data.provider.ProviderCatalog
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor
import com.lurenjia534.quotahub.data.provider.ProviderFailure
import com.lurenjia534.quotahub.data.provider.ProviderReplayPayload
import com.lurenjia534.quotahub.data.provider.ProviderSyncException
import com.lurenjia534.quotahub.data.provider.SecretBundle
import com.lurenjia534.quotahub.data.security.ApiKeyCipher
import com.lurenjia534.quotahub.data.security.EncryptedCredentialVault
import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteOpenHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionRepositoryTest {
    @Test
    fun readCredentials_loadsSecretFromVaultWithoutExposingItOnSubscription() = runBlocking {
        val subscriptionDao = FakeSubscriptionDao(initialEntities = emptyList())
        val repository = SubscriptionRepository(
            database = FakeQuotaDatabase(subscriptionDao),
            subscriptionDao = subscriptionDao,
            quotaSnapshotDao = NoOpQuotaSnapshotDao(),
            providerCatalog = providerCatalog(),
            credentialVault = EncryptedCredentialVault(PassthroughCipher())
        )

        val subscriptionId = repository.createSubscription(
            provider = providerCatalog().descriptors.single(),
            customTitle = null,
            credentials = SecretBundle.single("apiKey", "live-secret")
        )

        val subscription = repository.getSubscriptionOnce(subscriptionId)
        val credentials = repository.readCredentials(subscriptionId).getOrThrow()

        assertEquals(CredentialState.Available, subscription?.credentialState)
        assertEquals("live-secret", credentials.requireValue("apiKey"))
    }

    @Test
    fun readCredentials_allowsOptionalFieldsToRemainMissing() = runBlocking {
        val subscriptionDao = FakeSubscriptionDao(initialEntities = emptyList())
        val catalog = providerCatalogWithOptionalField()
        val repository = SubscriptionRepository(
            database = FakeQuotaDatabase(subscriptionDao),
            subscriptionDao = subscriptionDao,
            quotaSnapshotDao = NoOpQuotaSnapshotDao(),
            providerCatalog = catalog,
            credentialVault = EncryptedCredentialVault(PassthroughCipher())
        )

        val subscriptionId = repository.createSubscription(
            provider = catalog.descriptors.single(),
            customTitle = null,
            credentials = SecretBundle.single("apiKey", "live-secret")
        )

        val subscription = repository.getSubscriptionOnce(subscriptionId)
        val credentials = repository.readCredentials(subscriptionId).getOrThrow()

        assertEquals(CredentialState.Available, subscription?.credentialState)
        assertEquals("live-secret", credentials.requireValue("apiKey"))
        assertEquals(null, credentials.value("accountId"))
    }

    @Test
    fun subscriptions_keepBrokenCredentialEntriesVisible() = runBlocking {
        val subscriptionDao = FakeSubscriptionDao(
            initialEntities = listOf(
                SubscriptionEntity(
                    id = 7L,
                    providerId = TEST_PROVIDER_ID,
                    customTitle = "Broken source",
                    apiKey = "encrypted-payload",
                    syncState = SyncState.Active.name
                )
            )
        )
        val repository = SubscriptionRepository(
            database = FakeQuotaDatabase(subscriptionDao),
            subscriptionDao = subscriptionDao,
            quotaSnapshotDao = NoOpQuotaSnapshotDao(),
            providerCatalog = providerCatalog(),
            credentialVault = EncryptedCredentialVault(ThrowingCipher())
        )

        val subscription = repository.subscriptions.first().single()

        assertTrue(subscription.credentialState is CredentialState.Broken)
        assertEquals(SyncState.AuthFailed, subscription.syncStatus.state)
        assertEquals("keystore unavailable", subscription.credentialIssue)
        assertEquals("keystore unavailable", subscription.syncStatus.lastError)
    }

    @Test
    fun getSubscriptionForRefresh_failsWhenCredentialsCannotBeRead() = runBlocking {
        val subscriptionDao = FakeSubscriptionDao(
            initialEntities = listOf(
                SubscriptionEntity(
                    id = 9L,
                    providerId = TEST_PROVIDER_ID,
                    customTitle = null,
                    apiKey = "encrypted-payload",
                    syncState = SyncState.NeverSynced.name
                )
            )
        )
        val repository = SubscriptionRepository(
            database = FakeQuotaDatabase(subscriptionDao),
            subscriptionDao = subscriptionDao,
            quotaSnapshotDao = NoOpQuotaSnapshotDao(),
            providerCatalog = providerCatalog(),
            credentialVault = EncryptedCredentialVault(ThrowingCipher())
        )

        val result = repository.getSubscriptionForRefresh(9L)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CredentialUnavailableException)
        assertEquals("keystore unavailable", result.exceptionOrNull()?.message)
    }

    @Test
    fun subscriptions_recoverTimedOutSyncingToSyncError() = runBlocking {
        val subscriptionDao = FakeSubscriptionDao(
            initialEntities = listOf(
                SubscriptionEntity(
                    id = 10L,
                    providerId = TEST_PROVIDER_ID,
                    customTitle = "Interrupted source",
                    apiKey = "encrypted-payload",
                    syncState = SyncState.Syncing.name,
                    syncStartedAt = 1L
                )
            )
        )
        val repository = SubscriptionRepository(
            database = FakeQuotaDatabase(subscriptionDao),
            subscriptionDao = subscriptionDao,
            quotaSnapshotDao = NoOpQuotaSnapshotDao(),
            providerCatalog = providerCatalog(),
            credentialVault = EncryptedCredentialVault(PassthroughCipher())
        )

        val subscription = repository.subscriptions.first().single()

        assertEquals(SyncState.SyncError, subscription.syncStatus.state)
        assertEquals("Previous sync was interrupted before completion.", subscription.syncStatus.lastError)
        assertTrue(subscription.syncStatus.lastFailureAt != null)
        assertEquals(null, subscription.syncStatus.syncStartedAt)
    }

    @Test
    fun subscriptions_recoverTimedOutSyncingToStaleWhenLastSuccessIsOld() = runBlocking {
        val subscriptionDao = FakeSubscriptionDao(
            initialEntities = listOf(
                SubscriptionEntity(
                    id = 11L,
                    providerId = TEST_PROVIDER_ID,
                    customTitle = "Interrupted source",
                    apiKey = "encrypted-payload",
                    syncState = SyncState.Syncing.name,
                    lastSuccessAt = 1L,
                    syncStartedAt = 1L
                )
            )
        )
        val repository = SubscriptionRepository(
            database = FakeQuotaDatabase(subscriptionDao),
            subscriptionDao = subscriptionDao,
            quotaSnapshotDao = NoOpQuotaSnapshotDao(),
            providerCatalog = providerCatalog(),
            credentialVault = EncryptedCredentialVault(PassthroughCipher())
        )

        val subscription = repository.subscriptions.first().single()

        assertEquals(SyncState.Stale, subscription.syncStatus.state)
        assertEquals("Previous sync was interrupted before completion.", subscription.syncStatus.lastError)
        assertEquals(null, subscription.syncStatus.syncStartedAt)
    }

    @Test
    fun markSubscriptionSyncFailure_mapsProviderAuthFailureToAuthFailed() = runBlocking {
        val subscriptionDao = FakeSubscriptionDao(initialEntities = emptyList())
        val repository = SubscriptionRepository(
            database = FakeQuotaDatabase(subscriptionDao),
            subscriptionDao = subscriptionDao,
            quotaSnapshotDao = NoOpQuotaSnapshotDao(),
            providerCatalog = providerCatalog(),
            credentialVault = EncryptedCredentialVault(PassthroughCipher())
        )

        val subscriptionId = repository.createSubscription(
            provider = providerCatalog().descriptors.single(),
            customTitle = null,
            credentials = SecretBundle.single("apiKey", "live-secret")
        )

        repository.markSubscriptionSyncFailure(
            subscriptionId = subscriptionId,
            error = ProviderSyncException(
                ProviderFailure.Auth("token expired")
            )
        )

        val subscription = repository.getSubscriptionOnce(subscriptionId)

        assertEquals(SyncState.AuthFailed, subscription?.syncStatus?.state)
        assertEquals("token expired", subscription?.syncStatus?.lastError)
    }

    @Test
    fun markSubscriptionSyncFailure_mapsProviderRateLimitFailureToSyncError() = runBlocking {
        val subscriptionDao = FakeSubscriptionDao(initialEntities = emptyList())
        val repository = SubscriptionRepository(
            database = FakeQuotaDatabase(subscriptionDao),
            subscriptionDao = subscriptionDao,
            quotaSnapshotDao = NoOpQuotaSnapshotDao(),
            providerCatalog = providerCatalog(),
            credentialVault = EncryptedCredentialVault(PassthroughCipher())
        )

        val subscriptionId = repository.createSubscription(
            provider = providerCatalog().descriptors.single(),
            customTitle = null,
            credentials = SecretBundle.single("apiKey", "live-secret")
        )

        repository.markSubscriptionSyncFailure(
            subscriptionId = subscriptionId,
            error = ProviderSyncException(
                ProviderFailure.RateLimited(
                    retryAfterMillis = 30_000L,
                    userMessage = "rate limited"
                )
            )
        )

        val subscription = repository.getSubscriptionOnce(subscriptionId)

        assertEquals(SyncState.SyncError, subscription?.syncStatus?.state)
        assertEquals("rate limited", subscription?.syncStatus?.lastError)
    }

    private fun providerCatalog(): ProviderCatalog {
        return ProviderCatalog(
            providers = listOf(
                object : CodingPlanProvider {
                    override val descriptor: ProviderDescriptor = ProviderDescriptor(
                        id = TEST_PROVIDER_ID,
                        displayName = "Test Provider",
                        credentialFields = listOf(
                            CredentialFieldSpec(
                                key = "apiKey",
                                label = "API Key"
                            )
                        )
                    )

                    override suspend fun validate(credentials: SecretBundle): Result<CapturedQuotaSnapshot> {
                        return Result.failure(NotImplementedError())
                    }

                    override suspend fun fetchSnapshot(
                        subscription: Subscription,
                        credentials: SecretBundle
                    ): Result<CapturedQuotaSnapshot> {
                        return Result.failure(NotImplementedError())
                    }

                    override fun replay(payload: ProviderReplayPayload): Result<CapturedQuotaSnapshot> {
                        return Result.failure(NotImplementedError())
                    }
                }
            )
        )
    }

    private fun providerCatalogWithOptionalField(): ProviderCatalog {
        return ProviderCatalog(
            providers = listOf(
                object : CodingPlanProvider {
                    override val descriptor: ProviderDescriptor = ProviderDescriptor(
                        id = TEST_PROVIDER_ID,
                        displayName = "Test Provider",
                        credentialFields = listOf(
                            CredentialFieldSpec(
                                key = "apiKey",
                                label = "API Key"
                            ),
                            CredentialFieldSpec(
                                key = "accountId",
                                label = "Account ID",
                                isSecret = false,
                                isRequired = false
                            )
                        )
                    )

                    override suspend fun validate(credentials: SecretBundle): Result<CapturedQuotaSnapshot> {
                        return Result.failure(NotImplementedError())
                    }

                    override suspend fun fetchSnapshot(
                        subscription: Subscription,
                        credentials: SecretBundle
                    ): Result<CapturedQuotaSnapshot> {
                        return Result.failure(NotImplementedError())
                    }

                    override fun replay(payload: ProviderReplayPayload): Result<CapturedQuotaSnapshot> {
                        return Result.failure(NotImplementedError())
                    }
                }
            )
        )
    }

    private class ThrowingCipher : ApiKeyCipher {
        override fun encrypt(plainText: String): String = plainText

        override fun decrypt(storedValue: String): String {
            throw IllegalStateException("keystore unavailable")
        }
    }

    private class PassthroughCipher : ApiKeyCipher {
        override fun encrypt(plainText: String): String = plainText

        override fun decrypt(storedValue: String): String = storedValue
    }

    private class FakeSubscriptionDao(
        initialEntities: List<SubscriptionEntity>
    ) : SubscriptionDao {
        private val entities = MutableStateFlow(initialEntities)

        override fun getAllSubscriptions(): Flow<List<SubscriptionEntity>> = entities

        override suspend fun getAllSubscriptionsOnce(): List<SubscriptionEntity> = entities.value

        override fun getSubscription(subscriptionId: Long): Flow<SubscriptionEntity?> {
            return entities.map { subscriptions ->
                subscriptions.firstOrNull { it.id == subscriptionId }
            }
        }

        override suspend fun getSubscriptionOnce(subscriptionId: Long): SubscriptionEntity? {
            return entities.value.firstOrNull { it.id == subscriptionId }
        }

        override fun getSubscriptionsByProvider(providerId: String): Flow<List<SubscriptionEntity>> {
            return entities.map { subscriptions ->
                subscriptions.filter { it.providerId == providerId }
            }
        }

        override suspend fun getSubscriptionCount(): Int = entities.value.size

        override suspend fun insertSubscription(subscription: SubscriptionEntity): Long {
            val nextId = subscription.id.takeIf { it != 0L }
                ?: ((entities.value.maxOfOrNull { it.id } ?: 0L) + 1L)
            entities.value = listOf(subscription.copy(id = nextId)) + entities.value
            return nextId
        }

        override suspend fun updateSubscription(subscription: SubscriptionEntity) {
            entities.value = entities.value.map { current ->
                if (current.id == subscription.id) subscription else current
            }
        }

        override suspend fun deleteSubscription(subscription: SubscriptionEntity) {
            deleteSubscriptionById(subscription.id)
        }

        override suspend fun deleteSubscriptionById(subscriptionId: Long): Int {
            val before = entities.value.size
            entities.value = entities.value.filterNot { it.id == subscriptionId }
            return before - entities.value.size
        }

        override suspend fun deleteAllSubscriptions(): Int {
            val removed = entities.value.size
            entities.value = emptyList()
            return removed
        }
    }

    private class NoOpQuotaSnapshotDao : QuotaSnapshotDao {
        override fun observeQuotaSnapshotRows(subscriptionId: Long): Flow<List<QuotaSnapshotRow>> {
            return MutableStateFlow(emptyList())
        }

        override suspend fun getQuotaSnapshotMetadata(subscriptionId: Long): QuotaSnapshotEntity? = null

        override suspend fun getAllQuotaSnapshotMetadata(): List<QuotaSnapshotEntity> = emptyList()

        override suspend fun upsertQuotaSnapshot(snapshot: QuotaSnapshotEntity) = Unit

        override suspend fun upsertQuotaResources(resources: List<QuotaResourceEntity>) = Unit

        override suspend fun upsertQuotaWindows(windows: List<QuotaWindowEntity>) = Unit

        override suspend fun clearQuotaSnapshot(subscriptionId: Long): Int = 0
    }

    private class FakeQuotaDatabase(
        private val subscriptionDao: SubscriptionDao
    ) : QuotaDatabase() {
        override fun subscriptionDao(): SubscriptionDao = subscriptionDao

        override fun quotaSnapshotDao(): QuotaSnapshotDao = NoOpQuotaSnapshotDao()

        override fun quotaUpgradeStateDao(): QuotaUpgradeStateDao {
            throw NotImplementedError("unused in repository tests")
        }

        override fun clearAllTables() = Unit

        override fun createInvalidationTracker(): InvalidationTracker {
            return InvalidationTracker(
                this,
                mapOf(),
                mapOf(),
                "subscription",
                "quota_snapshot",
                "quota_resource",
                "quota_window",
                "quota_upgrade_state"
            )
        }

        @Deprecated("RoomDatabase legacy test hook")
        override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper {
            throw NotImplementedError("unused in repository tests")
        }
    }

    private companion object {
        const val TEST_PROVIDER_ID = "test-provider"
    }
}
