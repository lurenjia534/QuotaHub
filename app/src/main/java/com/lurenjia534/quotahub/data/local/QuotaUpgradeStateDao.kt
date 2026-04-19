package com.lurenjia534.quotahub.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface QuotaUpgradeStateDao {
    @Query(
        "SELECT * FROM quota_upgrade_state WHERE singletonId = ${QuotaUpgradeStateEntity.SINGLETON_ID}"
    )
    suspend fun getState(): QuotaUpgradeStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: QuotaUpgradeStateEntity)
}
