package com.lurenjia534.quotahub.data.local

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuotaDatabaseMigrationTest {
    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DB_NAME)
    }

    @Test
    fun migrateFrom13To14_preservesSubscriptionsAndAddsFailureMetadataColumns() = runBlocking {
        val helper = createVersion13Database()
        try {
            val db = helper.writableDatabase
            db.insertLegacySubscription()
            db.close()
        } finally {
            helper.close()
        }

        val migratedDatabase = Room.databaseBuilder(
            context,
            QuotaDatabase::class.java,
            TEST_DB_NAME
        )
            .addMigrations(*QuotaDatabase.ALL_MIGRATIONS)
            .build()

        try {
            val migrated = migratedDatabase.subscriptionDao().getSubscriptionOnce(1L)

            requireNotNull(migrated)
            assertEquals("minimax", migrated.providerId)
            assertEquals("Primary", migrated.customTitle)
            assertEquals("sealed-payload", migrated.apiKey)
            assertEquals("SyncError", migrated.syncState)
            assertEquals(123L, migrated.lastSuccessAt)
            assertEquals(456L, migrated.lastFailureAt)
            assertEquals("timeout", migrated.lastError)
            assertEquals(789L, migrated.createdAt)
            assertNull(migrated.lastFailureKind)
            assertNull(migrated.retryAfterUntil)
            assertNull(migrated.nextEligibleSyncAt)
            assertNull(migrated.lastSyncCause)
        } finally {
            migratedDatabase.close()
        }
    }

    private fun createVersion13Database(): SupportSQLiteOpenHelper {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(TEST_DB_NAME)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(13) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `subscription` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `providerId` TEXT NOT NULL, `customTitle` TEXT, `apiKey` TEXT NOT NULL, `syncState` TEXT NOT NULL, `lastSuccessAt` INTEGER, `lastFailureAt` INTEGER, `lastError` TEXT, `syncStartedAt` INTEGER, `createdAt` INTEGER NOT NULL)"
                        )
                        db.execSQL(
                            "CREATE INDEX IF NOT EXISTS `index_subscription_providerId` ON `subscription` (`providerId`)"
                        )
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `quota_snapshot` (`subscriptionId` INTEGER NOT NULL, `fetchedAt` INTEGER NOT NULL, `rawPayloadJson` TEXT, `rawPayloadFormat` TEXT, `normalizerVersion` INTEGER, PRIMARY KEY(`subscriptionId`), FOREIGN KEY(`subscriptionId`) REFERENCES `subscription`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                        )
                        db.execSQL(
                            "CREATE INDEX IF NOT EXISTS `index_quota_snapshot_subscriptionId` ON `quota_snapshot` (`subscriptionId`)"
                        )
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `quota_resource` (`subscriptionId` INTEGER NOT NULL, `resourceKey` TEXT NOT NULL, `title` TEXT NOT NULL, `type` TEXT NOT NULL, `role` TEXT, `bucket` TEXT, `displayOrder` INTEGER NOT NULL, PRIMARY KEY(`subscriptionId`, `resourceKey`), FOREIGN KEY(`subscriptionId`) REFERENCES `quota_snapshot`(`subscriptionId`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                        )
                        db.execSQL(
                            "CREATE INDEX IF NOT EXISTS `index_quota_resource_subscriptionId` ON `quota_resource` (`subscriptionId`)"
                        )
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `quota_window` (`subscriptionId` INTEGER NOT NULL, `resourceKey` TEXT NOT NULL, `windowKey` TEXT NOT NULL, `scope` TEXT NOT NULL, `label` TEXT, `displayOrder` INTEGER NOT NULL, `total` INTEGER, `used` INTEGER, `remaining` INTEGER, `resetAtEpochMillis` INTEGER, `startsAt` INTEGER, `endsAt` INTEGER, `unit` TEXT NOT NULL, PRIMARY KEY(`subscriptionId`, `resourceKey`, `windowKey`), FOREIGN KEY(`subscriptionId`, `resourceKey`) REFERENCES `quota_resource`(`subscriptionId`, `resourceKey`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                        )
                        db.execSQL(
                            "CREATE INDEX IF NOT EXISTS `index_quota_window_subscriptionId_resourceKey` ON `quota_window` (`subscriptionId`, `resourceKey`)"
                        )
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `quota_upgrade_state` (`providerId` TEXT NOT NULL, `pendingReplay` INTEGER NOT NULL, `lastAppliedReplayFingerprint` TEXT, PRIMARY KEY(`providerId`))"
                        )
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)"
                        )
                        db.execSQL(
                            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '$V13_IDENTITY_HASH')"
                        )
                    }

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int
                    ) = Unit
                }
            )
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(configuration)
    }

    private fun SupportSQLiteDatabase.insertLegacySubscription() {
        execSQL(
            "INSERT INTO subscription (`id`, `providerId`, `customTitle`, `apiKey`, `syncState`, `lastSuccessAt`, `lastFailureAt`, `lastError`, `syncStartedAt`, `createdAt`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(
                1L,
                "minimax",
                "Primary",
                "sealed-payload",
                "SyncError",
                123L,
                456L,
                "timeout",
                null,
                789L
            )
        )
    }

    private companion object {
        const val TEST_DB_NAME = "quota-migration-test.db"
        const val V13_IDENTITY_HASH = "b56a6b948f25887ec1869e69905643ab"

        val context
            get() = InstrumentationRegistry.getInstrumentation().targetContext
    }
}
