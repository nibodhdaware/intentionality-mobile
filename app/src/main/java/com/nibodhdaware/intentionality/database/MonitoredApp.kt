package com.nibodhdaware.intentionality.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitored_apps")
data class MonitoredApp(
    @PrimaryKey
    val packageName: String,
    val appName: String = "", // Default empty for backwards compatibility
    
    // Custom intention prompt for this specific app (Premium feature)
    val customIntention: String = "", // Empty means use default prompt
    
    // Repeat interval - how often to show overlay
    val intervalMinutes: Int = 0, // Show overlay every X minutes (default: 0 = every time)
    
    // Legacy fields - kept for database schema compatibility, no longer used
    @Deprecated("Time window scheduling removed - monitoring is manual now")
    val startHour: Int = 0,
    @Deprecated("Time window scheduling removed - monitoring is manual now")
    val startMinute: Int = 0,
    @Deprecated("Time window scheduling removed - monitoring is manual now")
    val endHour: Int = 23,
    @Deprecated("Time window scheduling removed - monitoring is manual now")
    val endMinute: Int = 59,
    @Deprecated("Time window scheduling removed - monitoring is manual now")
    val allDay: Boolean = true
)
