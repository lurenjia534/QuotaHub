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
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.model.SyncCause
import com.lurenjia534.quotahub.data.model.SyncFailureKind
import com.lurenjia534.quotahub.data.model.SyncState
import com.lurenjia534.quotahub.data.provider.CapturedQuotaSnapshot
import com.lurenjia534.quotahub.data.provider.CodingPlanProvider
import com.lurenjia534.quotahub.data.provider.CredentialFieldSpec
import com.lurenjia534.quotahub.data.provider.ProviderCatalog
import com.lurenjia534.quotahub.data.provider.ProviderDescriptor
import com.lurenjia534.quotahub.data.provider.ProviderFailure
import com.lurenjia534.quotahub.data.provider.ProviderReplayPayload
import com.lurenjia534.quotahub.data.provider.ProviderReplaySupport
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
import org.junit.Assert.assertFalse
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
    fun subscriptions_keepUnsupportedProvidersVisible() = runBlocking {
        val subscriptionDao = FakeSubscriptionDao(
            initialEntities = listOf(
                SubscriptionEntity(
                    id = 8L,
                    providerId = "legacy-provider",
                    customTitle = "Legacy source",
                    apiKey = "encrypted-payload",
                    syncState = SyncState.Active.name
                )
            )
        )
        val repository = SubscriptionRepository(
            database = FakeQuotaDatabase(subscriptionDao),
            subscriptionDao = subscriptionDao,
            quotaSnapshotDao = NoOpQuotaSnapshotDao(),
            providerCatalog = ProviderCatalog(emptyList()),
            credentialVault = EncryptedCredentialVault(PassthroughCipher())
        )

        val subscription = repository.subscriptions.first().single()

        assertFalse(subscription.isProviderSupported)
        assertEquals("legacy-provider", subscription.provider.id)
        assertEquals("Legacy source", subscription.displayTitle)
        assertEquals(SyncState.SyncError, subscription.syncStatus.state)
        assertEquals(
            "Provider module is unavailable in the current app build.",
            subscription.syncStatus.lastError
        )
        assertEquals(
            "legacy-provider is unavailable in the current app build. Update the app or remove this subscription.",
            subscription.credentialIssue
        )
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
    fun getSubscriptionForRefresh_failsWhenProviderIsUnsupported() = runBlocking {
        val subscriptionDao = FakeSubscriptionDao(
            initialEntities = listOf(
                SubscriptionEntity(
                    id = 12L,
                    providerId = "legacy-provider",
                    customTitle = null,
                    apiKey = "encrypted-payload",
                    syncState = SyncState.Active.name
                )
            )
        )
        val repository = SubscriptionRepository(
            database = FakeQuotaDatabase(subscriptionDao),
            subscriptionDao = subscriptionDao,
            quotaSnapshotDao = NoOpQuotaSnapshotDao(),
            providerCatalog = ProviderCatalog(emptyList()),
            credentialVault = EncryptedCredentialVault(PassthroughCipher())
        )

        val result = repository.getSubscriptionForRefresh(12L)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals(
            "legacy-provider is unavailable in the current app build. Update the app or remove this subscription.",
            result.exceptionOrNull()?.message
        )
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
        repository.markSubscriptionSyncing(subscriptionId, SyncCause.ManualRefresh)

        repository.markSubscriptionSyncFailure(
            subscriptionId = subscriptionId,
            error = ProviderSyncException(
                ProviderFailure.Auth("token expired")
            )
        )

        val subscription = repository.getSubscriptionOnce(subscriptionId)

        assertEquals(SyncState.AuthFailed, subscription?.syncStatus?.state)
        assertEquals("token expired", subscription?.syncStatus?.lastError)
        assertEquals(SyncFailureKind.Auth, subscription?.syncStatus?.lastFailureKind)
        assertEquals(SyncCause.ManualRefresh, subscription?.syncStatus?.lastSyncCause)
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
        repository.markSubscriptionSyncing(subscriptionId, SyncCause.AutoRefresh)

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
        val syncStatus = requireNotNull(subscription).syncStatus

        assertEquals(SyncState.SyncError, syncStatus.state)
        assertEquals("rate limited", syncStatus.lastError)
        assertEquals(SyncFailureKind.RateLimited, syncStatus.lastFailureKind)
        assertEquals(SyncCause.AutoRefresh, syncStatus.lastSyncCause)
        assertEquals(30_000L, syncStatus.retryAfterUntil?.minus(syncStatus.lastFailureAt!!))
        assertEquals(syncStatus.retryAfterUntil, syncStatus.nextEligibleSyncAt)
    }

    @Test
    fun markSubscriptionSyncFailure_pausesAutomaticRetryForSchemaChangedFailures() = runBlocking {
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
        repository.markSubscriptionSyncing(subscriptionId, SyncCause.AutoRefresh)

        repository.markSubscriptionSyncFailure(
            subscriptionId = subscriptionId,
            error = ProviderSyncException(
                ProviderFailure.SchemaChanged("payload changed")
            )
        )

        val subscription = repository.getSubscriptionOnce(subscriptionId)

        assertEquals(SyncState.SyncError, subscription?.syncStatus?.state)
        assertEquals(SyncFailureKind.SchemaChanged, subscription?.syncStatus?.lastFailureKind)
        assertEquals(null, subscription?.syncStatus?.nextEligibleSyncAt)
    }

    @Test
    fun replayStoredQuotaSnapshotsNeedingUpgrade_recordsFailureWhenOldPayloadFormatIsUnsupported() = runBlocking {
        val quotaSnapshotDao = FakeQuotaSnapshotDao(
            initialMetadata = listOf(
                QuotaSnapshotEntity(
                    subscriptionId = 7L,
                    fetchedAt = 100L,
                    rawPayloadJson = """{"quota":1}""",
                    rawPayloadFormat = "test-provider/raw-quota@v0",
                    normalizerVersion = 2
                )
            )
        )
        val subscriptionDao = FakeSubscriptionDao(
            initialEntities = listOf(
                SubscriptionEntity(
                    id = 7L,
                    providerId = TEST_PROVIDER_ID,
                    customTitle = "Legacy replay",
                    apiKey = "encrypted-payload"
                )
            )
        )
        val repository = SubscriptionRepository(
            database = FakeQuotaDatabase(subscriptionDao, quotaSnapshotDao),
            subscriptionDao = subscriptionDao,
            quotaSnapshotDao = quotaSnapshotDao,
            providerCatalog = replayingProviderCatalog(
                currentPayloadFormat = "test-provider/raw-quota@v1",
                supportedPayloadFormats = setOf("test-provider/raw-quota@v1")
            ),
            credentialVault = EncryptedCredentialVault(PassthroughCipher())
        )

        val result = repository.replayStoredQuotaSnapshotsNeedingUpgrade()

        assertEquals(1, result.checked)
        assertEquals(0, result.replayed)
        assertEquals(0, result.skipped)
        assertEquals(1, result.failures.size)
        assertTrue(
            result.failures.single().reason.contains("Unsupported replay payload format")
        )
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

    private fun replayingProviderCatalog(
        currentPayloadFormat: String,
        supportedPayloadFormats: Set<String>
    ): ProviderCatalog {
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

                    override val replaySupport: ProviderReplaySupport = ProviderReplaySupport(
                        currentPayloadFormat = currentPayloadFormat,
                        supportedPayloadFormats = supportedPayloadFormats,
                        normalizerVersion = 2
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
                        return if (payload.payloadFormat in supportedPayloadFormats) {
                            Result.success(
                                CapturedQuotaSnapshot(
                                    snapshot = QuotaSnapshot.empty(),
                                    replayPayload = payload.copy(
                                        payloadFormat = currentPayloadFormat,
                                        normalizerVersion = 2
                                    )
                                )
                            )
                        } else {
                            Result.failure(
                                ProviderSyncException(
                                    ProviderFailure.SchemaChanged(
                                        "Unsupported replay payload format: ${payload.payloadFormat}"
                                    )
                                )
                            )
                        }
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

    private class FakeQuotaSnapshotDao(
        initialMetadata: List<QuotaSnapshotEntity> = emptyList()
    ) : QuotaSnapshotDao {
        private val metadataBySubscriptionId = initialMetadata.associateBy { it.subscriptionId }.toMutableMap()

        override fun observeQuotaSnapshotRows(subscriptionId: Long): Flow<List<QuotaSnapshotRow>> {
            return MutableStateFlow(emptyList())
        }

        override suspend fun getQuotaSnapshotMetadata(subscriptionId: Long): QuotaSnapshotEntity? {
            return metadataBySubscriptionId[subscriptionId]
        }

        override suspend fun getAllQuotaSnapshotMetadata(): List<QuotaSnapshotEntity> {
            return metadataBySubscriptionId.values.sortedBy { it.subscriptionId }
        }

        override suspend fun upsertQuotaSnapshot(snapshot: QuotaSnapshotEntity) {
            metadataBySubscriptionId[snapshot.subscriptionId] = snapshot
        }

        override suspend fun upsertQuotaResources(resources: List<QuotaResourceEntity>) = Unit

        override suspend fun upsertQuotaWindows(windows: List<QuotaWindowEntity>) = Unit

        override suspend fun clearQuotaSnapshot(subscriptionId: Long): Int {
            return if (metadataBySubscriptionId.remove(subscriptionId) != null) 1 else 0
        }
    }

    private class FakeQuotaDatabase(
        private val subscriptionDao: SubscriptionDao,
        private val quotaSnapshotDao: QuotaSnapshotDao = NoOpQuotaSnapshotDao()
    ) : QuotaDatabase() {
        override fun subscriptionDao(): SubscriptionDao = subscriptionDao

        override fun quotaSnapshotDao(): QuotaSnapshotDao = quotaSnapshotDao

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
