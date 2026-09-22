package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.SupportRecord
import com.example.model.SupportTemplate

@Database(entities = [SupportRecord::class, SupportTemplate::class], version = 2, exportSchema = false)
abstract class SupportDatabase : RoomDatabase() {
    abstract fun supportDao(): SupportDao
    abstract fun templateDao(): TemplateDao

    companion object {
        @Volatile
        private var INSTANCE: SupportDatabase? = null

        fun getInstance(context: Context): SupportDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SupportDatabase::class.java,
                    "support_calls.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
