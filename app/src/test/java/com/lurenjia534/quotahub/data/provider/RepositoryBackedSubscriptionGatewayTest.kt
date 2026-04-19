package com.lurenjia534.quotahub.data.provider

import com.lurenjia534.quotahub.data.model.CredentialState
import com.lurenjia534.quotahub.data.model.CredentialUnavailableException
import com.lurenjia534.quotahub.data.model.QuotaSnapshot
import com.lurenjia534.quotahub.data.model.Subscription
import com.lurenjia534.quotahub.data.model.SubscriptionSyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryBackedSubscriptionGatewayTest {
    @Test
    fun refresh_usesLatestSubscriptionLoadedFromStore() = runBlocking {
        val initialSubscription = subscription()
        val refreshedSubscription = subscription()
        val store = FakeSubscriptionGatewayStore(
            currentSubscription = initialSubscription,
            refreshedSubscription = refreshedSubscription,
            refreshedCredentials = SecretBundle.single(API_KEY_FIELD, "fresh-key")
        )
        val provider = RecordingProvider()
        val gateway = RepositoryBackedSubscriptionGateway(
            subscriptionData = initialSubscription,
            provider = provider,
            repository = store
        )

        val result = gateway.refresh()

        assertTrue(result.isSuccess)
        assertEquals(listOf("fresh-key"), provider.fetchedApiKeys)
        assertEquals(1, store.syncingCalls)
        assertEquals(1, store.cachedSnapshots.size)
    }

    @Test
    fun updateCredentials_validatesThenPersistsNewSecret() = runBlocking {
        val initialSubscription = subscription()
        val store = FakeSubscriptionGatewayStore(
            currentSubscription = initialSubscription,
            refreshedSubscription = initialSubscription
        )
        val provider = RecordingProvider()
        val gateway = RepositoryBackedSubscriptionGateway(
            subscriptionData = initialSubscription,
            provider = provider,
            repository = store
        )

        val result = gateway.updateCredentials(
            SecretBundle.single(API_KEY_FIELD, "rotated-key")
        )

        assertTrue(result.isSuccess)
        assertEquals("rotated-key", provider.validatedApiKeys.single())
        assertEquals("rotated-key", store.updatedCredentials?.requireValue(API_KEY_FIELD))
        assertEquals(1, store.successCalls)
    }

    @Test
    fun refresh_failsWhenLatestCredentialsCannotBeRead() = runBlocking {
        val initialSubscription = subscription()
        val store = FakeSubscriptionGatewayStore(
            currentSubscription = initialSubscription,
            refreshedSubscription = initialSubscription,
            credentialReadError = CredentialUnavailableException("keystore unavailable")
        )
        val provider = RecordingProvider()
        val gateway = RepositoryBackedSubscriptionGateway(
            subscriptionData = initialSubscription,
            provider = provider,
            repository = store
        )

        val result = gateway.refresh()

        assertTrue(result.isFailure)
        assertEquals("keystore unavailable", result.exceptionOrNull()?.message)
        assertTrue(provider.fetchedApiKeys.isEmpty())
        assertEquals("keystore unavailable", store.lastFailure?.message)
    }

    private fun subscription(): Subscription {
        return Subscription(
            id = 1L,
            provider = ProviderDescriptor(
                id = "test-provider",
                displayName = "Test Provider",
                credentialFields = listOf(
                    CredentialFieldSpec(
                        key = API_KEY_FIELD,
                        label = "API Key"
                    )
                )
            ),
            customTitle = null,
            credentialState = CredentialState.Available,
            syncStatus = SubscriptionSyncStatus.neverSynced(),
            createdAt = 0L
        )
    }

    private class RecordingProvider : CodingPlanProvider {
        override val descriptor: ProviderDescriptor = ProviderDescriptor(
            id = "test-provider",
            displayName = "Test Provider",
            credentialFields = listOf(
                CredentialFieldSpec(
                    key = API_KEY_FIELD,
                    label = "API Key"
                )
            )
        )

        val fetchedApiKeys = mutableListOf<String>()
        val validatedApiKeys = mutableListOf<String>()

        override suspend fun validate(credentials: SecretBundle): Result<CapturedQuotaSnapshot> {
            validatedApiKeys += credentials.requireValue(API_KEY_FIELD)
            return Result.success(CapturedQuotaSnapshot(snapshot()))
        }

        override suspend fun fetchSnapshot(
            subscription: Subscription,
            credentials: SecretBundle
        ): Result<CapturedQuotaSnapshot> {
            fetchedApiKeys += credentials.requireValue(API_KEY_FIELD)
            return Result.success(CapturedQuotaSnapshot(snapshot()))
        }

        override fun replay(payload: ProviderReplayPayload): Result<CapturedQuotaSnapshot> {
            return Result.failure(NotImplementedError())
        }

        private fun snapshot(): QuotaSnapshot {
            return QuotaSnapshot(
                fetchedAt = 5_000L,
                resources = emptyList()
            )
        }
    }

    private class FakeSubscriptionGatewayStore(
        currentSubscription: Subscription,
        private val refreshedSubscription: Subscription,
        private var refreshedCredentials: SecretBundle = SecretBundle.single(API_KEY_FIELD, "initial-key"),
        private val credentialReadError: Throwable? = null
    ) : SubscriptionGatewayStore {
        private val subscriptionFlow = MutableStateFlow<Subscription?>(currentSubscription)
        private val quotaFlow = MutableStateFlow(QuotaSnapshot.empty())

        var updatedCredentials: SecretBundle? = null
            private set
        var syncingCalls: Int = 0
            private set
        var successCalls: Int = 0
            private set
        val cachedSnapshots = mutableListOf<CapturedQuotaSnapshot>()
        var lastFailure: Throwable? = null
            private set

        override fun getSubscription(subscriptionId: Long): Flow<Subscription?> = subscriptionFlow

        override fun getQuotaSnapshot(subscriptionId: Long): Flow<QuotaSnapshot> = quotaFlow

        override suspend fun getSubscriptionForRefresh(subscriptionId: Long): Result<Subscription> {
            return Result.success(refreshedSubscription)
        }

        override suspend fun readCredentials(subscriptionId: Long): Result<SecretBundle> {
            credentialReadError?.let { return Result.failure(it) }
            return Result.success(refreshedCredentials)
        }

        override suspend fun updateSubscriptionCredentials(
            subscriptionId: Long,
            credentials: SecretBundle
        ) {
            updatedCredentials = credentials
            refreshedCredentials = credentials
            val current = subscriptionFlow.value ?: return
            subscriptionFlow.value = current.copy(
                credentialState = CredentialState.Available
            )
        }

        override suspend fun cacheQuotaSnapshot(
            subscriptionId: Long,
            capturedSnapshot: CapturedQuotaSnapshot
        ) {
            cachedSnapshots += capturedSnapshot
            quotaFlow.value = capturedSnapshot.snapshot
        }

        override suspend fun markSubscriptionSyncing(subscriptionId: Long) {
            syncingCalls += 1
        }

        override suspend fun markSubscriptionSyncSuccess(subscriptionId: Long, fetchedAt: Long) {
            successCalls += 1
        }

        override suspend fun markSubscriptionSyncFailure(subscriptionId: Long, error: Throwable) {
            lastFailure = error
        }

        override suspend fun updateSubscriptionTitle(subscriptionId: Long, customTitle: String?): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun deleteSubscription(subscriptionId: Long) = Unit
    }

    private companion object {
        const val API_KEY_FIELD = "apiKey"
    }
}
