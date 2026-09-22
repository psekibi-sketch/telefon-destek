package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "support_templates")
data class SupportTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "TESPIT" or "ISLEM"
    val text: String
)
