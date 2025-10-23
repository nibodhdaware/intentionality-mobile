package com.nibodhdaware.intentionality.database

import kotlinx.coroutines.flow.Flow

class MonitoredAppRepository(private val monitoredAppDao: MonitoredAppDao) {

    val allMonitoredApps: Flow<List<MonitoredApp>> = monitoredAppDao.getAll()

    suspend fun insert(monitoredApp: MonitoredApp) {
        monitoredAppDao.insert(monitoredApp)
    }

    suspend fun delete(monitoredApp: MonitoredApp) {
        monitoredAppDao.delete(monitoredApp)
    }
}