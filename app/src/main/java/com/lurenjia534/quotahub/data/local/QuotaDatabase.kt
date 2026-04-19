package com.lurenjia534.quotahub.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * QuotaHub应用的主数据库
 *
 * 使用Room框架管理本地数据持久化。
 * 数据库版本5，包含了订阅和模型配额两张核心表。
 *
 * 数据库表说明：
 * - subscription: 存储用户的AI服务订阅信息
 * - model_remain: 存储各订阅的模型剩余配额
 *
 * 设计说明：
 * - 采用单例模式确保数据库实例全局唯一
 * - 使用volatile关键字保证多线程安全
 * - fallbackToDestructiveMigration策略：当版本升级时自动删除旧表
 *   （生产环境建议使用更严谨的迁移策略）
 */
@Database(
    entities = [SubscriptionEntity::class, ModelRemainEntity::class],
    version = 5,
    exportSchema = false
)
abstract class QuotaDatabase : RoomDatabase() {
    /**
     * 获取订阅数据访问对象
     */
    abstract fun subscriptionDao(): SubscriptionDao

    /**
     * 获取模型配额数据访问对象
     */
    abstract fun modelRemainDao(): ModelRemainDao

    companion object {
        /** 单例实例，使用volatile保证可见性 */
        @Volatile
        private var INSTANCE: QuotaDatabase? = null

        /**
         * 获取数据库实例（单例）
         *
         * 双重检查锁定模式，在多线程环境下保证单例安全。
         * 使用applicationContext避免内存泄漏。
         *
         * @param context 应用上下文
         * @return 数据库单例实例
         */
        fun getDatabase(context: Context): QuotaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuotaDatabase::class.java,
                    "quota_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
