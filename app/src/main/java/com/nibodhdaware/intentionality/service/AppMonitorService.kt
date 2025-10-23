package com.nibodhdaware.intentionality.service

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.nibodhdaware.intentionality.MainActivity
import com.nibodhdaware.intentionality.R
import com.nibodhdaware.intentionality.database.AppDatabase
import com.nibodhdaware.intentionality.database.MonitoredAppRepository
import com.nibodhdaware.intentionality.ui.prompt.IntentionPromptActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class AppMonitorService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private lateinit var repository: MonitoredAppRepository
    
    private var lastDetectedApp: String? = null
    private val recentlyPromptedApps = mutableSetOf<String>()
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "app_monitor_channel"
        private const val PROMPT_COOLDOWN_MS = 30000L // 30 seconds cooldown
    }

    override fun onCreate() {
        super.onCreate()
        val monitoredAppDao = AppDatabase.getDatabase(this).monitoredAppDao()
        repository = MonitoredAppRepository(monitoredAppDao)
        
        createNotificationChannel()
        startForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            while (true) {
                checkForegroundApp()
                delay(2000) // Check every 2 seconds
            }
        }
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
            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                time - 1000 * 10,
                time
            )
            
            val foregroundApp = usageStats?.maxByOrNull { it.lastTimeUsed }?.packageName
            val monitoredApps = repository.allMonitoredApps.first().map { it.packageName }

            if (foregroundApp != null && 
                foregroundApp != packageName && // Ignore our own app
                monitoredApps.contains(foregroundApp) &&
                foregroundApp != lastDetectedApp &&
                !recentlyPromptedApps.contains(foregroundApp)
            ) {
                lastDetectedApp = foregroundApp
                showPrompt(foregroundApp)
                
                // Add to cooldown list and remove after cooldown period
                recentlyPromptedApps.add(foregroundApp)
                scope.launch {
                    delay(PROMPT_COOLDOWN_MS)
                    recentlyPromptedApps.remove(foregroundApp)
                }
            } else if (foregroundApp != lastDetectedApp) {
                lastDetectedApp = foregroundApp
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showPrompt(packageName: String) {
        try {
            val pm = applicationContext.packageManager
            val appName = try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                packageName
            }

            val intent = Intent(this, IntentionPromptActivity::class.java).apply {
                putExtra("app_name", appName)
                putExtra("package_name", packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        } catch (e: Exception) {
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