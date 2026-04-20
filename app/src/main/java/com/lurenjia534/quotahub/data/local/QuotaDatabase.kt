package com.lurenjia534.quotahub.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * QuotaHub应用的主数据库
 *
 * 使用Room框架管理本地数据持久化。
 * 数据库版本13，包含订阅、规范化配额快照、可重放的原始 provider payload，
 * 以及升级协调状态表。
 *
 * 数据库表说明：
 * - subscription: 存储用户的AI服务订阅信息
 * - quota_snapshot: 存储每个订阅的最新快照元数据
 * - quota_resource: 存储快照中的资源级投影
 * - quota_window: 存储资源下的额度窗口
 *
 * 设计说明：
 * - 采用单例模式确保数据库实例全局唯一
 * - 使用volatile关键字保证多线程安全
 * - 当前仍处于WIP阶段，旧schema升级仍允许 destructive migration
 * - quota_upgrade_state 已升级为 provider 维度的 replay ledger
 */
@Database(
    entities = [
        SubscriptionEntity::class,
        QuotaSnapshotEntity::class,
        QuotaResourceEntity::class,
        QuotaWindowEntity::class,
        QuotaUpgradeStateEntity::class
    ],
    version = 13,
    exportSchema = false
)
abstract class QuotaDatabase : RoomDatabase() {
    /**
     * 获取订阅数据访问对象
     */
    abstract fun subscriptionDao(): SubscriptionDao

    /**
     * 获取规范化配额快照数据访问对象
     */
    abstract fun quotaSnapshotDao(): QuotaSnapshotDao

    /**
     * 获取升级协调状态数据访问对象
     */
    abstract fun quotaUpgradeStateDao(): QuotaUpgradeStateDao

    companion object {
        const val CURRENT_VERSION = 13

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
