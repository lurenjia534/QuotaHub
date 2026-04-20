package com.lurenjia534.quotahub.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface QuotaUpgradeStateDao {
    @Query("SELECT * FROM quota_upgrade_state")
    suspend fun getStates(): List<QuotaUpgradeStateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: QuotaUpgradeStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(states: List<QuotaUpgradeStateEntity>)
}
