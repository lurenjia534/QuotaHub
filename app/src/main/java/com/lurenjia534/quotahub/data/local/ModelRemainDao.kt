package com.lurenjia534.quotahub.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelRemainDao {
    @Query("SELECT * FROM model_remain WHERE subscriptionId = :subscriptionId ORDER BY displayOrder ASC")
    fun getModelRemainsBySubscription(subscriptionId: Long): Flow<List<ModelRemainEntity>>

    @Query("SELECT * FROM model_remain WHERE subscriptionId = :subscriptionId ORDER BY displayOrder ASC")
    suspend fun getModelRemainsBySubscriptionOnce(subscriptionId: Long): List<ModelRemainEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertModelRemains(modelRemains: List<ModelRemainEntity>): List<Long>

    @Query("DELETE FROM model_remain WHERE subscriptionId = :subscriptionId")
    suspend fun clearModelRemainsBySubscription(subscriptionId: Long): Int

    @Query("DELETE FROM model_remain")
    suspend fun clearAllModelRemains(): Int
}