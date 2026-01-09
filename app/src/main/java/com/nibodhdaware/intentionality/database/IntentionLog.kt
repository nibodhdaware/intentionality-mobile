package com.nibodhdaware.intentionality.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intention_logs")
data class IntentionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val reason: String,
    val dumbnessRating: Int, // 1-5 scale
    val timestamp: Long = System.currentTimeMillis()
)
