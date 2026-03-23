package com.lurenjia534.quotahub.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ApiKeyEntity::class], version = 1, exportSchema = false)
abstract class QuotaDatabase : RoomDatabase() {
    abstract fun apiKeyDao(): ApiKeyDao

    companion object {
        @Volatile
        private var INSTANCE: QuotaDatabase? = null

        fun getDatabase(context: Context): QuotaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuotaDatabase::class.java,
                    "quota_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}