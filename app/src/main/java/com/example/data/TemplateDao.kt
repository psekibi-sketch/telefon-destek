package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.SupportTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM support_templates WHERE type = :type ORDER BY id ASC")
    fun getTemplatesByType(type: String): Flow<List<SupportTemplate>>

    @Query("SELECT * FROM support_templates ORDER BY id ASC")
    fun getAllTemplates(): Flow<List<SupportTemplate>>

    @Query("SELECT * FROM support_templates ORDER BY id ASC")
    suspend fun getAllTemplatesList(): List<SupportTemplate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: SupportTemplate): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templates: List<SupportTemplate>): List<Long>

    @Delete
    suspend fun delete(template: SupportTemplate)

    @Query("DELETE FROM support_templates WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM support_templates")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM support_templates")
    suspend fun getCount(): Int
}
