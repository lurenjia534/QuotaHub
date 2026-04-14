package com.lurenjia534.quotahub.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subscription",
    indices = [Index(value = ["providerId"])]
)
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val providerId: String,
    val customTitle: String?,
    val apiKey: String,
    val createdAt: Long = System.currentTimeMillis()
)