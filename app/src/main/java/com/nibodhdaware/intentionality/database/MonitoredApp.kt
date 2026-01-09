package com.nibodhdaware.intentionality.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitored_apps")
data class MonitoredApp(
    @PrimaryKey
    val packageName: String,
    val appName: String = "", // Default empty for backwards compatibility
    
    // Time schedule - when the overlay should be active
    val startHour: Int = 0,      // 0-23, 0 = midnight
    val startMinute: Int = 0,    // 0-59
    val endHour: Int = 23,       // 0-23, 23 = 11 PM
    val endMinute: Int = 59,     // 0-59
    val allDay: Boolean = true,  // If true, ignore start/end times
    
    // Repeat interval - how often to show overlay
    val intervalMinutes: Int = 5 // Show overlay every X minutes (default: 5)
)
