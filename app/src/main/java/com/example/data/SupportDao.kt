package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.SupportRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SupportDao {
    @Query("SELECT * FROM support_records ORDER BY id DESC")
    fun getAllRecords(): Flow<List<SupportRecord>>

    @Query("SELECT * FROM support_records ORDER BY id DESC")
    suspend fun getAllRecordsList(): List<SupportRecord>

    @Query("SELECT * FROM support_records WHERE basTarihi >= :startDateTime AND basTarihi <= :endDateTime ORDER BY basTarihi ASC")
    suspend fun getRecordsBetweenDates(startDateTime: String, endDateTime: String): List<SupportRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SupportRecord): Long

    @Update
    suspend fun update(record: SupportRecord)

    @Delete
    suspend fun delete(record: SupportRecord)

    @Query("DELETE FROM support_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM support_records WHERE basTarihi >= :startDateTime AND basTarihi <= :endDateTime")
    suspend fun deleteRecordsBetweenDates(startDateTime: String, endDateTime: String): Int

    @Query("SELECT COUNT(*) FROM support_records WHERE basTarihi >= :startDateTime AND basTarihi <= :endDateTime")
    suspend fun countRecordsBetweenDates(startDateTime: String, endDateTime: String): Int

    @Query("DELETE FROM support_records")
    suspend fun deleteAll()
}
