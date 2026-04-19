package com.lurenjia534.quotahub.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quota_upgrade_state")
data class QuotaUpgradeStateEntity(
    @PrimaryKey
    val singletonId: Int = SINGLETON_ID,
    val pendingReplay: Boolean = false,
    val lastAppliedReplayFingerprint: String? = null
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
