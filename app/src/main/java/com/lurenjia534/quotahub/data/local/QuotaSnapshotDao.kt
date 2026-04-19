package com.lurenjia534.quotahub.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuotaSnapshotDao {
    @Query(
        """
        SELECT
            s.subscriptionId AS snapshotSubscriptionId,
            s.fetchedAt AS fetchedAt,
            r.resourceKey AS resourceKey,
            r.title AS title,
            r.type AS resourceType,
            r.displayOrder AS resourceDisplayOrder,
            w.scope AS scope,
            w.displayOrder AS windowDisplayOrder,
            w.total AS total,
            w.used AS used,
            w.remaining AS remaining,
            w.resetsAt AS resetsAt,
            w.startsAt AS startsAt,
            w.endsAt AS endsAt,
            w.unit AS unit
        FROM quota_snapshot s
        LEFT JOIN quota_resource r
            ON r.subscriptionId = s.subscriptionId
        LEFT JOIN quota_window w
            ON w.subscriptionId = r.subscriptionId
            AND w.resourceKey = r.resourceKey
        WHERE s.subscriptionId = :subscriptionId
        ORDER BY r.displayOrder ASC, w.displayOrder ASC
        """
    )
    fun observeQuotaSnapshotRows(subscriptionId: Long): Flow<List<QuotaSnapshotRow>>

    @Query("SELECT * FROM quota_snapshot WHERE subscriptionId = :subscriptionId")
    suspend fun getQuotaSnapshotMetadata(subscriptionId: Long): QuotaSnapshotEntity?

    @Query("SELECT * FROM quota_snapshot")
    suspend fun getAllQuotaSnapshotMetadata(): List<QuotaSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuotaSnapshot(snapshot: QuotaSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuotaResources(resources: List<QuotaResourceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuotaWindows(windows: List<QuotaWindowEntity>)

    @Query("DELETE FROM quota_snapshot WHERE subscriptionId = :subscriptionId")
    suspend fun clearQuotaSnapshot(subscriptionId: Long): Int
}
