package com.nibodhdaware.intentionality.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoredAppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(monitoredApp: MonitoredApp)
    
    @Update
    suspend fun update(monitoredApp: MonitoredApp)

    @Delete
    suspend fun delete(monitoredApp: MonitoredApp)

    @Query("SELECT * FROM monitored_apps")
    fun getAll(): Flow<List<MonitoredApp>>
    
    @Query("SELECT * FROM monitored_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackageName(packageName: String): MonitoredApp?

    @Query("DELETE FROM monitored_apps")
    suspend fun clearAll()
}