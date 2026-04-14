package com.lurenjia534.quotahub.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 模型剩余配额数据访问对象（DAO）
 *
 * 提供对model_remain表的数据库操作。
 * 每个订阅可以拥有多个模型的配额信息。
 *
 * 设计说明：
 * - 所有查询都按displayOrder排序，确保返回顺序一致
 * - 支持按订阅隔离数据，便于多订阅场景
 * - 使用upsert策略（REPLACE）实现数据的原子性更新
 */
@Dao
interface ModelRemainDao {
    /**
     * 监听指定订阅的模型配额变化
     * @param subscriptionId 订阅ID
     * @return 模型配额列表的Flow流，按显示顺序排列
     */
    @Query("SELECT * FROM model_remain WHERE subscriptionId = :subscriptionId ORDER BY displayOrder ASC")
    fun getModelRemainsBySubscription(subscriptionId: Long): Flow<List<ModelRemainEntity>>

    /**
     * 一次性获取订阅的模型配额（不持续监听）
     * @param subscriptionId 订阅ID
     * @return 模型配额列表
     */
    @Query("SELECT * FROM model_remain WHERE subscriptionId = :subscriptionId ORDER BY displayOrder ASC")
    suspend fun getModelRemainsBySubscriptionOnce(subscriptionId: Long): List<ModelRemainEntity>

    /**
     * 批量插入或更新模型配额
     *
     * 使用REPLACE策略：当主键相同时更新现有记录，否则插入新记录。
     * 这确保了数据更新的原子性。
     *
     * @param modelRemains 要插入/更新的模型配额列表
     * @return 插入后生成的ID列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertModelRemains(modelRemains: List<ModelRemainEntity>): List<Long>

    /**
     * 清除指定订阅的所有模型配额
     *
     * 通常在刷新配额前调用，以确保数据一致性。
     * @param subscriptionId 订阅ID
     * @return 删除的行数
     */
    @Query("DELETE FROM model_remain WHERE subscriptionId = :subscriptionId")
    suspend fun clearModelRemainsBySubscription(subscriptionId: Long): Int

    /**
     * 清除所有模型配额（全局清理）
     *
     * 谨慎使用，通常仅在需要重置所有数据时调用。
     * @return 删除的行数
     */
    @Query("DELETE FROM model_remain")
    suspend fun clearAllModelRemains(): Int
}