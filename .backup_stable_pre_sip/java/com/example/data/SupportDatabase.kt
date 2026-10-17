package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.SupportRecord
import com.example.model.SupportTemplate

@Database(entities = [SupportRecord::class, SupportTemplate::class], version = 4, exportSchema = false)
abstract class SupportDatabase : RoomDatabase() {
    abstract fun supportDao(): SupportDao
    abstract fun templateDao(): TemplateDao

    companion object {
        @Volatile
        private var INSTANCE: SupportDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE support_records ADD COLUMN kullaniciAdi TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): SupportDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SupportDatabase::class.java,
                    "support_calls.db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
