package com.lurenjia534.quotahub.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscription ORDER BY createdAt DESC")
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscription WHERE id = :subscriptionId")
    fun getSubscription(subscriptionId: Long): Flow<SubscriptionEntity?>

    @Query("SELECT * FROM subscription WHERE id = :subscriptionId")
    suspend fun getSubscriptionOnce(subscriptionId: Long): SubscriptionEntity?

    @Query("SELECT * FROM subscription WHERE providerId = :providerId ORDER BY createdAt DESC")
    fun getSubscriptionsByProvider(providerId: String): Flow<List<SubscriptionEntity>>

    @Query("SELECT COUNT(*) FROM subscription")
    suspend fun getSubscriptionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionEntity): Long

    @Update
    suspend fun updateSubscription(subscription: SubscriptionEntity)

    @Delete
    suspend fun deleteSubscription(subscription: SubscriptionEntity)

    @Query("DELETE FROM subscription WHERE id = :subscriptionId")
    suspend fun deleteSubscriptionById(subscriptionId: Long): Int

    @Query("DELETE FROM subscription")
    suspend fun deleteAllSubscriptions(): Int
}