package com.lurenjia534.quotahub.data.provider

import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.lurenjia534.quotahub.data.local.QuotaDatabase
import com.lurenjia534.quotahub.data.local.QuotaResourceEntity
import com.lurenjia534.quotahub.data.local.QuotaSnapshotDao
import com.lurenjia534.quotahub.data.local.QuotaSnapshotEntity
import com.lurenjia534.quotahub.data.local.QuotaSnapshotRow
import com.lurenjia534.quotahub.data.local.QuotaUpgradeStateDao
import com.lurenjia534.quotahub.data.local.SubscriptionDao
import com.lurenjia534.quotahub.data.local.SubscriptionEntity
import com.lurenjia534.quotahub.data.local.QuotaWindowEntity
import com.lurenjia534.quotahub.data.repository.SubscriptionRepository
import com.lurenjia534.quotahub.data.security.EncryptedCredentialVault
import com.lurenjia534.quotahub.data.security.ApiKeyCipher
import com.lurenjia534.quotahub.sync.SubscriptionSyncCoordinator
import com.lurenjia534.quotahub.data.provider.SecretBundle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionRegistryTest {
    @Test
    fun getGatewayById_returnsReadOnlyGatewayForUnsupportedProvider() = runBlocking {
        val subscriptionDao = FakeSubscriptionDao(
            initialEntities = listOf(
                SubscriptionEntity(
                    id = 11L,
                    providerId = "legacy-provider",
                    customTitle = "Legacy subscription",
                    apiKey = "sealed"
                )
            )
        )
        val repository = SubscriptionRepository(
            database = FakeQuotaDatabase(
                subscriptionDao = subscriptionDao
            ),
            subscriptionDao = subscriptionDao,
            quotaSnapshotDao = NoOpQuotaSnapshotDao(),
            providerCatalog = ProviderCatalog(emptyList()),
            credentialVault = EncryptedCredentialVault(PassthroughCipher())
        )
        val registry = SubscriptionRegistry(
            repository = repository,
            providerCatalog = ProviderCatalog(emptyList()),
            cardProjectorRegistry = SubscriptionCardProjectorRegistry(emptyMap()),
            syncCoordinator = NoOpSubscriptionSyncCoordinator()
        )

        val gateway = registry.getGatewayById(11L)

        assertNotNull(gateway)
        requireNotNull(gateway)
        assertFalse(gateway.capabilities.canRefresh)
        assertFalse(gateway.capabilities.canUpdateCredentials)
        assertFalse(gateway.capabilities.canRename)
        assertTrue(gateway.subscription.provider is com.lurenjia534.quotahub.data.model.SubscriptionProvider.Unsupported)
    }

    private class FakeSubscriptionDao(
        initialEntities: List<SubscriptionEntity>
    ) : SubscriptionDao {
        private val entities = MutableStateFlow(initialEntities)

        override fun getAllSubscriptions(): Flow<List<SubscriptionEntity>> = entities

        override suspend fun getAllSubscriptionsOnce(): List<SubscriptionEntity> = entities.value

        override fun getSubscription(subscriptionId: Long): Flow<SubscriptionEntity?> {
            return MutableStateFlow(entities.value.firstOrNull { it.id == subscriptionId })
        }

        override suspend fun getSubscriptionOnce(subscriptionId: Long): SubscriptionEntity? {
            return entities.value.firstOrNull { it.id == subscriptionId }
        }

        override fun getSubscriptionsByProvider(providerId: String): Flow<List<SubscriptionEntity>> {
            return MutableStateFlow(entities.value.filter { it.providerId == providerId })
        }

        override suspend fun getSubscriptionCount(): Int = entities.value.size

        override suspend fun insertSubscription(subscription: SubscriptionEntity): Long {
            entities.value = listOf(subscription) + entities.value
            return subscription.id
        }

        override suspend fun updateSubscription(subscription: SubscriptionEntity) {
            entities.value = entities.value.map { current ->
                if (current.id == subscription.id) subscription else current
            }
        }

        override suspend fun deleteSubscription(subscription: SubscriptionEntity) = Unit

        override suspend fun deleteSubscriptionById(subscriptionId: Long): Int = 0

        override suspend fun deleteAllSubscriptions(): Int = 0
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
        private val subscriptionDao: SubscriptionDao,
        private val quotaSnapshotDao: QuotaSnapshotDao = NoOpQuotaSnapshotDao()
    ) : QuotaDatabase() {
        override fun subscriptionDao(): SubscriptionDao = subscriptionDao

        override fun quotaSnapshotDao(): QuotaSnapshotDao = quotaSnapshotDao

        override fun quotaUpgradeStateDao(): QuotaUpgradeStateDao {
            throw NotImplementedError("unused in subscription registry tests")
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
            throw NotImplementedError("unused in subscription registry tests")
        }
    }

    private class NoOpSubscriptionSyncCoordinator : SubscriptionSyncCoordinator {
        override suspend fun refresh(
            subscriptionId: Long,
            cause: com.lurenjia534.quotahub.data.model.SyncCause
        ): Result<Unit> = Result.success(Unit)

        override suspend fun reauthenticate(
            subscriptionId: Long,
            credentials: SecretBundle
        ): Result<Unit> = Result.success(Unit)
    }

    private class PassthroughCipher : ApiKeyCipher {
        override fun encrypt(plainText: String): String = plainText

        override fun decrypt(storedValue: String): String = storedValue
    }
}
