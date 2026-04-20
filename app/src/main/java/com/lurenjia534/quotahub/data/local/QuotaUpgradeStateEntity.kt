package com.lurenjia534.quotahub.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quota_upgrade_state")
data class QuotaUpgradeStateEntity(
    @PrimaryKey
    val providerId: String,
    val pendingReplay: Boolean = false,
    val lastAppliedReplayFingerprint: String? = null
)
