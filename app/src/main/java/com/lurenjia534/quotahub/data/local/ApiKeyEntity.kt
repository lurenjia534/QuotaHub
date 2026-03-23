package com.lurenjia534.quotahub.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_key")
data class ApiKeyEntity(
    @PrimaryKey
    val id: Int = 1,
    val key: String,
    val createdAt: Long = System.currentTimeMillis()
)