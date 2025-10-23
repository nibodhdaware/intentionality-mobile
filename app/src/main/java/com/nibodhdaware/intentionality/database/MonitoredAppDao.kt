package com.nibodhdaware.intentionality.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoredAppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(monitoredApp: MonitoredApp)

    @Delete
    suspend fun delete(monitoredApp: MonitoredApp)

    @Query("SELECT * FROM monitored_apps")
    fun getAll(): Flow<List<MonitoredApp>>
}