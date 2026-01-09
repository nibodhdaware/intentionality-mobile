package com.nibodhdaware.intentionality.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IntentionLogDao {
    @Insert
    suspend fun insert(log: IntentionLog): Long
    
    @Query("SELECT * FROM intention_logs WHERE timestamp >= :startOfDay ORDER BY timestamp ASC")
    fun getTodaysLogs(startOfDay: Long): Flow<List<IntentionLog>>
    
    @Query("SELECT * FROM intention_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<IntentionLog>>
    
    @Query("DELETE FROM intention_logs WHERE timestamp < :timestamp")
    suspend fun deleteOldLogs(timestamp: Long)
}
