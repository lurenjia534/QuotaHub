package com.lurenjia534.quotahub.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object QuotaDatabaseMigrations {
    val MIGRATION_12_13: Migration = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val legacyState = readLegacyUpgradeState(db)

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `quota_upgrade_state_new` (
                    `providerId` TEXT NOT NULL,
                    `pendingReplay` INTEGER NOT NULL,
                    `lastAppliedReplayFingerprint` TEXT,
                    PRIMARY KEY(`providerId`)
                )
                """.trimIndent()
            )

            legacyState?.fingerprints.orEmpty().forEach { fingerprint ->
                val providerId = fingerprint.substringBefore(':').trim()
                if (providerId.isEmpty()) {
                    return@forEach
                }
                db.execSQL(
                    """
                    INSERT INTO `quota_upgrade_state_new`
                    (`providerId`, `pendingReplay`, `lastAppliedReplayFingerprint`)
                    VALUES (?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any>(
                        providerId,
                        if (legacyState?.pendingReplay == true) 1 else 0,
                        fingerprint
                    )
                )
            }

            db.execSQL("DROP TABLE `quota_upgrade_state`")
            db.execSQL("ALTER TABLE `quota_upgrade_state_new` RENAME TO `quota_upgrade_state`")
        }
    }

    private fun readLegacyUpgradeState(
        db: SupportSQLiteDatabase
    ): LegacyUpgradeState? {
        db.query(
            """
            SELECT pendingReplay, lastAppliedReplayFingerprint
            FROM quota_upgrade_state
            LIMIT 1
            """.trimIndent()
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                return null
            }
            val fingerprintValue = cursor.getString(1)
                ?.split('|')
                ?.map(String::trim)
                ?.filter(String::isNotEmpty)
                .orEmpty()
            return LegacyUpgradeState(
                pendingReplay = cursor.getInt(0) != 0,
                fingerprints = fingerprintValue
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_12_13
    )

    private data class LegacyUpgradeState(
        val pendingReplay: Boolean,
        val fingerprints: List<String>
    )
}
