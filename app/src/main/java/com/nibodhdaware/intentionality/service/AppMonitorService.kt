package com.nibodhdaware.intentionality.service

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nibodhdaware.intentionality.MainActivity
import com.nibodhdaware.intentionality.R
import com.nibodhdaware.intentionality.database.AppDatabase
import com.nibodhdaware.intentionality.database.MonitoredAppRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class AppMonitorService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private lateinit var repository: MonitoredAppRepository
    
    private var lastDetectedApp: String? = null
    private val recentlyPromptedApps = mutableSetOf<String>()
    private var cachedMonitoredApps: List<String> = emptyList()
    private var lastCacheUpdate: Long = 0L
    
    companion object {
        private const val TAG = "AppMonitorService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "app_monitor_channel"
        private const val PROMPT_COOLDOWN_MS = 30000L // 30 seconds cooldown
        private const val CACHE_REFRESH_INTERVAL_MS = 10000L // Refresh cache every 10 seconds
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "===== Service onCreate =====")
        val monitoredAppDao = AppDatabase.getDatabase(this).monitoredAppDao()
        repository = MonitoredAppRepository(monitoredAppDao)
        
        createNotificationChannel()
        startForeground()
        Log.d(TAG, "Service started in foreground")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "===== Service onStartCommand =====")
        scope.launch(Dispatchers.IO) {
            while (true) {
                checkForegroundApp()
                delay(3000) // Check every 3 seconds (reduced frequency for less resource usage)
            }
        }
        Log.d(TAG, "Monitoring loop started, checking every 3 seconds")
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors app usage for intentionality"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForeground() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Intentionality Active")
            .setContentText("Monitoring your app usage")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private suspend fun checkForegroundApp() {
        try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val time = System.currentTimeMillis()
            
            // Refresh cached monitored apps periodically instead of every check
            if (time - lastCacheUpdate > CACHE_REFRESH_INTERVAL_MS) {
                cachedMonitoredApps = repository.allMonitoredApps.first().map { it.packageName }
                lastCacheUpdate = time
            }
            
            // Query only last 5 seconds instead of 10 for better performance
            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                time - 5000,
                time
            )
            
            val foregroundApp = usageStats?.maxByOrNull { it.lastTimeUsed }?.packageName
            
            // Reduce logging in production for better performance
            if (foregroundApp != lastDetectedApp) {
                Log.d(TAG, "App changed: $lastDetectedApp -> $foregroundApp")
            }

            if (foregroundApp != null && 
                foregroundApp != packageName && // Ignore our own app
                cachedMonitoredApps.contains(foregroundApp) &&
                foregroundApp != lastDetectedApp &&
                !recentlyPromptedApps.contains(foregroundApp)
            ) {
                Log.d(TAG, "✅ Showing prompt for: $foregroundApp")
                lastDetectedApp = foregroundApp
                showPrompt(foregroundApp)
                
                // Add to cooldown list and remove after cooldown period
                recentlyPromptedApps.add(foregroundApp)
                scope.launch {
                    delay(PROMPT_COOLDOWN_MS)
                    recentlyPromptedApps.remove(foregroundApp)
                }
            } else {
                // Update last detected app if changed
                if (foregroundApp != lastDetectedApp) {
                    lastDetectedApp = foregroundApp
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in checkForegroundApp", e)
            e.printStackTrace()
        }
    }

    private fun showPrompt(packageName: String) {
        try {
            Log.d(TAG, "===== showPrompt called for: $packageName =====")
            val pm = applicationContext.packageManager
            val appName = try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e(TAG, "App not found: $packageName", e)
                packageName
            }

            Log.d(TAG, "App name: $appName")
            
            // Check if we have SYSTEM_ALERT_WINDOW permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!android.provider.Settings.canDrawOverlays(this)) {
                    Log.e(TAG, "❌ SYSTEM_ALERT_WINDOW permission not granted!")
                    return
                }
            }
            
            // Start OverlayService to show the system overlay
            val intent = Intent(this, OverlayService::class.java).apply {
                putExtra(OverlayService.EXTRA_APP_NAME, appName)
                putExtra(OverlayService.EXTRA_PACKAGE_NAME, packageName)
            }
            
            Log.d(TAG, "Starting OverlayService...")
            startService(intent)
            Log.d(TAG, "✅ OverlayService started successfully!")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing prompt", e)
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}