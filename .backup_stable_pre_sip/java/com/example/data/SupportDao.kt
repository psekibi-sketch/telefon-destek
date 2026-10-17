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

    @Query("SELECT * FROM support_records WHERE baslangicTarihi >= :startDateTime AND baslangicTarihi <= :endDateTime ORDER BY baslangicTarihi ASC")
    suspend fun getRecordsBetweenDates(startDateTime: String, endDateTime: String): List<SupportRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SupportRecord): Long

    @Update
    suspend fun update(record: SupportRecord)

    @Delete
    suspend fun delete(record: SupportRecord)

    @Query("DELETE FROM support_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM support_records WHERE baslangicTarihi >= :startDateTime AND baslangicTarihi <= :endDateTime")
    suspend fun deleteRecordsBetweenDates(startDateTime: String, endDateTime: String): Int

    @Query("SELECT COUNT(*) FROM support_records WHERE baslangicTarihi >= :startDateTime AND baslangicTarihi <= :endDateTime")
    suspend fun countRecordsBetweenDates(startDateTime: String, endDateTime: String): Int

    @Query("SELECT * FROM support_records WHERE isSynced = 0 ORDER BY id ASC")
    suspend fun getUnsyncedRecords(): List<SupportRecord>

    @Query("SELECT COUNT(*) FROM support_records WHERE isSynced = 0")
    fun getUnsyncedCount(): Flow<Int>

    @Query("UPDATE support_records SET isSynced = 1, syncedAt = :syncedAt WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>, syncedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM support_records WHERE sicilNo1 = :sicilNo ORDER BY id DESC LIMIT 1")
    suspend fun getLatestBySicilNo(sicilNo: String): SupportRecord?

    @Query("""
        SELECT * FROM support_records 
        WHERE (:sicilNo != '' AND (sicilNo1 LIKE '%' || :sicilNo || '%' OR yn500SicilNo1 LIKE '%' || :sicilNo || '%'))
           OR (:telefon != '' AND (telefon LIKE '%' || :telefon || '%' OR telefon2 LIKE '%' || :telefon || '%'))
           OR (:gorusulen != '' AND gorusulenKisi LIKE '%' || :gorusulen || '%')
        ORDER BY id DESC LIMIT 50
    """)
    suspend fun searchRecords(sicilNo: String, telefon: String, gorusulen: String): List<SupportRecord>

    @Query("SELECT * FROM support_records WHERE (sicilNo1 LIKE '%' || :query || '%' OR yn500SicilNo1 LIKE '%' || :query || '%') ORDER BY id DESC LIMIT 50")
    suspend fun searchBySicilNo(query: String): List<SupportRecord>

    @Query("SELECT * FROM support_records WHERE (telefon LIKE '%' || :query || '%' OR telefon2 LIKE '%' || :query || '%') ORDER BY id DESC LIMIT 50")
    suspend fun searchByPhone(query: String): List<SupportRecord>

    @Query("SELECT * FROM support_records WHERE gorusulenKisi LIKE '%' || :query || '%' ORDER BY id DESC LIMIT 50")
    suspend fun searchByGorusulen(query: String): List<SupportRecord>

    @Query("SELECT * FROM support_records WHERE musteriBayiAdi LIKE '%' || :query || '%' ORDER BY id DESC LIMIT 50")
    suspend fun searchByMusteriBayi(query: String): List<SupportRecord>

    @Query("""
        SELECT * FROM support_records 
        WHERE (sicilNo1 LIKE '%' || :query || '%' OR yn500SicilNo1 LIKE '%' || :query || '%')
           OR musteriBayiAdi LIKE '%' || :query || '%'
           OR gorusulenKisi LIKE '%' || :query || '%'
           OR (telefon LIKE '%' || :query || '%' OR telefon2 LIKE '%' || :query || '%')
        ORDER BY id DESC LIMIT 50
    """)
    suspend fun searchAllFields(query: String): List<SupportRecord>

    @Query("DELETE FROM support_records")
    suspend fun deleteAll()
}
