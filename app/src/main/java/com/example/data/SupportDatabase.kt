package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.MissedCallRecord
import com.example.model.SupportRecord
import com.example.model.SupportTemplate

@Database(entities = [SupportRecord::class, SupportTemplate::class, MissedCallRecord::class], version = 7, exportSchema = false)
abstract class SupportDatabase : RoomDatabase() {
    abstract fun supportDao(): SupportDao
    abstract fun templateDao(): TemplateDao
    abstract fun missedCallDao(): MissedCallDao

    companion object {
        @Volatile
        private var INSTANCE: SupportDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Table support_templates if needed
                db.execSQL("CREATE TABLE IF NOT EXISTS support_templates (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL, text TEXT NOT NULL)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 3 columns if needed
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE support_records ADD COLUMN kullaniciAdi TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS missed_calls (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, callerNumber TEXT NOT NULL, callerName TEXT NOT NULL DEFAULT '', timestamp INTEGER NOT NULL DEFAULT 0, dateFormatted TEXT NOT NULL DEFAULT '', reason TEXT NOT NULL DEFAULT 'Cevapsız', isHandled INTEGER NOT NULL DEFAULT 0)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE missed_calls ADD COLUMN callType TEXT NOT NULL DEFAULT 'MISSED'")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE missed_calls ADD COLUMN duration TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE support_records ADD COLUMN audioUri TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
            }
        }

        fun getInstance(context: Context): SupportDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SupportDatabase::class.java,
                    "support_calls.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
