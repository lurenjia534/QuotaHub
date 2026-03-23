package com.lurenjia534.quotahub.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelRemainDao {
    @Query("SELECT * FROM model_remain ORDER BY displayOrder ASC")
    fun getModelRemains(): Flow<List<ModelRemainEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertModelRemains(modelRemains: List<ModelRemainEntity>): List<Long>

    @Query("DELETE FROM model_remain")
    suspend fun clearModelRemains(): Int
}
