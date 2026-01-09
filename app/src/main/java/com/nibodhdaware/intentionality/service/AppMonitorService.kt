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
import com.nibodhdaware.intentionality.database.MonitoredApp
import com.nibodhdaware.intentionality.database.MonitoredAppRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.Calendar

class AppMonitorService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private lateinit var repository: MonitoredAppRepository
    
    private var lastDetectedApp: String? = null
    private var cachedMonitoredApps: Map<String, MonitoredApp> = emptyMap() // Changed to store full app data
    private var lastCacheUpdate: Long = 0L
    
    // Track last overlay time for each app (for interval-based display)
    private val lastOverlayTime = mutableMapOf<String, Long>()
    
    companion object {
        private const val TAG = "AppMonitorService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "app_monitor_channel"
        private const val CACHE_REFRESH_INTERVAL_MS = 10000L // Refresh cache every 10 seconds
        private const val CHECK_INTERVAL_MS = 1000L // Check every 1 second
        
        // Notification action constants
        const val ACTION_PAUSE = "com.nibodhdaware.intentionality.ACTION_PAUSE"
        const val ACTION_RESUME = "com.nibodhdaware.intentionality.ACTION_RESUME"
        const val ACTION_STOP = "com.nibodhdaware.intentionality.ACTION_STOP"
        
        // Static flag to track overlay state globally
        var isOverlayVisible = false
        var currentMonitoredApp: String? = null // Track which app is currently being monitored
        var isPaused = false // Track if monitoring is paused
        
        // Apps where user pressed Go Back - reset their timer to now
        val appsToResetTimer = mutableSetOf<String>()
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
        
        // Handle notification actions
        when (intent?.action) {
            ACTION_PAUSE -> {
                isPaused = true
                Log.d(TAG, "Monitoring PAUSED")
                updateNotification(cachedMonitoredApps.size)
                return START_STICKY
            }
            ACTION_RESUME -> {
                isPaused = false
                Log.d(TAG, "Monitoring RESUMED")
                updateNotification(cachedMonitoredApps.size)
                return START_STICKY
            }
            ACTION_STOP -> {
                Log.d(TAG, "Monitoring STOPPED")
                stopSelf()
                return START_NOT_STICKY
            }
        }
        
        scope.launch(Dispatchers.IO) {
            while (true) {
                if (!isPaused) {
                    checkForegroundApp()
                }
                delay(CHECK_INTERVAL_MS) // Check every 1 second
            }
        }
        Log.d(TAG, "Monitoring loop started, checking every ${CHECK_INTERVAL_MS}ms")
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
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForeground() {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Create pause action
        val pauseIntent = Intent(this, AppMonitorService::class.java).apply {
            action = ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this,
            1,
            pauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Create stop action
        val stopIntent = Intent(this, AppMonitorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("✅ Intentionality Active")
            .setContentText("Starting monitoring...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Makes it persistent
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false) // Prevent swiping away
            .addAction(
                android.R.drawable.ic_media_pause,
                "Pause",
                pausePendingIntent
            )
            .addAction(
                android.R.drawable.ic_delete,
                "Stop",
                stopPendingIntent
            )
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
                cachedMonitoredApps = repository.allMonitoredApps.first()
                    .associateBy { it.packageName }
                lastCacheUpdate = time
                // Update notification with count
                updateNotification(cachedMonitoredApps.size)
            }
            
            // Query only last 2 seconds for faster detection
            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                time - 2000,
                time
            )
            
            val foregroundApp = usageStats?.maxByOrNull { it.lastTimeUsed }?.packageName
            
            // If overlay is visible and the monitored app is no longer foreground, dismiss overlay
            if (isOverlayVisible && currentMonitoredApp != null) {
                if (foregroundApp != currentMonitoredApp && foregroundApp != packageName) {
                    Log.d(TAG, "📱 Monitored app ($currentMonitoredApp) is no longer foreground. Dismissing overlay.")
                    dismissCurrentOverlay()
                }
                return // Don't show new overlay while one is visible
            }
            
            // Log only when app actually changes
            if (foregroundApp != lastDetectedApp) {
                Log.d(TAG, "App changed: $lastDetectedApp -> $foregroundApp")
            }

            // Check if current foreground app is monitored
            if (foregroundApp != null && 
                foregroundApp != packageName && // Ignore our own app
                cachedMonitoredApps.containsKey(foregroundApp) &&
                !isOverlayVisible // Don't show if overlay is already visible
            ) {
                // Check if this app needs timer reset (user pressed Go Back)
                if (appsToResetTimer.contains(foregroundApp)) {
                    appsToResetTimer.remove(foregroundApp)
                    lastOverlayTime.remove(foregroundApp) // Clear timer so popup shows immediately
                    Log.d(TAG, "🔄 Timer cleared for $foregroundApp - popup will show now")
                    // Don't return - let it continue to show the popup
                }
                
                val monitoredApp = cachedMonitoredApps[foregroundApp]!!
                
                // Check if we're within the active time window
                if (isWithinActiveTime(monitoredApp)) {
                    // Check if enough time has passed since last overlay
                    val lastTime = lastOverlayTime[foregroundApp] ?: 0L
                    val intervalMs = monitoredApp.intervalMinutes * 60 * 1000L
                    val timeSinceLastOverlay = time - lastTime
                    
                    if (timeSinceLastOverlay >= intervalMs) {
                        Log.d(TAG, "✅ Showing prompt for: $foregroundApp (interval: ${monitoredApp.intervalMinutes} min, time since last: ${timeSinceLastOverlay / 60000} min)")
                        lastDetectedApp = foregroundApp
                        currentMonitoredApp = foregroundApp
                        isOverlayVisible = true
                        lastOverlayTime[foregroundApp] = time
                        showPrompt(foregroundApp)
                    } else {
                        val minutesRemaining = (intervalMs - timeSinceLastOverlay) / 60000
                        if (foregroundApp != lastDetectedApp) {
                            Log.d(TAG, "⏳ Waiting for interval: $foregroundApp (${minutesRemaining}min remaining)")
                        }
                    }
                } else {
                    if (foregroundApp != lastDetectedApp) {
                        Log.d(TAG, "🕐 App outside active time window: $foregroundApp")
                    }
                }
            }
            
            // Update last detected app
            if (foregroundApp != lastDetectedApp) {
                lastDetectedApp = foregroundApp
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in checkForegroundApp", e)
            e.printStackTrace()
        }
    }
    
    /**
     * Check if current time is within the app's active window
     */
    private fun isWithinActiveTime(app: MonitoredApp): Boolean {
        if (app.allDay) return true
        
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute
        
        val startTimeInMinutes = app.startHour * 60 + app.startMinute
        val endTimeInMinutes = app.endHour * 60 + app.endMinute
        
        return if (startTimeInMinutes <= endTimeInMinutes) {
            // Same day (e.g., 9:00 AM to 5:00 PM)
            currentTimeInMinutes in startTimeInMinutes..endTimeInMinutes
        } else {
            // Crosses midnight (e.g., 11:00 PM to 2:00 AM)
            currentTimeInMinutes >= startTimeInMinutes || currentTimeInMinutes <= endTimeInMinutes
        }
    }
    
    private fun dismissCurrentOverlay() {
        try {
            val intent = Intent(this, OverlayService::class.java)
            stopService(intent)
            isOverlayVisible = false
            currentMonitoredApp = null
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing overlay", e)
        }
    }
    
    private fun updateNotification(monitoredAppCount: Int) {
        try {
            val notificationIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val contentTitle = when {
                isPaused -> "⏸️ Monitoring Paused"
                else -> "✅ Intentionality Active"
            }
            
            val contentText = when {
                isPaused -> "Tap Resume to continue monitoring"
                monitoredAppCount > 0 -> "Watching $monitoredAppCount ${if (monitoredAppCount == 1) "app" else "apps"}"
                else -> "No apps monitored • Tap to add apps"
            }

            // Create pause/resume action
            val pauseResumeIntent = Intent(this, AppMonitorService::class.java).apply {
                action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
            }
            val pauseResumePendingIntent = PendingIntent.getService(
                this,
                1,
                pauseResumeIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            
            // Create stop action
            val stopIntent = Intent(this, AppMonitorService::class.java).apply {
                action = ACTION_STOP
            }
            val stopPendingIntent = PendingIntent.getService(
                this,
                2,
                stopIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .addAction(
                    if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                    if (isPaused) "Resume" else "Pause",
                    pauseResumePendingIntent
                )
                .addAction(
                    android.R.drawable.ic_delete,
                    "Stop",
                    stopPendingIntent
                )
                .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification", e)
        }
    }
    
    /**
     * Reset overlay timers for an app - allows it to show immediately next time
     */
    fun resetAppTimer(packageName: String) {
        lastOverlayTime.remove(packageName)
        Log.d(TAG, "Timer reset for app: $packageName")
    }
    
    /**
     * Reset all overlay timers - allows all apps to show immediately
     */
    fun resetAllTimers() {
        lastOverlayTime.clear()
        Log.d(TAG, "All app timers reset")
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
            isOverlayVisible = false
            currentMonitoredApp = null
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