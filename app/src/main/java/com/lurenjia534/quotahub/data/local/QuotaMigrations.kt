package com.lurenjia534.quotahub.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object QuotaMigrations {
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE subscription ADD COLUMN syncState TEXT NOT NULL DEFAULT 'NeverSynced'"
            )
            db.execSQL(
                "ALTER TABLE subscription ADD COLUMN lastSuccessAt INTEGER"
            )
            db.execSQL(
                "ALTER TABLE subscription ADD COLUMN lastFailureAt INTEGER"
            )
            db.execSQL(
                "ALTER TABLE subscription ADD COLUMN lastError TEXT"
            )
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createQuotaProjectionTables(db, includeReplayColumns = false)
            db.execSQL(
                """
                INSERT INTO quota_snapshot(subscriptionId, fetchedAt)
                SELECT subscriptionId, MAX(cachedAt)
                FROM model_remain
                GROUP BY subscriptionId
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO quota_resource(subscriptionId, resourceKey, title, type, displayOrder)
                SELECT subscriptionId, modelName, modelName, 'Model', displayOrder
                FROM model_remain
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO quota_window(
                    subscriptionId,
                    resourceKey,
                    scope,
                    displayOrder,
                    total,
                    used,
                    remaining,
                    resetsAt,
                    startsAt,
                    endsAt,
                    unit
                )
                SELECT
                    subscriptionId,
                    modelName,
                    'Interval',
                    0,
                    currentIntervalTotalCount,
                    max(currentIntervalTotalCount - currentIntervalUsageCount, 0),
                    currentIntervalUsageCount,
                    remainsTime,
                    startTime,
                    endTime,
                    'Request'
                FROM model_remain
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO quota_window(
                    subscriptionId,
                    resourceKey,
                    scope,
                    displayOrder,
                    total,
                    used,
                    remaining,
                    resetsAt,
                    startsAt,
                    endsAt,
                    unit
                )
                SELECT
                    subscriptionId,
                    modelName,
                    'Weekly',
                    1,
                    currentWeeklyTotalCount,
                    max(currentWeeklyTotalCount - currentWeeklyUsageCount, 0),
                    currentWeeklyUsageCount,
                    weeklyRemainsTime,
                    weeklyStartTime,
                    weeklyEndTime,
                    'Request'
                FROM model_remain
                WHERE currentWeeklyTotalCount > 0
                """.trimIndent()
            )
            db.execSQL("DROP TABLE model_remain")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE quota_snapshot ADD COLUMN rawPayloadJson TEXT")
            db.execSQL("ALTER TABLE quota_snapshot ADD COLUMN rawPayloadFormat TEXT")
            db.execSQL("ALTER TABLE quota_snapshot ADD COLUMN normalizerVersion INTEGER")
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS quota_upgrade_state (
                    singletonId INTEGER NOT NULL,
                    pendingReplay INTEGER NOT NULL,
                    lastAppliedReplayFingerprint TEXT,
                    PRIMARY KEY(singletonId)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT OR REPLACE INTO quota_upgrade_state(
                    singletonId,
                    pendingReplay,
                    lastAppliedReplayFingerprint
                ) VALUES (1, 1, NULL)
                """.trimIndent()
            )
        }
    }

    val ALL = arrayOf(
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9
    )

    private fun createQuotaProjectionTables(
        database: SupportSQLiteDatabase,
        includeReplayColumns: Boolean
    ) {
        val quotaSnapshotDefinition = if (includeReplayColumns) {
            """
            CREATE TABLE IF NOT EXISTS quota_snapshot (
                subscriptionId INTEGER NOT NULL,
                fetchedAt INTEGER NOT NULL,
                rawPayloadJson TEXT,
                rawPayloadFormat TEXT,
                normalizerVersion INTEGER,
                PRIMARY KEY(subscriptionId),
                FOREIGN KEY(subscriptionId) REFERENCES subscription(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        } else {
            """
            CREATE TABLE IF NOT EXISTS quota_snapshot (
                subscriptionId INTEGER NOT NULL,
                fetchedAt INTEGER NOT NULL,
                PRIMARY KEY(subscriptionId),
                FOREIGN KEY(subscriptionId) REFERENCES subscription(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        }
        database.execSQL(quotaSnapshotDefinition)
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_quota_snapshot_subscriptionId
            ON quota_snapshot(subscriptionId)
            """.trimIndent()
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS quota_resource (
                subscriptionId INTEGER NOT NULL,
                resourceKey TEXT NOT NULL,
                title TEXT NOT NULL,
                type TEXT NOT NULL,
                displayOrder INTEGER NOT NULL,
                PRIMARY KEY(subscriptionId, resourceKey),
                FOREIGN KEY(subscriptionId) REFERENCES quota_snapshot(subscriptionId) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_quota_resource_subscriptionId
            ON quota_resource(subscriptionId)
            """.trimIndent()
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS quota_window (
                subscriptionId INTEGER NOT NULL,
                resourceKey TEXT NOT NULL,
                scope TEXT NOT NULL,
                displayOrder INTEGER NOT NULL,
                total INTEGER,
                used INTEGER,
                remaining INTEGER,
                resetsAt INTEGER,
                startsAt INTEGER,
                endsAt INTEGER,
                unit TEXT NOT NULL,
                PRIMARY KEY(subscriptionId, resourceKey, scope),
                FOREIGN KEY(subscriptionId, resourceKey) REFERENCES quota_resource(subscriptionId, resourceKey) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_quota_window_subscriptionId_resourceKey
            ON quota_window(subscriptionId, resourceKey)
            """.trimIndent()
        )
    }
}
