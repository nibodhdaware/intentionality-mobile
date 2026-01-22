package com.nibodhdaware.intentionality.database

import com.nibodhdaware.intentionality.billing.BillingManager
import com.nibodhdaware.intentionality.firebase.FirebaseManager
import com.nibodhdaware.intentionality.firebase.MonitoredAppData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

class MonitoredAppRepository(
    private val monitoredAppDao: MonitoredAppDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val TAG = "MonitoredAppRepository"

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

    /**
     * Pushes current local monitored apps to Firebase, if user is signed in and has Pro entitlement.
     */
    suspend fun syncToFirebase() {
        withContext(dispatcher) {
            if (!BillingManager.hasProEntitlement() || !FirebaseManager.isUserSignedIn()) {
                Log.d(TAG, "Skipping Firebase sync: User not Pro or not signed in.")
                return@withContext
            }

            val localApps = allMonitoredApps.firstOrNull() ?: emptyList()
            val monitoredAppDataList = localApps.map {
                MonitoredAppData(
                    packageName = it.packageName,
                    appName = it.appName,
                    customIntention = it.customIntention,
                    intervalMinutes = it.intervalMinutes
                )
            }

            val result = FirebaseManager.syncMonitoredApps(monitoredAppDataList)
            result.onSuccess {
                Log.d(TAG, "Monitored apps successfully synced to Firebase.")
            }.onFailure { e ->
                Log.e(TAG, "Failed to sync monitored apps to Firebase: ${e.message}", e)
            }
        }
    }

    /**
     * Fetches monitored apps from Firebase and merges them with the local database.
     * Firebase data is considered authoritative.
     */
    suspend fun fetchAndMergeFromFirebase() {
        withContext(dispatcher) {
            if (!BillingManager.hasProEntitlement() || !FirebaseManager.isUserSignedIn()) {
                Log.d(TAG, "Skipping Firebase fetch: User not Pro or not signed in.")
                return@withContext
            }

            val result = FirebaseManager.getMonitoredApps()
            result.onSuccess { firebaseAppsData ->
                Log.d(TAG, "Fetched ${firebaseAppsData.size} apps from Firebase.")

                val firebaseApps = firebaseAppsData.map {
                    MonitoredApp(
                        packageName = it.packageName,
                        appName = it.appName,
                        customIntention = it.customIntention,
                        intervalMinutes = it.intervalMinutes
                    )
                }

                // Clear local database and insert Firebase apps (Firebase is authoritative)
                monitoredAppDao.clearAll()
                firebaseApps.forEach { monitoredAppDao.insert(it) }
                Log.d(TAG, "Local database updated with Firebase data.")

            }.onFailure { e ->
                Log.e(TAG, "Failed to fetch and merge monitored apps from Firebase: ${e.message}", e)
            }
        }
    }

    suspend fun clearAll() {
        monitoredAppDao.clearAll()
    }
}