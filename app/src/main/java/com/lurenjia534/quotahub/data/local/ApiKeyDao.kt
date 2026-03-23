package com.lurenjia534.quotahub.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiKeyDao {
    @Query("SELECT * FROM api_key WHERE id = 1")
    fun getApiKey(): Flow<ApiKeyEntity?>

    @Query("SELECT * FROM api_key WHERE id = 1")
    suspend fun getApiKeyOnce(): ApiKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiKey(apiKey: ApiKeyEntity): Long

    @Query("DELETE FROM api_key")
    suspend fun deleteApiKey(): Int
}
