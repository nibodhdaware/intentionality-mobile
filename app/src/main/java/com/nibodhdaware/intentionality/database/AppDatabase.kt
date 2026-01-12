package com.nibodhdaware.intentionality.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MonitoredApp::class, IntentionLog::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun monitoredAppDao(): MonitoredAppDao
    abstract fun intentionLogDao(): IntentionLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add appName column with default empty string
                database.execSQL("ALTER TABLE monitored_apps ADD COLUMN appName TEXT NOT NULL DEFAULT ''")
            }
        }
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add scheduling and interval columns
                database.execSQL("ALTER TABLE monitored_apps ADD COLUMN startHour INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE monitored_apps ADD COLUMN startMinute INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE monitored_apps ADD COLUMN endHour INTEGER NOT NULL DEFAULT 23")
                database.execSQL("ALTER TABLE monitored_apps ADD COLUMN endMinute INTEGER NOT NULL DEFAULT 59")
                database.execSQL("ALTER TABLE monitored_apps ADD COLUMN allDay INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE monitored_apps ADD COLUMN intervalMinutes INTEGER NOT NULL DEFAULT 5")
            }
        }
        
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create intention_logs table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS intention_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        packageName TEXT NOT NULL,
                        appName TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        dumbnessRating INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """)
            }
        }
        
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add customIntention column for per-app custom prompts (Premium feature)
                database.execSQL("ALTER TABLE monitored_apps ADD COLUMN customIntention TEXT NOT NULL DEFAULT ''")
                // Note: We keep the old time scheduling columns for backwards compatibility
                // They are no longer used but removing them would require table recreation
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}