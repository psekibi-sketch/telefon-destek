package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "missed_calls")
data class MissedCallRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val callerNumber: String,
    val callerName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val dateFormatted: String = "",
    val reason: String = "Cevapsız", // "Cevapsız", "Gelen Çağrı", "Giden Arama"
    val isHandled: Boolean = false,
    val callType: String = "MISSED", // "MISSED" (Kırmızı), "INCOMING" (Beyaz), "OUTGOING" (Beyaz)
    val duration: String = ""
)

