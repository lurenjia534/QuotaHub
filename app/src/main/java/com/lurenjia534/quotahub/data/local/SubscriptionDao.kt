package com.lurenjia534.quotahub.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 订阅数据访问对象（DAO）
 *
 * 提供对subscription表的各种数据库操作。
 * 使用Room框架将数据库操作映射为Kotlin接口方法。
 *
 * 设计说明：
 * - 使用Flow返回类型实现响应式数据流，便于UI自动更新
 * - 提供一次性查询（suspend函数）和持续监听（Flow）两种方式
 * - 支持按提供商筛选订阅，便于实现多服务商管理
 */
@Dao
interface SubscriptionDao {
    /**
     * 获取所有订阅，按创建时间倒序排列
     * @return 所有订阅的Flow流
     */
    @Query("SELECT * FROM subscription ORDER BY createdAt DESC")
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>>

    /**
     * 监听指定订阅的变化
     * @param subscriptionId 订阅ID
     * @return 订阅实体的Flow流，订阅不存在时返回null
     */
    @Query("SELECT * FROM subscription WHERE id = :subscriptionId")
    fun getSubscription(subscriptionId: Long): Flow<SubscriptionEntity?>

    /**
     * 一次性获取订阅（不持续监听）
     * @param subscriptionId 订阅ID
     * @return 订阅实体，不存在则返回null
     */
    @Query("SELECT * FROM subscription WHERE id = :subscriptionId")
    suspend fun getSubscriptionOnce(subscriptionId: Long): SubscriptionEntity?

    /**
     * 获取指定提供商的所有订阅
     * @param providerId 提供商ID
     * @return 该提供商所有订阅的Flow流
     */
    @Query("SELECT * FROM subscription WHERE providerId = :providerId ORDER BY createdAt DESC")
    fun getSubscriptionsByProvider(providerId: String): Flow<List<SubscriptionEntity>>

    /**
     * 获取订阅总数
     * @return 订阅数量
     */
    @Query("SELECT COUNT(*) FROM subscription")
    suspend fun getSubscriptionCount(): Int

    /**
     * 插入新订阅，如果已存在则替换
     * @param subscription 要插入的订阅实体
     * @return 插入后生成的订阅ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionEntity): Long

    /**
     * 更新现有订阅
     * @param subscription 包含更新后的数据的订阅实体
     */
    @Update
    suspend fun updateSubscription(subscription: SubscriptionEntity)

    /**
     * 删除订阅
     * @param subscription 要删除的订阅实体
     */
    @Delete
    suspend fun deleteSubscription(subscription: SubscriptionEntity)

    /**
     * 根据ID删除订阅
     * @param subscriptionId 要删除的订阅ID
     * @return 删除的行数
     */
    @Query("DELETE FROM subscription WHERE id = :subscriptionId")
    suspend fun deleteSubscriptionById(subscriptionId: Long): Int

    /**
     * 删除所有订阅（用于清理数据）
     * @return 删除的行数
     */
    @Query("DELETE FROM subscription")
    suspend fun deleteAllSubscriptions(): Int
}
