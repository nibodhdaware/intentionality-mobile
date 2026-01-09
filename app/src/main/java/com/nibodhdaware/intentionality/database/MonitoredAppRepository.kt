package com.nibodhdaware.intentionality.database

import kotlinx.coroutines.flow.Flow

class MonitoredAppRepository(private val monitoredAppDao: MonitoredAppDao) {

    val allMonitoredApps: Flow<List<MonitoredApp>> = monitoredAppDao.getAll()

    suspend fun insert(monitoredApp: MonitoredApp) {
        monitoredAppDao.insert(monitoredApp)
    }
    
    suspend fun update(monitoredApp: MonitoredApp) {
        monitoredAppDao.update(monitoredApp)
    }

    suspend fun delete(monitoredApp: MonitoredApp) {
        monitoredAppDao.delete(monitoredApp)
    }
    
    suspend fun getByPackageName(packageName: String): MonitoredApp? {
        return monitoredAppDao.getByPackageName(packageName)
    }
}