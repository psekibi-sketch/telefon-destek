package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.MissedCallRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MissedCallDao {
    @Query("SELECT * FROM missed_calls ORDER BY timestamp DESC")
    fun getAllMissedCalls(): Flow<List<MissedCallRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(call: MissedCallRecord): Long

    @Query("DELETE FROM missed_calls WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM missed_calls")
    suspend fun deleteAll()

    @Query("UPDATE missed_calls SET isHandled = 1 WHERE id = :id")
    suspend fun markHandled(id: Long)

    @Query("UPDATE missed_calls SET isHandled = 1 WHERE callerNumber = :number OR callerNumber LIKE '%' || :number || '%' OR :number LIKE '%' || callerNumber || '%'")
    suspend fun markHandledByNumber(number: String)

    @Query("DELETE FROM missed_calls WHERE callerNumber = :number OR callerNumber LIKE '%' || :number || '%' OR :number LIKE '%' || callerNumber || '%'")
    suspend fun deleteByNumber(number: String)

    @Query("SELECT * FROM missed_calls WHERE callerNumber = :callerNumber AND timestamp >= :afterTimestamp LIMIT 1")
    suspend fun findRecentCall(callerNumber: String, afterTimestamp: Long): MissedCallRecord?

    @Query("SELECT * FROM missed_calls ORDER BY timestamp ASC")
    suspend fun getAllMissedCallsList(): List<MissedCallRecord>
}
